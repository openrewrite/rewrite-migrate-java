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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

public class URLConstructorsToURI_mock extends Recipe {
    @Override
    public String getDisplayName() {
        return "Custom Migration: Replace new `URL(String)` with `transformNonLiteralURIToValidURL(String)`";
    }

    @Override
    public String getDescription() {
        return "Standardizes URL creation by replacing new `URL(String)` with `transformNonLiteralURIToValidURL(String)`," +
               "ensuring consistent handling of absolute and relative paths.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            private final MethodMatcher methodMatcherSingleArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String)");
            private final MethodMatcher methodMatcherThreeArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, java.lang.String)");
            private final MethodMatcher methodMatcherFourArg = new MethodMatcher("java.net.URL <constructor>(java.lang.String, java.lang.String, int, java.lang.String)");
            boolean methodAdded = false;

            @Override
            public J visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {

                cd = (J.ClassDeclaration) super.visitClassDeclaration(cd, ctx);

                boolean methodExists = cd.getBody().getStatements().stream().filter(J.MethodDeclaration.class::isInstance).map(
                                J.MethodDeclaration.class::cast
                        )
                        .anyMatch(
                                md -> md.getSimpleName().equals("transformNonLiteralURIToValidURL")
                        );

                if (!methodExists && methodAdded) {
                    JavaTemplate convertUriMethod = JavaTemplate.builder(
                                    "public URL transformNonLiteralURIToValidURL(String spec) {\n" +
                                    "       if (URI.create(spec).isAbsolute()) {\n" +
                                    "           return URI.create(spec).toURL();\n" +
                                    "       } else {\n" +
                                    "           return new URL(spec);\n" +
                                    "       }\n" +
                                    "}")
                            .contextSensitive()
                            .imports("java.net.URI", "java.net.URL")
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();
                    maybeAddImport("java.net.URI");
                    getCursor().putMessage("alreadyTransformed", true); // Mark as transformed

                    cd = convertUriMethod.apply(updateCursor(cd), cd.getBody().getCoordinates().lastStatement());
                }
                return cd;
            }

            @Override
            public J visitNewClass(J.NewClass elem, ExecutionContext ctx) {
                J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (enclosingMethod != null && "transformNonLiteralURIToValidURL".equals(enclosingMethod.getSimpleName())) {
                    return super.visitNewClass(elem, ctx);
                }
                if (methodMatcherSingleArg.matches(elem)) {

                    if (elem.getArguments().get(0) instanceof J.Literal) {

                        JavaTemplate template = JavaTemplate.builder("URI.create(#{any(String)}).toURL()")
                                .imports("java.net.URI")
                                .contextSensitive()
                                .javaParser(JavaParser.fromJavaVersion())
                                .build();

                        J result = template.apply(getCursor(),
                                elem.getCoordinates().replace(),
                                elem.getArguments().get(0));
                        maybeAddImport("java.net.URI");
                        return result;

                    } else {
                        methodAdded = true;

                        JavaTemplate template = JavaTemplate.builder("transformNonLiteralURIToValidURL(#{any(String)})")
                                .imports("java.net.URI")
                                .contextSensitive()
                                .javaParser(JavaParser.fromJavaVersion())
                                .build();
                        J result = template.apply(getCursor(), elem.getCoordinates().replace(), elem.getArguments().get(0));
                        maybeAddImport("java.net.URI");
                        return result;
                    }
                } else if (methodMatcherThreeArg.matches(elem)) {

                    JavaTemplate template = JavaTemplate.builder("new URI(#{any(String)}, null, #{any(String)}, -1, #{any(String)}, null, null).toURL()")
                            .imports("java.net.URI", "java.net.URL")
                            .contextSensitive()
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();

                    J result = template.apply(getCursor(), elem.getCoordinates().replace(),
                            elem.getArguments().get(0),
                            elem.getArguments().get(1),
                            elem.getArguments().get(2));
                    maybeAddImport("java.net.URI");
                    return result;

                } else if (methodMatcherFourArg.matches(elem)) {

                    JavaTemplate template = JavaTemplate.builder("new URI(#{any(String)}, null, #{any(String)}, #{any(int)}, #{any(String)}, null, null).toURL()")
                            .imports("java.net.URI", "java.net.URL")
                            .contextSensitive()
                            .javaParser(JavaParser.fromJavaVersion())
                            .build();

                    J result = template.apply(getCursor(), elem.getCoordinates().replace(),
                            elem.getArguments().get(0),
                            elem.getArguments().get(1),
                            elem.getArguments().get(2),
                            elem.getArguments().get(3));
                    maybeAddImport("java.net.URI");
                    return result;
                }
                return super.visitNewClass(elem, ctx);
            }
        };
    }
}
