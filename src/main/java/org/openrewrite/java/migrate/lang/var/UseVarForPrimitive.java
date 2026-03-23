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

import static java.lang.String.format;
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
            if (DeclarationCheck.isPrimitive(vd)) {
                boolean isByteVariable = DeclarationCheck.declarationHasType(vd, Byte);
                boolean isShortVariable = DeclarationCheck.declarationHasType(vd, Short);
                if (isByteVariable || isShortVariable) {
                    return vd;
                }
                return DeclarationCheck.transformToVar(vd, it -> it instanceof J.Literal ? expandWithPrimitivTypeHint(vd, it) : it);
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

        private Expression expandWithPrimitivTypeHint(J.VariableDeclarations vd, Expression initializer) {
            String valueSource = ((J.Literal) initializer).getValueSource();

            if (valueSource == null) {
                return initializer;
            }

            boolean isLongLiteral = Long == vd.getType();
            boolean inferredAsLong = valueSource.endsWith("l") || valueSource.endsWith("L");
            boolean isFloatLiteral = Float == vd.getType();
            boolean inferredAsFloat = valueSource.endsWith("f") || valueSource.endsWith("F");
            boolean isDoubleLiteral = Double == vd.getType();
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
