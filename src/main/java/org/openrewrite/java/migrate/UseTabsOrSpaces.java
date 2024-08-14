/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.format.NormalizeTabsOrSpacesVisitor;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseTabsOrSpaces extends Recipe {
    @Option(displayName = "Use tabs",
            description = "Whether to use tabs for indentation.")
    boolean useTabs;

    @Override
    public String getDisplayName() {
        return "Force indentation to either tabs or spaces";
    }

    @Override
    public String getDescription() {
        return "This is useful for one-off migrations of a codebase that has mixed indentation styles, while " +
               "preserving all other auto-detected formatting rules.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    TabsAndIndentsStyle style = ((SourceFile) cu).getStyle(TabsAndIndentsStyle.class);
                    if (style == null) {
                        style = IntelliJ.tabsAndIndents();
                    }
                    style = style.withUseTabCharacter(useTabs);
                    return new NormalizeTabsOrSpacesVisitor<>(style).visit(tree, ctx);
                }
                return (J) tree;
            }
        };
    }
}
