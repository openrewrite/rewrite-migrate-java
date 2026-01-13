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
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateStringReaderToReaderOf extends Recipe {
    private static final MethodMatcher STRING_READER_CONSTRUCTOR = new MethodMatcher("java.io.StringReader <constructor>(java.lang.String)");
    private static final MethodMatcher TO_STRING_METHOD = new MethodMatcher("java.lang.Object toString()", true);

    String displayName = "Use `Reader.of(CharSequence)` for non-synchronized readers";

    String description = "Migrate `new StringReader(String)` to `Reader.of(CharSequence)` in Java 25+. " +
                "This only applies when assigning to `Reader` variables or returning from methods that return `Reader`. " +
                "The new method creates non-synchronized readers which are more efficient when thread-safety is not required.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesJavaVersion<>(25), new UsesMethod<>(STRING_READER_CONSTRUCTOR)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations mV, ExecutionContext ctx) {
                        if (TypeUtils.isOfClassType(mV.getTypeAsFullyQualified(), "java.io.Reader")) {
                            return mV.withVariables(ListUtils.map(mV.getVariables(), v -> {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return (J.VariableDeclarations.NamedVariable) new TransformVisitor().visitNonNull(v, ctx, getCursor().getParentOrThrow());
                            }));
                        }
                        return super.visitVariableDeclarations(mV, ctx);
                    }

                    @Override
                    public J visitAssignment(J.Assignment a, ExecutionContext ctx) {
                        if (a.getVariable() instanceof J.Identifier) {
                            J.Identifier variable = (J.Identifier) a.getVariable();
                            if (TypeUtils.isOfClassType(variable.getType(), "java.io.Reader")) {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return new TransformVisitor().visitNonNull(a, ctx, getCursor().getParentOrThrow());
                            }
                        }
                        return super.visitAssignment(a, ctx);
                    }

                    @Override
                    public J visitReturn(J.Return r, ExecutionContext ctx) {
                        J.MethodDeclaration method = getCursor().firstEnclosing(J.MethodDeclaration.class);
                        if (method != null && method.getReturnTypeExpression() != null) {
                            JavaType returnType = method.getReturnTypeExpression().getType();
                            if (TypeUtils.isOfClassType(returnType, "java.io.Reader")) {
                                maybeRemoveImport("java.io.StringReader");
                                maybeAddImport("java.io.Reader");
                                return new TransformVisitor().visitNonNull(r, ctx, getCursor().getParentOrThrow());
                            }
                        }
                        return super.visitReturn(r, ctx);
                    }
                }
        );
    }

    private static class TransformVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            if (STRING_READER_CONSTRUCTOR.matches(newClass)) {
                return JavaTemplate.builder("Reader.of(#{any(java.lang.CharSequence)})")
                        .imports("java.io.Reader")
                        .build()
                        .apply(getCursor(), newClass.getCoordinates().replace(), optimizeCharSequenceToString(newClass.getArguments().get(0)));
            }
            return super.visitNewClass(newClass, ctx);
        }

        private Expression optimizeCharSequenceToString(Expression expr) {
            if (expr instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) expr;
                if (TO_STRING_METHOD.matches(mi) &&
                        mi.getSelect() != null && TypeUtils.isAssignableTo("java.lang.CharSequence", mi.getSelect().getType())) {
                    return mi.getSelect();
                }
            }
            return expr;
        }
    }
}
