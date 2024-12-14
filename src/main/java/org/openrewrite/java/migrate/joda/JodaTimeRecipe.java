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
package org.openrewrite.java.migrate.joda;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JavaType;

import java.util.*;

public class JodaTimeRecipe extends ScanningRecipe<JodaTimeRecipe.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Migrate Joda-Time to Java time";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Joda Time.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public JodaTimeScanner getScanner(Accumulator acc) {
        return new JodaTimeScanner(acc);
    }

    @Override
    public JodaTimeVisitor getVisitor(Accumulator acc) {
        return new JodaTimeVisitor(acc, true, new LinkedList<>());
    }

    @Getter
    public static class Accumulator {
        private final Set<NamedVariable> unsafeVars = new HashSet<>();
        private final VarTable varTable = new VarTable();
    }

    static class VarTable {
        private final Map<JavaType, List<NamedVariable>> vars = new HashMap<>();

        public void addVars(J.MethodDeclaration methodDeclaration) {
            JavaType type = methodDeclaration.getMethodType();
            assert type != null;
            methodDeclaration.getParameters().forEach(p -> {
                if (!(p instanceof J.VariableDeclarations)) {
                    return;
                }
                J.VariableDeclarations.NamedVariable namedVariable = ((J.VariableDeclarations) p).getVariables().get(0);
                vars.computeIfAbsent(type, k -> new ArrayList<>()).add(namedVariable);
            });
        }

        public @Nullable NamedVariable getVarByName(@Nullable JavaType declaringType, String varName) {
            return vars.getOrDefault(declaringType, Collections.emptyList()).stream()
                    .filter(v -> v.getSimpleName().equals(varName))
                    .findFirst() // there should be only one variable with the same name
                    .orElse(null);
        }
    }
}
