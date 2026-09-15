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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.migrate.lombok.LombokConfig.STOP_BUBBLING;
import static org.openrewrite.java.migrate.lombok.LombokConfig.append;
import static org.openrewrite.java.migrate.lombok.LombokConfig.declaresStopBubbling;
import static org.openrewrite.java.migrate.lombok.LombokConfig.expandImports;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isConfig;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isLombokConfig;
import static org.openrewrite.java.migrate.lombok.LombokConfig.parse;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddStopBubblingToLombokConfig extends ScanningRecipe<AddStopBubblingToLombokConfig.Accumulator> {

    private static final String STOP_BUBBLING_TRUE = STOP_BUBBLING + " = true";

    String displayName = "Add `config.stopBubbling` to the root `lombok.config`";

    String description = "Append `config.stopBubbling = true` to the root `lombok.config` when it does not already " +
            "declare that key, so that Lombok reads the project's configuration and nothing else. Lombok resolves a " +
            "key by walking up from the directory of the Java file it is compiling and does not stop at the project, " +
            "so without this key a `lombok.config` in a parent directory of wherever the project happens to be " +
            "checked out takes part in the build. Note that this cuts the project off from such a file whether or " +
            "not it was meant to be read, so a project that deliberately inherits configuration from a directory " +
            "above it should not run this recipe. Nothing is added when the key is already declared, whatever value " +
            "it is assigned, as a project that turns bubbling off explicitly, or back on again, is doing so " +
            "deliberately; nor when the root file imports a file that is not among the sources, as there is then no " +
            "telling whether that file declares the key.";

    public static class Accumulator {
        @Nullable
        Path rootConfig;

        List<LombokConfig.Line> rootLines = new ArrayList<>();

        /**
         * Every file an {@code import} could name, keyed by path, so that a key the root pulls in rather than
         * declares itself is not declared a second time.
         */
        final Map<Path, List<LombokConfig.Line>> configs = new HashMap<>();
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
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (!isConfig(sourceFile)) {
                    return sourceFile;
                }
                Path path = sourceFile.getSourcePath();
                List<LombokConfig.Line> lines = parse(PlainTextParser.convert(sourceFile).getText());
                acc.configs.put(path, lines);
                if (isLombokConfig(sourceFile) && path.getParent() == null) {
                    acc.rootConfig = path;
                    acc.rootLines = lines;
                }
                return sourceFile;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.rootConfig == null) {
            return TreeVisitor.noop();
        }
        List<LombokConfig.Line> rootLines = expandImports(acc.rootConfig, acc.rootLines, acc.configs, new HashSet<>());
        if (rootLines == null || declaresStopBubbling(rootLines)) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (!sourceFile.getSourcePath().equals(acc.rootConfig)) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                return plainText.withText(append(plainText.getText(), singletonList(STOP_BUBBLING_TRUE)));
            }
        };
    }
}
