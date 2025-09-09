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
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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

                Optional<J.VariableDeclarations.NamedVariable> result = replaceIfUnusedInContext(v, J.ForEachLoop.class, J.ForEachLoop::getBody);
                if (result.isPresent()) {
                    return result.get();
                }

                result = replaceIfUnusedInContext(v, J.Try.Catch.class, J.Try.Catch::getBody);
                if (result.isPresent()) {
                    return result.get();
                }

                result = replaceIfUnusedInContext(v, J.Lambda.class, J.Lambda::getBody);
                return result.orElse(v);

            }

            private <T extends J> Optional<J.VariableDeclarations.NamedVariable> replaceIfUnusedInContext(
                    J.VariableDeclarations.NamedVariable variable,
                    Class<T> contextClass,
                    Function<T, J> bodyExtractor) {
                T context = getCursor().firstEnclosing(contextClass);
                if (context != null && !isVariableUsedInStatement(bodyExtractor.apply(context), variable.getSimpleName())) {
                    return Optional.of(replaceWithUnderscore(variable));
                }
                return Optional.empty();
            }

            private J.VariableDeclarations.NamedVariable replaceWithUnderscore(J.VariableDeclarations.NamedVariable variable) {
                return variable.withName(variable.getName()
                                .withSimpleName(UNDERSCORE)
                                .withFieldType(variable.getName().getFieldType().withName(UNDERSCORE)))
                        .withVariableType(variable.getVariableType().withName(UNDERSCORE));
            }

            private boolean isVariableUsedInStatement(J statement, String varName) {
                return new JavaIsoVisitor<AtomicBoolean>() {
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
                }.reduce(statement, new AtomicBoolean(false)).get();
            }
        });
    }
}
