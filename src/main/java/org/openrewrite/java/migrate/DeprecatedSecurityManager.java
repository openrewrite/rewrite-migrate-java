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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.List;

public class DeprecatedSecurityManager extends Recipe {

    @Override
    public String getDisplayName() {
        return "Call `System.setProperty(\"java.security.manager\")` before calling `java.lang.System setSecurityManager(java.lang.SecurityManager)`";
    }

    @Override
    public String getDescription() {
        return "The default value of the `java.security.manager` system property has been changed to disallow since Java 18." +
                " Unless the system property is set to allow on the command line, any invocation of System.setSecurityManager(SecurityManager) " +
                " with a non-null argument will throw an `UnsupportedOperationException`. " +
                " You can set system property as `Djava.security.manager=allow.` to prevent the exception" +
                "The recipe calls `System.setProperty(\"java.security.manager\")` before calling `java.lang.System setSecurityManager(SecurityManager)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(18),
                new JavaIsoVisitor<ExecutionContext>() {
                    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                        MethodMatcher SETSECURITY_REF = new MethodMatcher("java.lang.System setSecurityManager(java.lang.SecurityManager) ", true);
                        List<Statement> statements = block.getStatements();
                        boolean propertySet = false;
                        for (int i = 0; i < statements.size(); i++) {
                            Statement stmt = statements.get(i);
                            if (stmt instanceof J.MethodInvocation) {
                                if (SETSECURITY_REF.matches((J.MethodInvocation) stmt)) {
                                    for (Statement statement : statements) {
                                        if (statement instanceof J.MethodInvocation &&
                                                ((J.MethodInvocation) statement).getSimpleName().equals("setProperty") &&
                                                ((J.MethodInvocation) statement).getArguments().get(0) instanceof J.Literal &&
                                                ((J.Literal) ((J.MethodInvocation) statement).getArguments().get(0)).getValue().equals("java.security.manager")) {
                                            propertySet = true;
                                            break;
                                        }
                                    }
                                    String templateString = "System.setProperty(\"java.security.manager\", \"allow\");";
                                    if (!propertySet) {
                                        stmt = JavaTemplate.builder(templateString)
                                                .contextSensitive()
                                                .build().apply(new Cursor(getCursor(), stmt),
                                                        stmt.getCoordinates().replace());
                                        statements = ListUtils.insert(statements, stmt, i);
                                        return block.withStatements(statements);
                                    }
                                }
                            }
                        }
                        return super.visitBlock(block, ctx);
                    }
                });
    }
}
