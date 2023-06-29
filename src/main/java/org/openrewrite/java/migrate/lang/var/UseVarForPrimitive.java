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
package org.openrewrite.java.migrate.lang.var;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static java.lang.String.format;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarForPrimitive extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `var` for primitive-typed variables";
    }


    @Override
    public String getDescription() {
        //language=markdown
        return "Try to apply local variable type inference `var` to primitiv variables where possible." +
               "This recipe will not touch variable declaration with initializer containing ternary operators.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new VarForPrimitivesVisitor());
    }

    static final class VarForPrimitivesVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final JavaType.Primitive SHORT_TYPE = JavaType.Primitive.Short;
        private final JavaType.Primitive BYTE_TYPE = JavaType.Primitive.Byte;

        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();


        @Override
        public J.VariableDeclarations visitVariableDeclarations( J.VariableDeclarations vd,  ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(this.getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            // recipe specific
            boolean isNoPrimitive = !DeclarationCheck.isPrimitive(vd);
            boolean isByteVariable = DeclarationCheck.declarationHasType(vd, BYTE_TYPE);
            boolean isShortVariable = DeclarationCheck.declarationHasType(vd, SHORT_TYPE);
            if (isNoPrimitive || isByteVariable || isShortVariable) return vd;

            return transformToVar(vd);
        }


        private J.VariableDeclarations transformToVar( J.VariableDeclarations vd) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            if (initializer instanceof J.Literal) {
                initializer = expandWithPrimitivTypeHint(vd, initializer);
            }

            if (vd.getModifiers().isEmpty()) {
                return template.apply(getCursor(), vd.getCoordinates().replace(), simpleName, initializer)
                        .withPrefix(vd.getPrefix());
            } else {
                J.VariableDeclarations result = template.<J.VariableDeclarations>apply(getCursor(), vd.getCoordinates().replace(), simpleName, initializer)
                        .withModifiers(vd.getModifiers())
                        .withPrefix(vd.getPrefix());
                //noinspection DataFlowIssue
                return result.withTypeExpression(result.getTypeExpression().withPrefix(vd.getTypeExpression().getPrefix()));
            }
        }


        private Expression expandWithPrimitivTypeHint( J.VariableDeclarations vd,  Expression initializer) {
            String valueSource = ((J.Literal) initializer).getValueSource();

            if (valueSource == null) return initializer;

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

            if (typNotation != null) {
                initializer = ((J.Literal) initializer).withValueSource(format("%s%s", valueSource, typNotation));
            }

            return initializer;
        }
    }
}
