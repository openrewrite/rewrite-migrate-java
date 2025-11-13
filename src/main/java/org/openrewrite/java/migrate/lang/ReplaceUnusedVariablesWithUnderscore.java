/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.VariableReferences;

import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceUnusedVariablesWithUnderscore extends Recipe {

    private static final String UNDERSCORE = "_";

    @Override
    public String getDisplayName() {
        return "Replace unused variables with underscore";
    }

    @Override
    public String getDescription() {
        return "Replace unused variable declarations with underscore (_) for Java 22+. " +
                "This includes unused variables in enhanced for loops, catch blocks, " +
                "and lambda parameters where the variable is never referenced.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(22), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ForEachLoop visitForEachLoop(J.ForEachLoop forLoop, ExecutionContext ctx) {
                J.ForEachLoop l = super.visitForEachLoop(forLoop, ctx);
                Statement variable = l.getControl().getVariable();
                if (variable instanceof J.VariableDeclarations) {
                    for (J.VariableDeclarations.NamedVariable namedVariable : ((J.VariableDeclarations) variable).getVariables()) {
                        renameVariableIfUnusedInContext(namedVariable, l.getBody());
                    }
                }
                return l;
            }

            @Override
            public J.Try.Catch visitCatch(J.Try.Catch _catch, ExecutionContext ctx) {
                J.Try.Catch c = super.visitCatch(_catch, ctx);
                for (J.VariableDeclarations.NamedVariable namedVariable : c.getParameter().getTree().getVariables()) {
                    renameVariableIfUnusedInContext(namedVariable, c.getBody());
                }
                return c;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                J.Lambda l = super.visitLambda(lambda, ctx);
                for (J param : l.getParameters().getParameters()) {
                    if (param instanceof J.VariableDeclarations) {
                        for (J.VariableDeclarations.NamedVariable namedVariable : ((J.VariableDeclarations) param).getVariables()) {
                            renameVariableIfUnusedInContext(namedVariable, l.getBody());
                        }
                    }
                }
                return l;
            }

            private void renameVariableIfUnusedInContext(J.VariableDeclarations.NamedVariable variable, J context) {
                if (!UNDERSCORE.equals(variable.getName().getSimpleName()) &&
                        VariableReferences.findRhsReferences(context, variable.getName()).isEmpty() &&
                        !usedInModifyingUnary(variable.getName(), context)) {
                    doAfterVisit(new RenameVariable<>(variable, UNDERSCORE));
                }
            }

            private boolean usedInModifyingUnary(J.Identifier identifier, J context) {
                return new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Unary visitUnary(J.Unary unary, AtomicBoolean atomicBoolean) {
                        if (unary.getOperator().isModifying() &&
                                SemanticallyEqual.areEqual(identifier, unary.getExpression())) {
                            atomicBoolean.set(true);
                        }
                        return super.visitUnary(unary, atomicBoolean);
                    }
                }.reduce(context, new AtomicBoolean(false)).get();
            }
        });
    }
}
