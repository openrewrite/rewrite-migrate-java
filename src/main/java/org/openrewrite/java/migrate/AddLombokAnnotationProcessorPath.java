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
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddAnnotationProcessor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddLombokAnnotationProcessorPath extends ScanningRecipe<AddLombokAnnotationProcessorPath.Accumulator> {

    private static final String LOMBOK_GROUP = "org.projectlombok";
    private static final String LOMBOK_ARTIFACT = "lombok";

    String displayName = "Add `lombok` to the `maven-compiler-plugin` annotation processor paths";

    String description = "Add the `lombok` annotation processor path, matching the version of the `lombok` dependency. " +
            "Once `annotationProcessorPaths` is configured, javac no longer discovers annotation processors on the classpath, " +
            "so Lombok has to be listed there explicitly to keep running.";

    public static class Accumulator {
        @Nullable
        String lombokVersion;

        final AddAnnotationProcessor.Scanned processorPaths = new AddAnnotationProcessor.Scanned();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        // The version is only consulted by the visitor; the scan phase ignores it, so any placeholder is fine here.
        TreeVisitor<?, ExecutionContext> processorPathScanner =
                new AddAnnotationProcessor(LOMBOK_GROUP, LOMBOK_ARTIFACT, "0").getScanner(acc.processorPaths);
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                processorPathScanner.visit(document, ctx);
                if (acc.lombokVersion == null) {
                    for (ResolvedDependency lombok : getResolutionResult().findDependencies(LOMBOK_GROUP, LOMBOK_ARTIFACT, null)) {
                        acc.lombokVersion = lombok.getVersion();
                        break;
                    }
                }
                return document;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.lombokVersion == null) {
            return TreeVisitor.noop();
        }
        return new AddAnnotationProcessor(LOMBOK_GROUP, LOMBOK_ARTIFACT, acc.lombokVersion).getVisitor(acc.processorPaths);
    }
}
