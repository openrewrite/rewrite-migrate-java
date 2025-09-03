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
package org.openrewrite.java.migrate;


import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class RelocateSuperCall extends Recipe {

    @Override
    public String getDisplayName() {
        return "Move `super()` after conditionals (Java 25+)";
    }

    @Override
    public String getDescription() {
        return "Relocates `super()` calls to take advantage of the early construction context introduced by JEP 513 in Java 25+, allowing statements before constructor calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(25),
                new RelocateSuperCallVisitor());
    }

    private static class RelocateSuperCallVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (!method.isConstructor() || method.getBody() == null) {
                return method;
            }

            List<Statement> statements = method.getBody().getStatements();
            if (statements.size() < 2) {
                return method;
            }


            int superCallIdx = -1;
            for (int i = 0; i < statements.size(); i++) {
                Statement stmt = statements.get(i);

                // Check if this statement contains a super() call
                if (stmt.printTrimmed(getCursor()).startsWith("super(")) {
                    superCallIdx = i;
                    break;
                } else if (stmt instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) stmt;
                    if ("super".equals(mi.getSimpleName())) {
                        superCallIdx = i;
                        break;
                    }
                }
            }
            // Move super() to the end
            if (superCallIdx == -1) {
                return method;
            }

            // Check for forbidden usages before super()
            for (int i = 0; i < superCallIdx; i++) {
                Statement stmt = statements.get(i);
                // Example checks (expand as needed):
                if (stmt instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) stmt;
                    if (mi.getSelect() != null) {
                        String select = mi.getSelect().printTrimmed();
                        if ("this".equals(select) || "super".equals(select)) {
                            // Log or comment: forbidden usage before super()
                            // ctx.getOnError().accept(...);
                        }
                    }
                }
                if (stmt instanceof J.Assignment) {
                    J.Assignment assign = (J.Assignment) stmt;
                    // Check assignment to initialized fields (requires symbol table)
                    // For now, just log assignment
                }
                // Add more checks for field accesses, etc.
            }

            // Find optimal position for super() call
            int optimalPosition = findOptimalSuperPosition(statements, superCallIdx);

            if (optimalPosition == superCallIdx) {
                return method; // No change needed
            }

            List<Statement> updated = new java.util.ArrayList<>(statements);
            Statement superCall = updated.remove(superCallIdx);

            // Adjust insertion position if we removed an element before it
            int insertPos = optimalPosition > superCallIdx ? optimalPosition - 1 : optimalPosition;
            updated.add(insertPos, superCall);

            return method.withBody(method.getBody().withStatements(updated));

        }

        private int findOptimalSuperPosition(List<Statement> statements, int currentSuperIdx) {
            // Find the latest position where super() can be safely placed
            int optimalPosition = 0; // Start at the beginning

            for (int i = 0; i < statements.size(); i++) {
                if (i == currentSuperIdx) continue; // Skip the current super() call

                Statement stmt = statements.get(i);

                // Check for invalid field reads (this.field access that's not assignment)
                if (containsThisFieldAccess(stmt)) {
                    // Found an invalid operation - super() should be before this
                    return optimalPosition;
                }

                // If it's a safe operation (assignment, conditionals, etc.), we can move past it
                if (isSafeBeforeSuper(stmt)) {
                    optimalPosition = i + 1;
                }
            }

            // If no invalid operations found, super() can go at the end
            return optimalPosition;
        }

        private boolean containsThisFieldAccess(Statement stmt) {
            // Check if statement contains this.field read (not assignment)
            String stmtStr = stmt.printTrimmed();

            if (stmtStr.contains("this.")) {
                // If it's an assignment to this.field, it's safe
                if (stmtStr.trim().startsWith("this.") && stmtStr.contains(" = ")) {
                    return false;
                }
                // Any other this.field usage is invalid before super()
                return true;
            }
            return false;
        }

        private boolean isSafeBeforeSuper(Statement stmt) {
            // Safe operations that can happen before super() in Java 25+
            if (stmt instanceof J.Assignment) {
                J.Assignment assign = (J.Assignment) stmt;
                // Check if it's an assignment to this.field
                if (assign.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) assign.getVariable();
                    if (fa.getTarget() instanceof J.Identifier &&
                        "this".equals(((J.Identifier) fa.getTarget()).getSimpleName())) {
                        return true; // this.field = value is safe
                    }
                }
            }

            // Conditionals and variable declarations are generally safe
            if (stmt instanceof J.If || stmt instanceof J.VariableDeclarations) {
                return true;
            }

            return false;
        }

    }
}
