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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;


@Value
@EqualsAndHashCode(callSuper = false)
public class ReferenceCloneMethod extends Recipe {
    private static final MethodMatcher REFERENCE_CLONE = new MethodMatcher("java.lang.ref.Reference clone()", true);

    @Override
    public String getDisplayName() {
        return "Replace `java.lang.ref.Reference.clone()` with constructor call";
    }

    @Override
    public String getDescription() {
        return "The recipe replaces any clone calls that may resolve to a `java.lang.ref.Reference.clone()` " +
               "or any of its known subclasses: `java.lang.ref.PhantomReference`, `java.lang.ref.SoftReference`, and `java.lang.ref.WeakReference` " +
               "with a constructor call passing in the referent and reference queue as parameters.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(REFERENCE_CLONE),
                new JavaVisitor<ExecutionContext>() {
                    private static final String REFERENCE_CLONE_REPLACED = "REFERENCE_CLONE_REPLACED";

                    @Override
                    public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
                        J j = super.visitTypeCast(typeCast, ctx);
                        if (Boolean.TRUE.equals(getCursor().pollNearestMessage(REFERENCE_CLONE_REPLACED)) &&
                            j instanceof J.TypeCast) {
                            J.TypeCast tc = (J.TypeCast) j;
                            if (TypeUtils.isOfType(tc.getType(), tc.getExpression().getType())) {
                                return tc.getExpression();
                            }
                        }
                        return j;
                    }

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        super.visitMethodInvocation(method, ctx);
                        if (REFERENCE_CLONE.matches(method) && method.getSelect() instanceof J.Identifier) {
                            J.Identifier methodRef = (J.Identifier) method.getSelect();
                            String template = "new " + methodRef.getType().toString() + "(" + methodRef.getSimpleName() + ", new ReferenceQueue<>())";
                            getCursor().putMessageOnFirstEnclosing(J.TypeCast.class, REFERENCE_CLONE_REPLACED, true);
                            J replacement = JavaTemplate.builder(template)
                                    .contextSensitive()
                                    .imports("java.lang.ref.ReferenceQueue")
                                    .build().apply(getCursor(), method.getCoordinates().replace());
                            doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(replacement));
                            return replacement;
                        }
                        return method;
                    }
                }
        );
    }
}
