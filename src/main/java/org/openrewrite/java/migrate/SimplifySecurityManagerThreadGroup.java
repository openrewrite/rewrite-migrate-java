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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Value
@EqualsAndHashCode(callSuper = false)
public class SimplifySecurityManagerThreadGroup extends Recipe {
    String displayName = "Simplify the removed security manager's thread group fallback";
    String description = "Remove a null SecurityManager local used only by the immediately following thread group conditional.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(25), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                List<Statement> statements = new ArrayList<>(b.getStatements());
                for (int i = 0; i + 1 < statements.size(); i++) {
                    if (!(statements.get(i) instanceof J.VariableDeclarations) ||
                        !(statements.get(i + 1) instanceof J.Assignment)) {
                        continue;
                    }
                    J.VariableDeclarations declaration = (J.VariableDeclarations) statements.get(i);
                    if (declaration.getVariables().size() != 1 || !declaration.getComments().isEmpty() ||
                        !TypeUtils.isOfClassType(declaration.getType(), "java.lang.SecurityManager")) {
                        continue;
                    }
                    J.VariableDeclarations.NamedVariable variable = declaration.getVariables().get(0);
                    if (!J.Literal.isLiteralValue(variable.getInitializer(), null) || variable.getVariableType() == null) {
                        continue;
                    }
                    J.Assignment assignment = (J.Assignment) statements.get(i + 1);
                    if (!(assignment.getAssignment() instanceof J.Ternary)) {
                        continue;
                    }
                    J.Ternary ternary = (J.Ternary) assignment.getAssignment();
                    Expression condition = ternary.getCondition().unwrap();
                    if (!(condition instanceof J.Binary)) {
                        continue;
                    }
                    J.Binary binary = (J.Binary) condition;
                    if (binary.getOperator() != J.Binary.Type.NotEqual ||
                        !sameVariable(binary.getLeft(), variable) || !J.Literal.isLiteralValue(binary.getRight(), null) ||
                        !(ternary.getTruePart() instanceof J.MethodInvocation)) {
                        continue;
                    }
                    J.MethodInvocation method = (J.MethodInvocation) ternary.getTruePart();
                    if (!"getThreadGroup".equals(method.getSimpleName()) || !sameVariable(method.getSelect(), variable)) {
                        continue;
                    }
                    AtomicInteger references = new AtomicInteger();
                    new JavaIsoVisitor<AtomicInteger>() {
                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicInteger count) {
                            if (sameVariable(identifier, variable) && !identifier.getId().equals(variable.getName().getId())) {
                                count.incrementAndGet();
                            }
                            return identifier;
                        }
                    }.visit(b, references);
                    if (references.get() != 2) {
                        continue;
                    }
                    statements.set(i + 1, assignment.withAssignment(ternary.getFalsePart().withPrefix(ternary.getPrefix())));
                    statements.remove(i--);
                }
                return b.withStatements(statements);
            }

            private boolean sameVariable(Expression expression, J.VariableDeclarations.NamedVariable variable) {
                return expression instanceof J.Identifier && variable.getVariableType().equals(((J.Identifier) expression).getFieldType());
            }
        });
    }
}
