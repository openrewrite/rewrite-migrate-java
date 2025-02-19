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

import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class URLConstructorsToURI extends ScanningRecipe<Set<String>> {
    @Override
    public String getDisplayName() {
        return "Convert `new URL(String)` to `URI.create(String).toURL()`";
    }

    @Override
    public String getDescription() {
        return "Converts `new URL(String)` constructors to `URI.create(String).toURL()`.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> wrapperMethodClasses) {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcherSingleArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String)");

            @Override
            public J.NewClass visitNewClass(J.NewClass nc, ExecutionContext ctx) {
                if (methodMatcherSingleArg.matches(nc)) {
                    Expression arg = nc.getArguments().get(0);
                    if (!(arg instanceof J.Literal && arg.getType().toString().equals("String"))) {
                        J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (cd != null) {
                            wrapperMethodClasses.add(cd.getType().getFullyQualifiedName());
                        }
                    }
                }
                return super.visitNewClass(nc, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> wrapperMethodClasses) {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcherSingleArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String)");
            private final MethodMatcher methodMatcherThreeArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, java.lang.String)");
            private final MethodMatcher methodMatcherFourArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, int, java.lang.String)");
            JavaType.Method methodType;


            @Override
            public J visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                boolean wrapperMethodExists = cd.getBody()
                        .getStatements()
                        .stream()
                        .filter(J.MethodDeclaration.class::isInstance)
                        .map(J.MethodDeclaration.class::cast)
                        .anyMatch(md -> md.getSimpleName().equals("transformNonLiteralURIToValidURL"));

                if (!wrapperMethodExists && wrapperMethodClasses.contains(cd.getType().getFullyQualifiedName())) {
                    JavaTemplate convertUriMethod = JavaTemplate.builder(
                                    "public URL transformNonLiteralURIToValidURL(String spec) {\n" +
                                            "       try {\n" +
                                            "           return URI.create(spec).toURL();\n" +
                                            "       } catch (Exception e) {\n" +
                                            "           return new URL(spec);\n" +
                                            "       }\n" +
                                            "}")
                            .contextSensitive()
                            .imports("java.net.URI", "java.net.URL")
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();
                    maybeAddImport("java.net.URI");

                    cd = convertUriMethod.apply(updateCursor(cd), cd.getBody().getCoordinates().lastStatement());

                    List<Statement> statements = cd.getBody().getStatements();
                    J.MethodDeclaration md = (J.MethodDeclaration) statements.get(statements.size() - 1);
                    methodType = md.getMethodType();
                }
                return super.visitClassDeclaration(cd, ctx);
            }

            @Override
            public J visitNewClass(J.NewClass nc, ExecutionContext ctx) {
                J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod != null && enclosingMethod.getSimpleName().equals("transformNonLiteralURIToValidURL")) {
                    return super.visitNewClass(nc, ctx);
                }

                if (methodMatcherSingleArg.matches(nc)) {
                    Expression arg = nc.getArguments().get(0);

                    if (arg instanceof J.Literal && arg.getType().toString().equals("String")) {
                        // If the argument is a string literal, only convert the constructor if the argument is a valid input

                        String literalValue = ((J.Literal) arg).getValueSource();

                        if (literalValue == null) {
                            return nc;
                        }

                        // Remove quotations from string
                        literalValue = literalValue.substring(1);
                        literalValue = literalValue.substring(0, literalValue.length() - 1);


                        // Check that this string is a valid input for URI.create().toURL()
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            URI.create(literalValue).toURL();
                        } catch (Exception e) {
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
                    } else {
                        // If the argument is not a string literal, replace the constructor with the wrapper method
                        JavaTemplate template = JavaTemplate.builder("transformNonLiteralURIToValidURL(#{any(String)})")
                                .imports("java.net.URI")
                                .contextSensitive()
                                .javaParser(JavaParser.fromJavaVersion())
                                .build();
                        J.MethodInvocation result = template.apply(updateCursor(nc), nc.getCoordinates().replace(), nc.getArguments().get(0));
                        result = result.withMethodType(methodType);
                        J.Identifier name = result.getName();
                        name = name.withType(methodType);
                        result = result.withName(name);
                        maybeAddImport("java.net.URI");
                        return result;
                    }
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
}
