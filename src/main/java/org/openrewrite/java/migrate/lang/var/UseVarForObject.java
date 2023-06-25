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
import org.openrewrite.java.tree.TypeTree;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarForObject extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `var` for reference-typed variables";
    }


    @Override
    public String getDescription() {
        //language=markdown
        return "Try to apply local variable type inference `var` to variables containing Objects where possible." +
               "This recipe will not touch variable declaration with genrics or initializer containing ternary operators.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForObjectVisitor());
    }


    static final class UseVarForObjectVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();


        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean usesGenerics = DeclarationCheck.useGenerics(vd);
            boolean usesTernary = DeclarationCheck.initializedByTernary(vd);
            if (isPrimitive || usesGenerics || usesTernary) return vd;

            return transformToVar(vd);
        }


        private J.VariableDeclarations transformToVar(J.VariableDeclarations vd) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            if (vd.getModifiers().isEmpty()) {
                return template.apply(getCursor(), vd.getCoordinates().replace(), simpleName, initializer)
                        .withPrefix(vd.getPrefix());
            } else {
                J.VariableDeclarations result = template.<J.VariableDeclarations>apply(getCursor(), vd.getCoordinates().replace(), simpleName, initializer)
                        .withModifiers(vd.getModifiers())
                        .withPrefix(vd.getPrefix());
                TypeTree typeExpression = result.getTypeExpression();
                //noinspection DataFlowIssue
                return typeExpression != null ? result.withTypeExpression(typeExpression.withPrefix(vd.getTypeExpression().getPrefix())) : vd;
            }
        }
    }
}
