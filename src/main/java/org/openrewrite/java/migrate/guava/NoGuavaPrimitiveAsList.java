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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Collections.nCopies;

public class NoGuavaPrimitiveAsList extends Recipe {

    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("com.google.common.primitives.* asList(..)");

    @Getter
    final String displayName = "Prefer `Arrays.asList(..)` over Guava primitives";

    @Getter
    final String description = "Migrate from Guava `com.google.common.primitives.* asList(..)` to `Arrays.asList(..)` " +
            "where each argument is an element of the primitive type or its box, so the list keeps its element type.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(METHOD_MATCHER),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                        if (METHOD_MATCHER.matches(elem) && argumentsAreElements(elem)) {
                            maybeRemoveImport("com.google.common.primitives.Booleans");
                            maybeRemoveImport("com.google.common.primitives.Chars");
                            maybeRemoveImport("com.google.common.primitives.Doubles");
                            maybeRemoveImport("com.google.common.primitives.Floats");
                            maybeRemoveImport("com.google.common.primitives.Longs");
                            maybeRemoveImport("com.google.common.primitives.Ints");
                            maybeRemoveImport("com.google.common.primitives.Shorts");
                            maybeRemoveImport("com.google.common.primitives.Bytes");
                            maybeAddImport("java.util.Arrays");

                            String args = String.join(",", nCopies(elem.getArguments().size(), "#{any()}"));
                            return JavaTemplate
                                    .builder("Arrays.asList(" + args + ')')
                                    .imports("java.util.Arrays")
                                    .build()
                                    .apply(getCursor(), elem.getCoordinates().replace(), elem.getArguments().toArray());
                        }
                        return super.visitMethodInvocation(elem, ctx);
                    }
                }
        );
    }

    /**
     * {@code Arrays.asList} takes each argument as an element of the argument's own type,
     * so an array or a widened argument would change the element type of the list.
     */
    private static boolean argumentsAreElements(J.MethodInvocation asList) {
        JavaType.Method method = asList.getMethodType();
        if (method == null || method.getParameterTypes().size() != 1 ||
            !(method.getParameterTypes().get(0) instanceof JavaType.Array)) {
            return false;
        }
        JavaType elementType = ((JavaType.Array) method.getParameterTypes().get(0)).getElemType();
        if (!(elementType instanceof JavaType.Primitive)) {
            return false;
        }
        String boxedType = ((JavaType.Primitive) elementType).getClassName();
        for (Expression argument : asList.getArguments()) {
            if (!(argument instanceof J.Empty) && argument.getType() != elementType &&
                !TypeUtils.isOfClassType(argument.getType(), boxedType)) {
                return false;
            }
        }
        return true;
    }
}
