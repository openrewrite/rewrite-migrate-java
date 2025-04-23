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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.net.URI;

public class URLConstructorToURICreate extends Recipe {

    private static final String URI_FQN = "java.net.URI";
    private static final String URL_FQN = "java.net.URL";
    private static final MethodMatcher methodMatcherSingleArg = new MethodMatcher(URL_FQN + "#<init>(java.lang.String)");

    @Override
    public String getDisplayName() {
        return "Convert `new URL(String)` to `URI.create(String).toURL()`";
    }

    @Override
    public String getDescription() {
        return "Converts `new URL(String)` constructor to `URI.create(String).toURL()`. The URL constructor has been deprecated due to security vulnerabilities when handling malformed URLs. Using `URI.create(String)` provides stronger validation and safer URL handling in modern Java applications.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(URL_FQN, false),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass nc, ExecutionContext ctx) {
                        if (methodMatcherSingleArg.matches(nc)) {
                            String path = extractPath(nc.getArguments().get(0));
                            if (isNotValidPath(path)) {
                                return nc;
                            }

                            JavaTemplate template = JavaTemplate.builder("URI.create(#{any(String)}).toURL()")
                                    .imports(URI_FQN)
                                    .javaParser(JavaParser.fromJavaVersion())
                                    .build();
                            maybeAddImport(URI_FQN);
                            maybeRemoveImport(URL_FQN);

                            return template.apply(getCursor(),
                                    nc.getCoordinates().replace(),
                                    nc.getArguments().get(0));
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
                        } else if (arg instanceof J.Identifier &&
                                TypeUtils.isOfType(arg.getType(), JavaType.Primitive.String)) {
                            // find constant value of the identifier
                            return CursorUtil.findCursorForTree(getCursor(), arg)
                                    .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class))
                                    .toNull();
                        } else {
                            // null indicates no path extractable
                            return null;
                        }
                    }

                    private boolean isNotValidPath(@Nullable String path) {
                        if (path == null) {
                            return true;
                        }

                        try {
                            //noinspection ResultOfMethodCallIgnored
                            URI.create(path).toURL();
                            return false;
                        } catch (Exception e) {
                            return true;
                        }
                    }
                });
    }
}
