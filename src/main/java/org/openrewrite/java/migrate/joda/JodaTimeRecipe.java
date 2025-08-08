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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JavaType;

import java.util.*;

import static java.util.Collections.emptyList;

@EqualsAndHashCode(callSuper = false)
@Value
public class JodaTimeRecipe extends ScanningRecipe<JodaTimeRecipe.Accumulator> {

    /**
     * Controls whether additional safety checks are performed during the migration process.
     * When enabled, the recipe will verify that expressions are safe to migrate before performing the migration.
     * This helps prevent potential issues or bugs that might arise from automatic migration.
     */
    @Option(displayName = "Enable safe migration",
      description = "When enabled, performs additional safety checks to verify that expressions are safe to migrate before converting them. " +
                    "Safety checks include analyzing method parameters, return values, and variable usages across class boundaries.",
      required = false
    )
    @Nullable
    Boolean safeMigration;

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
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JodaTimeScanner(acc, Boolean.TRUE.equals(safeMigration));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        JodaTimeVisitor jodaTimeVisitor = new JodaTimeVisitor(acc, Boolean.TRUE.equals(safeMigration), new LinkedList<>());
        return Preconditions.check(new UsesType<>("org.joda.time.*", true), jodaTimeVisitor);
    }

    @Getter
    public static class Accumulator {
        private final Set<NamedVariable> unsafeVars = new HashSet<>();
        private final Map<JavaType.Method, Boolean> safeMethodMap = new HashMap<>();
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
            return vars.getOrDefault(declaringType, emptyList()).stream()
                    .filter(v -> v.getSimpleName().equals(varName))
                    .findFirst() // there should be only one variable with the same name
                    .orElse(null);
        }
    }
}
