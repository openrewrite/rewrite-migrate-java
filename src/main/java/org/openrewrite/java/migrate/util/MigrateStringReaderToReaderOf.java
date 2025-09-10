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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

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
                        if (TypeUtils.isOfClassType(multiVariable.getTypeAsFullyQualified(), "java.io.Reader")) {
                            return multiVariable.withVariables(ListUtils.map(multiVariable.getVariables(), v -> {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return (J.VariableDeclarations.NamedVariable) new TransformVisitor().visit(v, executionContext, getCursor().getParent());
                            }));
                        }
                        return super.visitVariableDeclarations(multiVariable, executionContext);
                    }

                    @Override
                    public J visitAssignment(J.Assignment assignment, ExecutionContext executionContext) {
                        if (assignment.getVariable() instanceof J.Identifier) {
                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                            if (TypeUtils.isOfClassType(variable.getType(), "java.io.Reader")) {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return new TransformVisitor().visit(assignment, executionContext, getCursor().getParent());
                            }
                        }
                        return super.visitAssignment(assignment, executionContext);
                    }

                    @Override
                    public J visitReturn(J.Return return_, ExecutionContext executionContext) {
                        J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        if (method != null && method.getReturnTypeExpression() != null) {
                            JavaType returnType = method.getReturnTypeExpression().getType();
                            if (TypeUtils.isOfClassType(returnType, "java.io.Reader")) {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return new TransformVisitor().visit(return_, executionContext, getCursor().getParent());
                            }
                        }
                        return super.visitReturn(return_, executionContext);
                    }

                    private class TransformVisitor extends JavaVisitor<ExecutionContext> {
                        @Override
                        public J visitNewClass(J.NewClass newClass, ExecutionContext executionContext) {
                            if (STRING_READER_CONSTRUCTOR.matches(newClass)) {
                                Expression argument = newClass.getArguments().get(0);
                                argument = optimizeCharSequenceToString(argument);
                                JavaTemplate template = JavaTemplate.builder("Reader.of(#{any(java.lang.CharSequence)})")
                                        .imports("java.io.Reader")
                                        .contextSensitive()
                                        .build();

                                return template.apply(getCursor(), newClass.getCoordinates().replace(), argument);
                            }
                            return super.visitNewClass(newClass, executionContext);
                        }

                        private Expression optimizeCharSequenceToString(Expression expr) {
                            if (expr instanceof J.MethodInvocation) {
                                J.MethodInvocation mi = (J.MethodInvocation) expr;
                                if ("toString".equals(mi.getSimpleName()) &&
                                        (mi.getArguments().isEmpty() || (mi.getArguments().size() == 1 && mi.getArguments().get(0) instanceof J.Empty)) &&
                                        mi.getSelect() != null && TypeUtils.isAssignableTo("java.lang.CharSequence", mi.getSelect().getType())) {
                                    return mi.getSelect();
                                }
                            }
                            return expr;
                        }
                    }
                }
        );
    }
}
