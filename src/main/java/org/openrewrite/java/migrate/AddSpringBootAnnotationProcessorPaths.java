/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddAnnotationProcessor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddSpringBootAnnotationProcessorPaths extends ScanningRecipe<AddSpringBootAnnotationProcessorPaths.Accumulator> {

    private static final String SPRING_BOOT_GROUP = "org.springframework.boot";
    private static final List<String> PROCESSOR_ARTIFACTS = asList(
            "spring-boot-configuration-processor",
            "spring-boot-autoconfigure-processor");

    String displayName = "Keep Spring Boot annotation processors in newly created `annotationProcessorPaths`";

    String description = "Once `annotationProcessorPaths` is configured on the `maven-compiler-plugin`, javac no longer discovers " +
            "annotation processors on the classpath. When an earlier step created that list, for instance to enable Lombok, " +
            "add the `spring-boot-configuration-processor` and `spring-boot-autoconfigure-processor` the module declares as dependencies, " +
            "so their metadata is still generated. Lists that existed before the run are left alone.";

    public static class Accumulator {
        final List<Processor> processors = new ArrayList<>();

        /**
         * POMs whose effective `maven-compiler-plugin` already configured `annotationProcessorPaths` before this run.
         */
        final Set<Path> hadProcessorPaths = new HashSet<>();

        Accumulator() {
            for (String artifactId : PROCESSOR_ARTIFACTS) {
                processors.add(new Processor(artifactId));
            }
        }
    }

    static class Processor {
        final String artifactId;
        final AddAnnotationProcessor.Scanned processorPaths = new AddAnnotationProcessor.Scanned();

        /**
         * POMs that declare the processor, and their parents, which is where `AddAnnotationProcessor` may put the path.
         */
        final Set<Path> targets = new HashSet<>();

        @Nullable
        String version;

        Processor(String artifactId) {
            this.artifactId = artifactId;
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        List<TreeVisitor<?, ExecutionContext>> processorPathScanners = new ArrayList<>();
        for (Processor processor : acc.processors) {
            // The version is only consulted by the visitor; the scan phase ignores it, so any placeholder is fine here.
            processorPathScanners.add(new AddAnnotationProcessor(SPRING_BOOT_GROUP, processor.artifactId, "0")
                    .getScanner(processor.processorPaths));
        }
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                for (TreeVisitor<?, ExecutionContext> scanner : processorPathScanners) {
                    scanner.visit(document, ctx);
                }
                MavenResolutionResult mrr = getResolutionResult();
                Path sourcePath = mrr.getPom().getRequested().getSourcePath();
                if (sourcePath == null) {
                    return document;
                }
                if (hasProcessorPaths(mrr.getPom())) {
                    acc.hadProcessorPaths.add(sourcePath);
                }
                for (Processor processor : acc.processors) {
                    List<ResolvedDependency> found = mrr.findDependencies(SPRING_BOOT_GROUP, processor.artifactId, null);
                    if (found.isEmpty()) {
                        continue;
                    }
                    if (processor.version == null) {
                        processor.version = found.get(0).getVersion();
                    }
                    processor.targets.add(sourcePath);
                    MavenResolutionResult parent = mrr.getParent();
                    if (parent != null && parent.getPom().getRequested().getSourcePath() != null) {
                        processor.targets.add(parent.getPom().getRequested().getSourcePath());
                    }
                }
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        List<Processor> processors = new ArrayList<>();
        List<TreeVisitor<?, ExecutionContext>> processorVisitors = new ArrayList<>();
        for (Processor processor : acc.processors) {
            if (processor.version != null) {
                processors.add(processor);
                processorVisitors.add(new AddAnnotationProcessor(SPRING_BOOT_GROUP, processor.artifactId, processor.version)
                        .getVisitor(processor.processorPaths));
            }
        }
        if (processors.isEmpty()) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof Xml.Document)) {
                    return tree;
                }
                Path sourcePath = ((Xml.Document) tree).getSourcePath();
                // Only complete a list that an earlier step created in this run: adding a path to a list that
                // was already there, or creating a new list, would change which processors run.
                if (acc.hadProcessorPaths.contains(sourcePath) || !declaresProcessorPaths((Xml.Document) tree)) {
                    return tree;
                }
                Tree t = tree;
                for (int i = 0; i < processors.size(); i++) {
                    if (processors.get(i).targets.contains(sourcePath)) {
                        t = processorVisitors.get(i).visit(t, ctx);
                    }
                }
                return t;
            }
        };
    }

    private static boolean hasProcessorPaths(ResolvedPom pom) {
        return hasProcessorPaths(pom.getPlugins()) || hasProcessorPaths(pom.getPluginManagement());
    }

    private static boolean hasProcessorPaths(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId()) &&
                plugin.getConfiguration() != null &&
                plugin.getConfiguration().has("annotationProcessorPaths")) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresProcessorPaths(Xml.Document document) {
        AtomicBoolean found = new AtomicBoolean();
        new XmlIsoVisitor<AtomicBoolean>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, AtomicBoolean f) {
                if ("annotationProcessorPaths".equals(tag.getName())) {
                    f.set(true);
                    return tag;
                }
                return super.visitTag(tag, f);
            }
        }.visit(document, found);
        return found.get();
    }
}
