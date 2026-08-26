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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public class URLConstructorToURICreate extends Recipe {

    private static final String URI_FQN = "java.net.URI";
    private static final String URL_FQN = "java.net.URL";
    private static final MethodMatcher methodMatcherSingleArg = new MethodMatcher(URL_FQN + "#<init>(java.lang.String)");

    @Getter
    final String displayName = "Convert `new URL(String)` to `URI.create(String).toURL()`";

    @Getter
    final String description = "Converts `new URL(String)` constructor to `URI.create(String).toURL()`. The URL constructor has been deprecated due to security vulnerabilities when handling malformed URLs. Using `URI.create(String)` provides stronger validation and safer URL handling in modern Java applications.";

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
                        for (Cursor c = getCursor(); c != null; c = c.getParent()) {
                            if (c.getValue() instanceof J.Block) {
                                for (Statement statement : ((J.Block) c.getValue()).getStatements()) {
                                    if (statement instanceof J.VariableDeclarations) {
                                        for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statement).getVariables()) {
                                            if (isSameVariable(identifier, variable)) {
                                                Expression initializer = variable.getInitializer();
                                                if (initializer instanceof J.Literal &&
                                                        ((J.Literal) initializer).getValue() instanceof String &&
                                                        !isReassigned(variable)) {
                                                    return (String) ((J.Literal) initializer).getValue();
                                                }
                                                return null;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    private boolean isSameVariable(Expression expression, J.VariableDeclarations.NamedVariable variable) {
                        if (expression instanceof J.FieldAccess) {
                            return isSameVariable(((J.FieldAccess) expression).getName(), variable);
                        }
                        if (!(expression instanceof J.Identifier)) {
                            return false;
                        }
                        JavaType.Variable fieldType = ((J.Identifier) expression).getFieldType();
                        if (fieldType != null && variable.getVariableType() != null) {
                            return TypeUtils.isOfType(fieldType, variable.getVariableType());
                        }
                        return variable.getSimpleName().equals(((J.Identifier) expression).getSimpleName());
                    }

                    private boolean isReassigned(J.VariableDeclarations.NamedVariable variable) {
                        JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                        if (sourceFile == null) {
                            return true;
                        }
                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean reassigned) {
                                if (isSameVariable(assignment.getVariable(), variable)) {
                                    reassigned.set(true);
                                }
                                return super.visitAssignment(assignment, reassigned);
                            }

                            @Override
                            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean reassigned) {
                                if (isSameVariable(assignOp.getVariable(), variable)) {
                                    reassigned.set(true);
                                }
                                return super.visitAssignmentOperation(assignOp, reassigned);
                            }
                        }.reduce(sourceFile, new AtomicBoolean()).get();
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
