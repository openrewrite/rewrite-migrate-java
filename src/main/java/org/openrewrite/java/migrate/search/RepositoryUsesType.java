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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.marker.SearchResult;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class RepositoryUsesType extends ScanningRecipe<AtomicBoolean> {

    @Option(displayName = "Type pattern",
            description = "A fully-qualified type name, optionally with a `*` wildcard to match any type in a package " +
                    "or `..` to match types in subpackages as well.",
            example = "org.apache.commons.fileupload.servlet.*")
    String fullyQualifiedTypeName;

    String displayName = "Repository uses type";

    String description = "Marks all files in a repository where any source file uses a matching type. " +
            "Intended as a precondition for recipes that change a dependency together with the code using it, " +
            "when modules that do not use the type themselves, such as a parent pom managing the version, must change too.";

    Duration estimatedEffortPerOccurrence = Duration.ZERO;

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean usesType) {
        UsesType<ExecutionContext> visitor = new UsesType<>(fullyQualifiedTypeName, null);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!usesType.get() && tree instanceof SourceFile && visitor.visit(tree, ctx) != tree) {
                    usesType.set(true);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean usesType) {
        if (!usesType.get()) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    return SearchResult.mergingFound(tree, "Repository uses type: " + fullyQualifiedTypeName);
                }
                return tree;
            }
        };
    }
}
