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
package org.openrewrite.java.migrate.io;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class ReplaceSystemOutWithIOPrint extends Recipe {

    @Getter
    final String displayName = "Migrate `System.out.print` to Java 25 IO utility class";

    @Getter
    final String description = "Replace `System.out.print()`, `System.out.println()` with `IO.print()` and `IO.println()`. " +
            "Migrates to the new IO utility class introduced in Java 25.";

    private static final MethodMatcher SYSTEM_OUT_PRINT = new MethodMatcher("java.io.PrintStream print(..)");
    private static final MethodMatcher SYSTEM_OUT_PRINTLN = new MethodMatcher("java.io.PrintStream println(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(25),
                        Preconditions.or(
                                new UsesMethod<>(SYSTEM_OUT_PRINT),
                                new UsesMethod<>(SYSTEM_OUT_PRINTLN)
                        )
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        // Moving a generic expression to IO's Object overload can change its
                        // inferred type and remove conversions performed by PrintStream's overload.
                        if (!isSystemOutMethod(m) || hasGenericArgument(m.getArguments().get(0))) {
                            return m;
                        }
                        String methodName = m.getName().getSimpleName();
                        return m.getArguments().get(0) instanceof J.Empty ?
                                JavaTemplate.apply( "IO.#{}()", getCursor(), m.getCoordinates().replace(), methodName ) :
                                JavaTemplate.apply( "IO.#{}(#{any()})", getCursor(), m.getCoordinates().replace(), methodName, m.getArguments().get( 0 ) );
                    }

                    private boolean hasGenericArgument(Expression argument) {
                        if (!(argument instanceof J.Empty) &&
                            (argument.getType() == null || argument.getType() instanceof JavaType.Unknown)) {
                            return true;
                        }
                        boolean[] generic = {false};
                        new JavaIsoVisitor<boolean[]>() {
                            @Override
                            public J preVisit(J tree, boolean[] found) {
                                if (tree instanceof J.TypeCast &&
                                    (TypeUtils.isString(((J.TypeCast) tree).getType()) ||
                                     TypeUtils.isOfClassType(((J.TypeCast) tree).getType(), "java.lang.Object"))) {
                                    // An explicit cast supplies its own context independently of print.
                                    stopAfterPreVisit();
                                    return tree;
                                }
                                if (tree instanceof Expression &&
                                    (((Expression) tree).getType() instanceof JavaType.GenericTypeVariable ||
                                     ((Expression) tree).getType() instanceof JavaType.Intersection)) {
                                    found[0] = true;
                                }
                                if (tree instanceof J.MethodInvocation) {
                                    JavaType.Method method = ((J.MethodInvocation) tree).getMethodType();
                                    // Applied method types can erase the original type parameters and
                                    // already contain String inferred from PrintStream's overload.
                                    JavaType.Method declaration = method == null ? null :
                                            TypeUtils.findDeclaredMethod(method.getDeclaringType(), method.getName(),
                                                    method.getParameterTypes()).orElse(null);
                                    if (declaration == null || declaration.getReturnType() == null ||
                                        declaration.getReturnType() instanceof JavaType.Unknown ||
                                        declaration.getReturnType() instanceof JavaType.GenericTypeVariable ||
                                        declaration.getReturnType() instanceof JavaType.Intersection ||
                                        !declaration.getDeclaredFormalTypeNames().isEmpty()) {
                                        found[0] = true;
                                    }
                                }
                                if (found[0]) {
                                    stopAfterPreVisit();
                                }
                                return tree;
                            }
                        }.visit(argument, generic);
                        return generic[0];
                    }

                    private boolean isSystemOutMethod(J.MethodInvocation mi) {
                        if (SYSTEM_OUT_PRINT.matches(mi) || SYSTEM_OUT_PRINTLN.matches(mi)) {
                            Expression expression = mi.getSelect();
                            if (expression instanceof J.FieldAccess) {
                                return isSystemOut(((J.FieldAccess) expression).getName());
                            }
                            if (expression instanceof J.Identifier) {
                                maybeRemoveImport("java.lang.System.out");
                                return isSystemOut((J.Identifier) expression);
                            }
                        }
                        return false;
                    }

                    private boolean isSystemOut(J.Identifier identifier) {
                        return "out".equals(identifier.getSimpleName()) &&
                                identifier.getFieldType() != null &&
                                TypeUtils.isAssignableTo("java.lang.System", identifier.getFieldType().getOwner());
                    }
                });
    }
}
