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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.migrate.lombok.LombokConfig.assign;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isLombokConfig;

@Value
@EqualsAndHashCode(callSuper = false)
public class FlagUsage extends Recipe {

    private static final String ERROR = "error";

    @Option(displayName = "Configuration key",
            description = "The `flagUsage` key to assign, written into the file as it is spelled here.",
            example = "lombok.val.flagUsage")
    String key;

    String displayName = "Flag usage of a Lombok feature";

    String description = "Assign a `flagUsage` key the value `error` in every `lombok.config`, so that Lombok fails " +
            "the build on a use of the feature rather than compiling it. Run this once the uses are gone, to keep " +
            "them from coming back. Every config is written to rather than only the root, as a config below the root " +
            "has the last word on the directories under it and would otherwise go on allowing what the root forbids. " +
            "A file that assigns the key another value is rewritten to `error`; a file that speaks about the key in " +
            "a way that cannot be rewritten, such as `clear lombok.val.flagUsage`, is left as written, as is a " +
            "project with no `lombok.config` at all, as there is then no file to write to.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", key);
    }

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
                String text = assign(plainText.getText(), key, ERROR);
                return text == null ? sourceFile : plainText.withText(text);
            }
        };
    }
}
