/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.lang.var;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static org.openrewrite.java.tree.JavaType.Primitive.*;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseVarForPrimitive extends Recipe {

    String displayName = "Use `var` for primitive and String variables";


    String description = "Try to apply local variable type inference `var` to primitive and String literal variables where possible. " +
            "This recipe will not touch variable declarations with initializers containing ternary operators.";


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new VarForPrimitivesVisitor());
    }

    static final class VarForPrimitivesVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vardecl, ExecutionContext ctx) {
            J.VariableDeclarations vd = super.visitVariableDeclarations(vardecl, ctx);

            if (!DeclarationCheck.isVarApplicable(this.getCursor(), vd)) {
                return vd;
            }

            if (isStringLiteralInitializer(vd)) {
                return DeclarationCheck.transformToVar(vd);
            }
            if (DeclarationCheck.isPrimitive(vd) &&
                    !DeclarationCheck.declarationHasType(vd, Byte) &&
                    !DeclarationCheck.declarationHasType(vd, Short)) {
                Expression initializer = vd.getVariables().get(0).getInitializer();
                if (initializer == null) {
                    return vd;
                }
                if (initializer.getType() == vd.getType()) {
                    return DeclarationCheck.transformToVar(vd);
                }
                if (initializer instanceof J.Literal) {
                    J.Literal literal = (J.Literal) initializer;
                    String suffix = typeSuffix(vd.getType(), literal);
                    if (suffix != null) {
                        return DeclarationCheck.transformToVar(vd, it -> literal
                                .withValueSource(literal.getValueSource() + suffix)
                                .withType(vd.getType()));
                    }
                }
            }
            return vd;
        }

        private boolean isStringLiteralInitializer(J.VariableDeclarations vd) {
            if (!TypeUtils.isOfClassType(vd.getType(), "java.lang.String")) {
                return false;
            }
            Expression initializer = vd.getVariables().get(0).getInitializer();
            return initializer != null && initializer.unwrap() instanceof J.Literal;
        }

        private @Nullable String typeSuffix(@Nullable JavaType declaredType, J.Literal literal) {
            String source = literal.getValueSource();
            if (source == null) {
                return null;
            }
            if (literal.getType() == Int) {
                if (declaredType == Long) {
                    return "L";
                }
                // Hex, octal and binary int literals change value when read as a floating point literal
                String digits = source.startsWith("-") ? source.substring(1) : source;
                boolean decimal = "0".equals(digits) || !digits.startsWith("0");
                if (decimal && declaredType == Float) {
                    return "F";
                }
                if (decimal && declaredType == Double) {
                    return "D";
                }
            } else if (literal.getType() == Double && declaredType == Float &&
                    Character.isDigit(source.charAt(source.length() - 1))) {
                return "F";
            }
            return null;
        }
    }
}
