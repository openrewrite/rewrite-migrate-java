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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceUnusedVariablesWithUnderscore extends Recipe {

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
        return Preconditions.check(
                new UsesJavaVersion<>(22),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);

                        if ("_".equals(v.getSimpleName())) {
                            return v;
                        }

                        String varName = v.getSimpleName();

                        // Check if this variable should be replaced with underscore
                        J.ForEachLoop forLoop = getCursor().firstEnclosing(J.ForEachLoop.class);
                        if (forLoop != null && !isVariableUsedInStatement(forLoop.getBody(), varName)) {
                            J.VariableDeclarations.NamedVariable namedVariable = v.withName(v.getName().withSimpleName("_").withFieldType(v.getName().getFieldType().withName("_"))).withVariableType(v.getVariableType().withName("_"));
                            return namedVariable;
                        }

                        J.Try.Catch catchClause = getCursor().firstEnclosing(J.Try.Catch.class);
                        if (catchClause != null && !isVariableUsedInStatement(catchClause.getBody(), varName)) {
                            J.VariableDeclarations.NamedVariable namedVariable = v.withName(v.getName().withSimpleName("_").withFieldType(v.getName().getFieldType().withName("_"))).withVariableType(v.getVariableType().withName("_"));
                            return namedVariable;
                        }

                        J.Lambda lambda = getCursor().firstEnclosing(J.Lambda.class);
                        if (lambda != null && !isVariableUsedInStatement(lambda.getBody(), varName)) {
                            J.VariableDeclarations.NamedVariable namedVariable = v.withName(v.getName().withSimpleName("_").withFieldType(v.getName().getFieldType().withName("_"))).withVariableType(v.getVariableType().withName("_"));
                            return namedVariable;
                        }

                        return v;
                    }

                    private boolean isVariableUsedInStatement(J statement, String varName) {
                        if (statement == null || "_".equals(varName)) {
                            return false;
                        }

                        return new VariableUsageVisitor(varName).reduce(statement, new AtomicBoolean(false)).get();
                    }

                    private class VariableUsageVisitor extends JavaIsoVisitor<AtomicBoolean> {
                        private final String varName;

                        VariableUsageVisitor(String varName) {
                            this.varName = varName;
                        }

                        @Override
                        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean used) {
                            if (varName.equals(identifier.getSimpleName())) {
                                if (!(getCursor().getParent().getValue() instanceof J.VariableDeclarations.NamedVariable)) {
                                    used.set(true);
                                    return identifier;
                                }
                            }
                            return super.visitIdentifier(identifier, used);
                        }
                    }
                });
    }
}
