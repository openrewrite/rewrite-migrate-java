package org.openrewrite.java.migrate.lang.var;

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
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class UseVarForPrimitives extends Recipe {
    @Override
    public String getDisplayName() {
        return "UseVarForPrimitives";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Try to apply local variable type inference `var` to primitiv variables where possible." +
               "This recipe will not touch variable declaration with initializer containing ternary operators.";
    }

    @Override
    public List<RecipeExample> getExamples() {
        RecipeExample recipeExample = new RecipeExample();
        List<RecipeExample.Source> examples = new ArrayList<>();
        //language=java
        examples.add(new RecipeExample.Source(
                "package com.example.app;\n" +
                "                                \n" +
                "class A {\n" +
                "  void m() {\n" +
                "    String str = \"I am a value\";\n" +
                "    boolean b = true;\n" +
                "    char ch = '\ufffd';\n" +
                "    double d1 = 2.0;\n" +
                "    double d2 = 2.0D;\n" +
                "    float f1 = 2.0;\n" +
                "    float f2 = 2.0F;\n" +
                "    long l1 = 2;\n" +
                "    long l2 = 2L;\n" +
                "    // no change\n" +
                "    byte flags = 0;\n" +
                "    short mask = 0x7fff;\n" +
                "  }\n" +
                "}",
                "package com.example.app;\n" +
                "                                \n" +
                "class A {\n" +
                "  void m() {\n" +
                "    var str = \"I am a value\";\n" +
                "    var b = true;\n" +
                "    var ch = '\ufffd';\n" +
                "    var d1 = 2.0;\n" +
                "    var d2 = 2.0D;\n" +
                "    var f1 = 2.0;\n" +
                "    var f2 = 2.0F;\n" +
                "    var l1 = 2;\n" +
                "    var l2 = 2L;\n" +
                "    // no change\n" +
                "    byte flags = 0;\n" +
                "    short mask = 0x7fff;\n" +
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
                new VarForPrimitivesVisitor());
    }

    static final class VarForPrimitivesVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final JavaType.Primitive SHORT_TYPE = JavaType.Primitive.Short;
        private final JavaType.Primitive BYTE_TYPE = JavaType.Primitive.Byte;

        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext executionContext) {
            vd = super.visitVariableDeclarations(vd, executionContext);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(this.getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            // recipe specific
            boolean isNoPrimitive = !DeclarationCheck.isPrimitive(vd);
            boolean isByteVariable = DeclarationCheck.declarationHasType(vd, BYTE_TYPE);
            boolean isShortVariable = DeclarationCheck.declarationHasType(vd, SHORT_TYPE);
            if (isNoPrimitive || isByteVariable || isShortVariable) return vd;

            return transformToVar(vd);
        }

        @NotNull
        private J.VariableDeclarations transformToVar(@NotNull J.VariableDeclarations vd) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            if (initializer instanceof J.Literal) {
                initializer = expandWithPrimitivTypeHint(vd, initializer);
            }

            return template.apply(this.getCursor(), vd.getCoordinates().replace(), simpleName, initializer);
        }

        @NotNull
        private Expression expandWithPrimitivTypeHint(@NotNull J.VariableDeclarations vd, @NotNull Expression initializer) {
            String valueSource = ((J.Literal) initializer).getValueSource();

            if (isNull(valueSource)) return initializer;

            boolean isLongLiteral = JavaType.Primitive.Long.equals(vd.getType());
            boolean inferredAsLong = valueSource.endsWith("l") || valueSource.endsWith("L");
            boolean isFloatLiteral = JavaType.Primitive.Float.equals(vd.getType());
            boolean inferredAsFloat = valueSource.endsWith("f") || valueSource.endsWith("F");
            boolean isDoubleLiteral = JavaType.Primitive.Double.equals(vd.getType());
            boolean inferredAsDouble = valueSource.endsWith("d") || valueSource.endsWith("D") || valueSource.contains(".");

            String typNotation = null;
            if (isLongLiteral && !inferredAsLong) {
                typNotation = "L";
            } else if (isFloatLiteral && !inferredAsFloat) {
                typNotation = "F";
            } else if (isDoubleLiteral && !inferredAsDouble) {
                typNotation = "D";
            }

            if (nonNull(typNotation)) {
                initializer = ((J.Literal) initializer).withValueSource(format("%s%s", valueSource, typNotation));
            }

            return initializer;
        }
    }
}
