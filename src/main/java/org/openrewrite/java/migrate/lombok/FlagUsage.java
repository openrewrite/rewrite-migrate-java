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
package org.openrewrite.java.migrate.lombok;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.migrate.lombok.LombokConfig.assign;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isLombokConfig;

/**
 * Assigns a {@code flagUsage} key in every {@code lombok.config}, so that Lombok fails the build where a feature is
 * used. Every config is written to rather than only the root, as a config below the root has the last word on the
 * directories under it and would otherwise go on allowing what the root forbids.
 */
abstract class FlagUsage extends Recipe {

    private static final String ERROR = "error";

    /**
     * The key this recipe assigns, written into the file as it is spelled here.
     */
    abstract String getKey();

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (!isLombokConfig(sourceFile)) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                String text = assign(plainText.getText(), getKey(), ERROR);
                return text == null ? sourceFile : plainText.withText(text);
            }
        };
    }
}
