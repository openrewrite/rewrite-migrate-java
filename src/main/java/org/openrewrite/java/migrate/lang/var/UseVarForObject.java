/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang.var;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.HasJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarForObject extends Recipe {
    @Override
    public String getDisplayName() {
        return "UseVarForObjects";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Try to apply local variable type inference `var` to variables containing Objects where possible." +
               "This recipe will not touch variable declaration with genrics or initializer containing ternary operators.";
    }

    @Override
    public List<RecipeExample> getExamples() {
        RecipeExample recipeExample = new RecipeExample();
        List<RecipeExample.Source> examples = new ArrayList<>();
        //language=java
        examples.add(new RecipeExample.Source(
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  void m() {\n" +
                "    Object o = new Object();\n" +
                "  }\n" +
                "}",
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  void m() {\n" +
                "    var o = new Object();\n" +
                "  }\n" +
                "}",
                null, "java"));

        //language=java
        examples.add(new RecipeExample.Source(
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  Object o = new Object();\n" +
                "  void m() {\n" +
                "    Object inner = o;\n" +
                "  }\n" +
                "}",
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  void m() {\n" +
                "    var inner = o;\n" +
                "  }\n" +
                "}",
                null, "java"));
        //language=java
        examples.add(new RecipeExample.Source(
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  static {\n" +
                "    Object o = new Object();\n" +
                "  }\n" +
                "}",
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  static {\n" +
                "    var o = new Object();\n" +
                "  }\n" +
                "}",
                null, "java"));
        //language=java
        examples.add(new RecipeExample.Source(
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  {\n" +
                "    Object o = new Object();\n" +
                "  }\n" +
                "}",
                "package com.example.app;\n" +
                "\n" +
                "class A {\n" +
                "  {\n" +
                "    var o = new Object();\n" +
                "  }\n" +
                "}",
                null, "java"));

        //language=markdown
        recipeExample.setDescription("Applies `var` keyword to primitive variable definitions if the type is different from `short` or `byte`");
        recipeExample.setSources(examples);

        List<RecipeExample> exampleList = new ArrayList<>();
        exampleList.add(recipeExample);
        return exampleList;
    }

    @NotNull
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new HasJavaVersion("10", true),
                new UseVarForObjectVisitor());
    }


    static final class UseVarForObjectVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext executionContext) {
            vd = super.visitVariableDeclarations(vd, executionContext);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(this.getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean usesGenerics = DeclarationCheck.useGenerics(vd);
            boolean usesTernary = DeclarationCheck.initializedByTernary(vd);
            if (isPrimitive || usesGenerics || usesTernary) return vd;

            return transformToVar(vd);
        }

        @NotNull
        private J.VariableDeclarations transformToVar(@NotNull J.VariableDeclarations vd) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            return template.apply(this.getCursor(), vd.getCoordinates().replace(), simpleName, initializer);
        }
    }
}
