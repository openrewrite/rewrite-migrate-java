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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@EqualsAndHashCode(callSuper = false)
@Value
public class ModuleHasKotlinSource extends ScanningRecipe<ModuleHasKotlinSource.Accumulator> {

    private static final Set<String> BUILD_FILE_NAMES = new HashSet<>(Arrays.asList("pom.xml", "build.gradle", "build.gradle.kts"));

    @Option(displayName = "Invert marking",
            description = "If `true`, marks files in modules that do *not* contain Kotlin sources. Defaults to `false`.",
            required = false)
    @Nullable
    Boolean invertMarking;

    String displayName = "Module has Kotlin source files";

    String description = "Marks all files in modules that contain at least one Kotlin source file (`.kt`). " +
            "Intended as a precondition to scope recipes to projects that actually compile Kotlin, " +
            "as opposed to projects that merely pick up `kotlin-stdlib` transitively.";

    public static class Accumulator {
        final Set<JavaProject> modulesWithKotlin = new HashSet<>();
        final Map<Path, JavaProject> moduleDirectories = new HashMap<>();
        final List<Path> kotlinSourcesWithoutModule = new ArrayList<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    Path sourcePath = sourceFile.getSourcePath();
                    Optional<JavaProject> maybeJp = sourceFile.getMarkers().findFirst(JavaProject.class);
                    if (sourcePath.toString().endsWith(".kt")) {
                        if (maybeJp.isPresent()) {
                            acc.modulesWithKotlin.add(maybeJp.get());
                        } else {
                            acc.kotlinSourcesWithoutModule.add(sourcePath);
                        }
                    } else if (maybeJp.isPresent() && sourcePath.getFileName() != null &&
                               BUILD_FILE_NAMES.contains(sourcePath.getFileName().toString())) {
                        Path moduleDirectory = sourcePath.getParent() == null ? Paths.get("") : sourcePath.getParent();
                        acc.moduleDirectories.put(moduleDirectory, maybeJp.get());
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        // Some build tools, like the Android Gradle plugin, leave Kotlin sources without a `JavaProject` marker.
        // Attribute those to the module whose build file is in the closest enclosing directory.
        for (Path kotlinSource : acc.kotlinSourcesWithoutModule) {
            JavaProject enclosingModule = null;
            int enclosingDepth = -1;
            for (Map.Entry<Path, JavaProject> module : acc.moduleDirectories.entrySet()) {
                Path moduleDirectory = module.getKey();
                int depth = moduleDirectory.toString().isEmpty() ? 0 : moduleDirectory.getNameCount();
                if (depth > enclosingDepth && kotlinSource.startsWith(moduleDirectory)) {
                    enclosingModule = module.getValue();
                    enclosingDepth = depth;
                }
            }
            if (enclosingModule != null) {
                acc.modulesWithKotlin.add(enclosingModule);
            }
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                boolean shouldInvert = Boolean.TRUE.equals(invertMarking);
                Optional<JavaProject> maybeJp = tree.getMarkers().findFirst(JavaProject.class);
                if (!maybeJp.isPresent()) {
                    if (shouldInvert) {
                        return SearchResult.found(tree, "No module, so vacuously has no Kotlin source");
                    }
                    return tree;
                }
                JavaProject jp = maybeJp.get();
                if (shouldInvert && !acc.modulesWithKotlin.contains(jp)) {
                    return SearchResult.found(tree, "Module does not have Kotlin source");
                }
                if (!shouldInvert && acc.modulesWithKotlin.contains(jp)) {
                    return SearchResult.found(tree, "Module has Kotlin source");
                }
                return tree;
            }
        };
    }
}
