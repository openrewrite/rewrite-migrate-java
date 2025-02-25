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

import fj.data.Option;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.analysis.constantfold.ConstantFold;
import org.openrewrite.analysis.util.CursorUtil;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.net.URI;

public class URLConstructorsToURI extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert `new URL(String)` to `URI.create(String).toURL()`";
    }

    @Override
    public String getDescription() {
        return "Converts `new URL(String)` constructors to `URI.create(String).toURL()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcherSingleArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String)");
            private final MethodMatcher methodMatcherThreeArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, java.lang.String)");
            private final MethodMatcher methodMatcherFourArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, int, java.lang.String)");

            @Override
            public J visitNewClass(J.NewClass nc, ExecutionContext ctx) {
                if (methodMatcherSingleArg.matches(nc)) {
                    Expression arg = nc.getArguments().get(0);

                    if (arg instanceof J.Literal) {
                        // Check if the literal is of type String
                        if (!arg.getType().toString().equals("String")) {
                            return nc;
                        }

                        // Check if value is null
                        String literalValue = ((J.Literal) arg).getValueSource();
                        if (literalValue == null) {
                            return nc;
                        }

                        // Remove quotations from string
                        literalValue = literalValue.substring(1);
                        literalValue = literalValue.substring(0, literalValue.length() - 1);


                        // Check that this string is a valid input for URI.create().toURL()
                        if (isNotValidPath(literalValue)) {
                            return nc;
                        }

                    } else if (arg instanceof J.Identifier) {
                        // Check if type is String
                        JavaType type = arg.getType();
                        if (type == null || !type.toString().equals("java.lang.String")) {
                            return nc;
                        }


                        // Check if constant value is valid
                        String constantValue = null;
                        Option<String> constant = CursorUtil.findCursorForTree(getCursor(), arg)
                                .bind(c -> ConstantFold.findConstantLiteralValue(c, String.class));

                        if (constant.isSome()) {
                            constantValue = constant.some();
                        }

                        if (isNotValidPath(constantValue)) {
                            return nc;
                        }

                    } else {
                        return nc;
                    }

                    JavaTemplate template = JavaTemplate.builder("URI.create(#{any(String)}).toURL()")
                            .imports("java.net.URI")
                            .contextSensitive()
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();

                    J result = template.apply(getCursor(),
                            nc.getCoordinates().replace(),
                            nc.getArguments().get(0));
                    maybeAddImport("java.net.URI");

                    return result;
                } else if (methodMatcherThreeArg.matches(nc)) {
                    JavaTemplate template = JavaTemplate.builder("new URI(#{any(String)}, null, #{any(String)}, -1, #{any(String)}, null, null).toURL()")
                            .imports("java.net.URI", "java.net.URL")
                            .contextSensitive()
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();

                    J result = template.apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0),
                            nc.getArguments().get(1),
                            nc.getArguments().get(2));
                    maybeAddImport("java.net.URI");
                    return result;
                } else if (methodMatcherFourArg.matches(nc)) {
                    JavaTemplate template = JavaTemplate.builder("new URI(#{any(String)}, null, #{any(String)}, #{any(int)}, #{any(String)}, null, null).toURL()")
                            .imports("java.net.URI", "java.net.URL")
                            .contextSensitive()
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();

                    J result = template.apply(getCursor(), nc.getCoordinates().replace(),
                            nc.getArguments().get(0),
                            nc.getArguments().get(1),
                            nc.getArguments().get(2),
                            nc.getArguments().get(3));
                    maybeAddImport("java.net.URI");
                    return result;
                }
                return super.visitNewClass(nc, ctx);
            }
        };
    }

    private static boolean isNotValidPath(@Nullable String path) {
        if (path == null) {
            return true;
        }

        try {
            //noinspection ResultOfMethodCallIgnored
            URI.create(path).toURL();
        } catch (Exception e) {
            return true;
        }

        return false;
    }
}
