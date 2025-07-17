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

@EqualsAndHashCode(callSuper = false)
@Value
public class UseVarForObject extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `var` for reference-typed variables";
    }


    @Override
    public String getDescription() {
        //language=markdown
        return "Try to apply local variable type inference `var` to variables containing Objects where possible. " +
               "This recipe will not touch variable declarations with generics or initializers containing ternary operators.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForObjectVisitor());
    }


    static final class UseVarForObjectVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(getCursor(), vd);
            if (!isGeneralApplicable) {
                return vd;
            }

            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean usesGenerics = DeclarationCheck.useGenerics(vd);
            boolean usesTernary = DeclarationCheck.initializedByTernary(vd);
            Expression initializer = vd.getVariables().get(0).getInitializer();
            boolean usesArrayInitializer = initializer instanceof J.NewArray;
            boolean initializedByStaticMethod = DeclarationCheck.initializedByStaticMethod(initializer);
            if (isPrimitive || usesGenerics || usesTernary || usesArrayInitializer || initializedByStaticMethod) {
                return vd;
            }

            if (vd.getType() instanceof JavaType.FullyQualified) {
                maybeRemoveImport( (JavaType.FullyQualified) vd.getType() );
            }

            return DeclarationCheck.transformToVar(vd);
        }
    }
}
