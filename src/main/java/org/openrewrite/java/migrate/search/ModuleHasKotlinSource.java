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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class ModuleHasKotlinSource extends ScanningRecipe<Set<JavaProject>> {

    @Option(displayName = "Invert marking",
            description = "If `true`, marks files in modules that do *not* contain Kotlin sources. Defaults to `false`.",
            required = false)
    @Nullable
    Boolean invertMarking;

    String displayName = "Module has Kotlin source files";

    String description = "Marks all files in modules that contain at least one Kotlin source file (`.kt`). " +
            "Intended as a precondition to scope recipes to projects that actually compile Kotlin, " +
            "as opposed to projects that merely pick up `kotlin-stdlib` transitively.";

    @Override
    public Set<JavaProject> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<JavaProject> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    if (sourceFile.getSourcePath().toString().endsWith(".kt")) {
                        sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(acc::add);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<JavaProject> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                boolean shouldInvert = Boolean.TRUE.equals(invertMarking);
                Optional<JavaProject> maybeJp = tree.getMarkers().findFirst(JavaProject.class);
                if (!maybeJp.isPresent()) {
                    if (shouldInvert) {
                        return SearchResult.found(tree, "No module, so vacuously has no Kotlin source");
                    }
                    return tree;
                }
                JavaProject jp = maybeJp.get();
                if (shouldInvert && !acc.contains(jp)) {
                    return SearchResult.found(tree, "Module does not have Kotlin source");
                }
                if (!shouldInvert && acc.contains(jp)) {
                    return SearchResult.found(tree, "Module has Kotlin source");
                }
                return tree;
            }
        };
    }
}
