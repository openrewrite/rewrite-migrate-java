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
package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;

/**
 * Migrates StringReader to Reader.of(CharSequence) for Java 25+.
 * This recipe only transforms:
 * 1. Assignments to variables of type Reader (not StringReader)
 * 2. Return statements from methods that return Reader (not StringReader)
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateStringReaderToReaderOf extends Recipe {
    private static final MethodMatcher STRING_READER_CONSTRUCTOR = new MethodMatcher("java.io.StringReader <constructor>(java.lang.String)");

    @Override
    public String getDisplayName() {
        return "Use `Reader.of(CharSequence)` for non-synchronized readers";
    }

    @Override
    public String getDescription() {
        return "Migrate `new StringReader(String)` to `Reader.of(CharSequence)` in Java 25+. " +
               "This only applies when assigning to `Reader` variables or returning from methods that return `Reader`. " +
               "The new method creates non-synchronized readers which are more efficient when thread-safety is not required.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(25),
                        new UsesType<>("java.io.StringReader", false)
                ),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                        // Only process if the variable type is Reader (not StringReader)
                        if (isReaderType(multiVariable.getTypeAsFullyQualified())) {
                            // Check each variable in the declaration
                            return multiVariable.withVariables(ListUtils.map(multiVariable.getVariables(), v -> {
                                Expression initializer = v.getInitializer();
                                if (initializer instanceof J.NewClass) {
                                    J.NewClass nc = (J.NewClass) initializer;
                                    if (STRING_READER_CONSTRUCTOR.matches(nc)) {
                                        maybeRemoveImport("java.io.StringReader");
                                        return (J.VariableDeclarations.NamedVariable) new TransformVisitor().visit(v, executionContext, getCursor().getParent());
                                    }
                                }
                                return v;
                            }));
                        }
                        return super.visitVariableDeclarations(multiVariable, executionContext);
                    }

                    @Override
                    public J visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                        // Check if assigning new StringReader to a Reader variable
                        if (assignment.getAssignment() instanceof J.NewClass) {
                            J.NewClass nc = (J.NewClass) assignment.getAssignment();
                            if (STRING_READER_CONSTRUCTOR.matches(nc)) {
                                // Check if the variable being assigned to is of type Reader
                                if (assignment.getVariable() instanceof J.Identifier) {
                                    J.Identifier variable = (J.Identifier) assignment.getVariable();
                                    if (isReaderType(variable.getType())) {
                                        maybeRemoveImport("java.io.StringReader");
                                        return new TransformVisitor().visit(assignment, executionContext, getCursor().getParent());
                                    }
                                }
                            }
                        }
                        return super.visitAssignment(assignment, executionContext);
                    }

                    @Override
                    public J visitReturn(J.Return return_, ExecutionContext executionContext) {
                        // Check if returning new StringReader from a method that returns Reader
                        if (return_.getExpression() instanceof J.NewClass) {
                            J.NewClass nc = (J.NewClass) return_.getExpression();
                            if (STRING_READER_CONSTRUCTOR.matches(nc)) {
                                // Check if the method returns Reader (not StringReader)
                                J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                                if (method != null && method.getReturnTypeExpression() != null) {
                                    JavaType returnType = method.getReturnTypeExpression().getType();
                                    if (isReaderType(returnType)) {
                                        maybeRemoveImport("java.io.StringReader");
                                        return new TransformVisitor().visit(return_, executionContext, getCursor().getParent());
                                    }
                                }
                            }
                        }
                        return super.visitReturn(return_, executionContext);
                    }

                    private boolean isReaderType(JavaType type) {
                        if (type instanceof JavaType.FullyQualified) {
                            return "java.io.Reader".equals(((JavaType.FullyQualified) type).getFullyQualifiedName());
                        }
                        return false;
                    }

                    private class TransformVisitor extends JavaVisitor<ExecutionContext> {
                        @Override
                        public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                            if (STRING_READER_CONSTRUCTOR.matches(newClass)) {
                                Expression argument = newClass.getArguments().get(0);

                                // Optimize CharSequence.toString() calls
                                argument = optimizeCharSequenceToString(argument);

                                JavaTemplate template = JavaTemplate.builder("Reader.of(#{any(java.lang.CharSequence)})")
                                        .imports("java.io.Reader")
                                        .contextSensitive()
                                        .build();

                                maybeAddImport("java.io.Reader");
                                return template.apply(getCursor(), newClass.getCoordinates().replace(), argument);
                            }
                            return super.visitNewClass(newClass, executionContext);
                        }

                        private Expression optimizeCharSequenceToString(Expression expr) {
                            if (expr instanceof J.MethodInvocation) {
                                J.MethodInvocation mi = (J.MethodInvocation) expr;
                                if ("toString".equals(mi.getSimpleName()) &&
                                        (mi.getArguments().isEmpty() || (mi.getArguments().size() == 1 && mi.getArguments().get(0) instanceof J.Empty)) &&
                                    mi.getSelect() != null &&
                                    isCharSequenceType(mi.getSelect().getType())) {
                                    return mi.getSelect();
                                }
                            }
                            return expr;
                        }

                        private boolean isCharSequenceType(JavaType type) {
                            if (type instanceof JavaType.Class) {
                                JavaType.Class classType = (JavaType.Class) type;
                                String fqn = classType.getFullyQualifiedName();
                                return "java.lang.CharSequence".equals(fqn) ||
                                       "java.lang.String".equals(fqn) ||
                                       "java.lang.StringBuilder".equals(fqn) ||
                                       "java.lang.StringBuffer".equals(fqn) ||
                                       "java.nio.CharBuffer".equals(fqn) ||
                                       implementsCharSequence(classType);
                            }
                            return false;
                        }

                        private boolean implementsCharSequence(JavaType.Class classType) {
                            for (JavaType.FullyQualified iface : classType.getInterfaces()) {
                                if ("java.lang.CharSequence".equals(iface.getFullyQualifiedName())) {
                                    return true;
                                }
                            }
                            JavaType.FullyQualified supertype = classType.getSupertype();
                            if (supertype instanceof JavaType.Class) {
                                return implementsCharSequence((JavaType.Class) supertype);
                            }
                            return false;
                        }
                    }
                }
        );
    }
}
