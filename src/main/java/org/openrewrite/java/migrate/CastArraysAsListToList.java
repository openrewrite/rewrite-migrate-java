/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class CastArraysAsListToList extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove explicit casts on `Arrays.asList(..).toArray()`";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Convert code like `(Integer[]) Arrays.asList(1, 2, 3).toArray()` to `Arrays.asList(1, 2, 3).toArray(new Integer[0])`.";
    }

    private static final MethodMatcher ARRAYS_AS_LIST = new MethodMatcher("java.util.Arrays asList(..)", false);
    private static final MethodMatcher LIST_TO_ARRAY = new MethodMatcher("java.util.List toArray()", true);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesMethod<>(ARRAYS_AS_LIST), new UsesMethod<>(LIST_TO_ARRAY)),
                new CastArraysAsListToListVisitor());
    }

    private static class CastArraysAsListToListVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitTypeCast(J.TypeCast typeCast, ExecutionContext executionContext) {
            J j = super.visitTypeCast(typeCast, executionContext);
            if (!(j instanceof J.TypeCast)) {
                return j;
            }
            typeCast = (J.TypeCast) j;

            boolean matches = typeCast.getClazz().getTree() instanceof J.ArrayType
                              && (typeCast.getType() instanceof JavaType.Class || typeCast.getType() instanceof JavaType.Parameterized)
                              && ((JavaType.FullyQualified) typeCast.getType()).getOwningClass() == null // does not support inner class now
                              && LIST_TO_ARRAY.matches(typeCast.getExpression())
                              && typeCast.getExpression() instanceof J.MethodInvocation
                              && ARRAYS_AS_LIST.matches(((J.MethodInvocation) typeCast.getExpression()).getSelect());
            if (!matches) {
                return typeCast;
            }

            String fullyQualifiedName = ((JavaType.FullyQualified) typeCast.getType()).getFullyQualifiedName();
            int dimensionSize = ((J.ArrayType) typeCast.getClazz().getTree()).getDimensions().size();

            if (fullyQualifiedName.equals("java.lang.Object") && dimensionSize == 1) {
                // we don't need to fix this case because toArray() does return Object[] type
                return typeCast;
            }

            // we don't add generic type name here because generic array creation is not allowed
            StringBuilder newArrayString = new StringBuilder();
            String className = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1);
            newArrayString.append(className);
            newArrayString.append("[0]");
            for (int i = 0; i < dimensionSize - 1; i++) {
                newArrayString.append("[]");
            }

            JavaTemplate t = JavaTemplate
                    .builder("#{any(java.util.List)}.toArray(new " + newArrayString + ")")
                    .imports(fullyQualifiedName)
                    .build();
            return t.apply(updateCursor(typeCast), typeCast.getCoordinates().replace(), ((J.MethodInvocation) typeCast.getExpression()).getSelect());
        }
    }
}
