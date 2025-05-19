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
package org.openrewrite.java.migrate.util;


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

            Statement first = statements.get(0);
            if (!(first instanceof J.MethodInvocation)) {
                return method;
            }
            J.MethodInvocation methodInvocation = (J.MethodInvocation) first;
            if (!"super".equals(methodInvocation.getSimpleName())) {
                return method;
            }

            // Move super() to the end
            List<Statement> updated = new java.util.ArrayList<>(statements);
            updated.remove(0);
            updated.add(methodInvocation);

            return method.withBody(method.getBody().withStatements(updated));
        }
    }
}
