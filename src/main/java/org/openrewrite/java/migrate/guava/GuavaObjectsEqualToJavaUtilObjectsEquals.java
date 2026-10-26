/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.FullyQualifyTypeReference;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.NoStaticImport;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.concurrent.atomic.AtomicBoolean;

public class GuavaObjectsEqualToJavaUtilObjectsEquals extends Recipe {
    private static final String GUAVA_EQUAL = "com.google.common.base.Objects equal(Object, Object)";
    private static final MethodMatcher GUAVA_EQUAL_MATCHER = new MethodMatcher(GUAVA_EQUAL);
    private static final String JAVA_OBJECTS = "java.util.Objects";

    @Getter
    final String displayName = "Migrate Guava equality without static import collisions";

    @Getter
    final String description = "Use the existing Guava equality migration helpers and qualify Java Objects type " +
            "references in compilation units containing an originally unqualified Guava equality call.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit compilationUnit, ExecutionContext ctx) {
                // Evaluate this on the original attributed tree, before either method migration changes its type/name.
                boolean hadUnqualifiedGuavaCall = new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                        if (method.getSelect() == null && GUAVA_EQUAL_MATCHER.matches(method)) {
                            found.set(true);
                        }
                        return found.get() ? method : super.visitMethodInvocation(method, found);
                    }
                }.reduce(compilationUnit, new AtomicBoolean()).get();
                boolean hadJavaObjectsImport = compilationUnit.getImports().stream().anyMatch(anImport ->
                        !anImport.isStatic() && JAVA_OBJECTS.equals(anImport.getTypeName()));

                J.CompilationUnit cu = apply(new NoStaticImport(GUAVA_EQUAL).getVisitor(), compilationUnit, ctx);
                cu = apply(new ChangeMethodName(GUAVA_EQUAL, "equals", null, null).getVisitor(), cu, ctx);
                cu = apply(new ChangeMethodTargetToStatic(
                        "com.google.common.base.Objects equals(Object, Object)", JAVA_OBJECTS, null, null).getVisitor(), cu, ctx);
                if (hadUnqualifiedGuavaCall) {
                    // Type attribution distinguishes a type name from an application Objects type or variable.
                    cu = apply(new FullyQualifyTypeReference<ExecutionContext>(JavaType.ShallowClass.build(JAVA_OBJECTS)), cu, ctx);
                    if (!hadJavaObjectsImport) {
                        // Remove only the new ordinary import; preserve pre-existing imports, including Javadoc uses.
                        cu = cu.withImports(ListUtils.map(cu.getImports(), anImport ->
                                !anImport.isStatic() && JAVA_OBJECTS.equals(anImport.getTypeName()) ? null : anImport));
                    }
                }
                return cu;
            }

            private J.CompilationUnit apply(TreeVisitor<?, ExecutionContext> visitor, J.CompilationUnit cu, ExecutionContext ctx) {
                return (J.CompilationUnit) visitor.visitNonNull(cu, ctx, getCursor().getParentOrThrow());
            }
        };
    }
}
