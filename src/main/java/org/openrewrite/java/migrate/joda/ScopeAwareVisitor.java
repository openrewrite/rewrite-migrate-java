/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.joda;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
class ScopeAwareVisitor extends JavaVisitor<ExecutionContext> {
    protected final LinkedList<VariablesInScope> scopes;

    @Override
    public J preVisit(J j, ExecutionContext ctx) {
        if (j instanceof J.Block) {
            scopes.push(new VariablesInScope(getCursor()));
        }
        if (j instanceof J.MethodDeclaration) {
            scopes.push(new VariablesInScope(getCursor()));
        }
        if (j instanceof J.VariableDeclarations.NamedVariable) {
            assert !scopes.isEmpty();
            NamedVariable variable = (NamedVariable) j;
            scopes.peek().variables.add(variable);
        }
        return super.preVisit(j, ctx);
    }

    @Override
    public J postVisit(J j, ExecutionContext ctx) {
        if (j instanceof J.Block) {
            scopes.pop();
        }
        return super.postVisit(j, ctx);
    }

    Cursor findScope(NamedVariable variable) {
        for (VariablesInScope scope : scopes) {
            if (scope.variables.contains(variable)) {
                return scope.scope;
            }
        }
        return null;
    }

    // Returns the variable in the closest scope
    Optional<NamedVariable> findVarInScope(String varName) {
        for (VariablesInScope scope : scopes) {
            for (NamedVariable var : scope.variables) {
                if (var.getSimpleName().equals(varName)) {
                    return Optional.of(var);
                }
            }
        }
        return Optional.empty();
    }

    Cursor getCurrentScope() {
        assert !scopes.isEmpty();
        return scopes.peek().scope;
    }

    @Value
    public static class VariablesInScope {
        Cursor scope;
        Set<J.VariableDeclarations.NamedVariable> variables;

        public VariablesInScope(Cursor scope) {
            this.scope = scope;
            this.variables = new HashSet<>();
        }
    }
}
