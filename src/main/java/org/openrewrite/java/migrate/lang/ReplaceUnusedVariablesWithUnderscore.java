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
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.staticanalysis.VariableReferences;

import java.util.function.Function;

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
        return Preconditions.check(new UsesJavaVersion<>(25), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                if (UNDERSCORE.equals(v.getSimpleName())) {
                    return v;
                }
                if (replaceIfUnusedInContext(v, J.ForEachLoop.class, J.ForEachLoop::getBody) ||
                        replaceIfUnusedInContext(v, J.Try.Catch.class, J.Try.Catch::getBody) ||
                        replaceIfUnusedInContext(v, J.Lambda.class, J.Lambda::getBody)) {
                    doAfterVisit(new RenameVariable<>(variable, UNDERSCORE));
                }
                return v;
            }

            private <T extends J> boolean replaceIfUnusedInContext(
                    J.VariableDeclarations.NamedVariable variable, Class<T> contextClass, Function<T, J> bodyExtractor) {
                T context = getCursor().firstEnclosing(contextClass);
                return context != null && VariableReferences.findRhsReferences(bodyExtractor.apply(context), variable.getName()).isEmpty();
            }
        });
    }
}
