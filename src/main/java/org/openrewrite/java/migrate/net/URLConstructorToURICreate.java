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
package org.openrewrite.java.migrate.net;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.trait.VariableAccess;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class URLConstructorToURICreate extends Recipe {

    private static final String URI_FQN = "java.net.URI";
    private static final String URL_FQN = "java.net.URL";
    private static final MethodMatcher SINGLE_ARG_CONSTRUCTOR = new MethodMatcher(URL_FQN + "#<init>(java.lang.String)");

    @Getter
    final String displayName = "Convert `new URL(String)` to `URI.create(String).toURL()`";

    @Getter
    final String description = "Converts `new URL(String)` constructor to `URI.create(String).toURL()`. The URL constructor has been deprecated due to security vulnerabilities when handling malformed URLs. Using `URI.create(String)` provides stronger validation and safer URL handling in modern Java applications.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(SINGLE_ARG_CONSTRUCTOR),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass nc, ExecutionContext ctx) {
                        if (SINGLE_ARG_CONSTRUCTOR.matches(nc)) {
                            String path = extractPath(nc.getArguments().get(0));
                            if (isNotValidPath(path)) {
                                return nc;
                            }

                            maybeRemoveImport(URL_FQN);
                            maybeAddImport(URI_FQN);
                            return JavaTemplate.builder("URI.create(#{any(String)}).toURL()")
                                    .imports(URI_FQN)
                                    .javaParser(JavaParser.fromJavaVersion())
                                    .build()
                                    .apply(getCursor(), nc.getCoordinates().replace(), nc.getArguments().get(0));
                        }
                        return super.visitNewClass(nc, ctx);
                    }

                    private @Nullable String extractPath(Expression arg) {
                        if (arg instanceof J.Literal &&
                                TypeUtils.isOfType(arg.getType(), JavaType.Primitive.String)) {
                            // Check if value is not null
                            String literalValueSource = ((J.Literal) arg).getValueSource();
                            // Remove quotations from string
                            return literalValueSource != null ? literalValueSource.substring(1, literalValueSource.length() - 1).trim() : null;
                        }
                        if (arg instanceof J.Identifier &&
                                TypeUtils.isOfType(arg.getType(), JavaType.Primitive.String)) {
                            return findLiteralInitializer((J.Identifier) arg);
                        }
                        // null indicates no path extractable
                        return null;
                    }

                    private @Nullable String findLiteralInitializer(J.Identifier identifier) {
                        return Scope.at(Scope.cursorTo(getCursor(), identifier)).lookup(identifier.getSimpleName())
                                .filter(binding -> !isAssigned(binding))
                                .map(binding -> binding.getCursor().getParentTreeCursor().getValue())
                                .filter(J.VariableDeclarations.NamedVariable.class::isInstance)
                                .map(variable -> ((J.VariableDeclarations.NamedVariable) variable).getInitializer())
                                .filter(initializer -> initializer instanceof J.Literal && ((J.Literal) initializer).getValue() instanceof String)
                                .map(initializer -> (String) ((J.Literal) initializer).getValue())
                                .orElse(null);
                    }

                    /**
                     * Whether code of the source file writes to the variable. Another class may write to a field by a
                     * qualified name, outside the field's scope. {@link Scope.Binding#isEffectivelyFinal()} would
                     * also count every non-final field as assigned.
                     */
                    private boolean isAssigned(Scope.Binding binding) {
                        if (binding.references().anyMatch(reference -> new VariableAccess(reference).isWriteAccess())) {
                            return true;
                        }
                        JavaType field = binding.getType();
                        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                        if (binding.isLocal() || !(field instanceof JavaType.Variable) || sourceFile == null) {
                            return false;
                        }
                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean written) {
                                if (TypeUtils.isOfType(identifier.getFieldType(), field) &&
                                    new VariableAccess(getCursor()).isWriteAccess()) {
                                    written.set(true);
                                }
                                return identifier;
                            }
                        }.reduce(sourceFile, new AtomicBoolean()).get();
                    }

                    private boolean isNotValidPath(@Nullable String path) {
                        if (path == null) {
                            return true;
                        }

                        try {
                            URI.create(path).toURL();
                            return false;
                        } catch (Exception e) {
                            return true;
                        }
                    }
                });
    }
}
