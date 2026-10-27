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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveImport;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;

import java.util.concurrent.atomic.AtomicBoolean;

public class GuavaObjectsEqualToJavaUtilObjectsEquals extends Recipe {
    private static final String GUAVA_EQUAL = "com.google.common.base.Objects equal(Object, Object)";
    private static final MethodMatcher GUAVA_EQUAL_MATCHER = new MethodMatcher(GUAVA_EQUAL);

    @Getter
    final String displayName = "Migrate Guava equality without static import collisions";

    @Getter
    final String description = "Qualify originally unqualified Guava equality calls before applying the existing " +
            "method migration. Retain Java compilation units whose Java package prefix can be shadowed.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof JavaSourceFile)) {
                    return tree;
                }
                Tree source = tree;
                if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    boolean hadUnqualifiedGuavaCall = new JavaIsoVisitor<AtomicBoolean>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                            if (method.getSelect() == null && GUAVA_EQUAL_MATCHER.matches(method)) {
                                found.set(true);
                            }
                            return found.get() ? method : super.visitMethodInvocation(method, found);
                        }
                    }.reduce(cu, new AtomicBoolean()).get();
                    if (hadUnqualifiedGuavaCall) {
                        if (VariableNameUtils.findNamesInScope(getCursor()).contains("java") || hasJavaTypeName(cu)) {
                            return cu;
                        }
                        source = apply(new JavaIsoVisitor<ExecutionContext>() {
                            private final JavaTemplate equals = JavaTemplate.builder(
                                    "java.util.Objects.equals(#{any(java.lang.Object)}, #{any(java.lang.Object)})").build();
                            private boolean cleanupScheduled;

                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                                if (m.getSelect() == null && GUAVA_EQUAL_MATCHER.matches(m)) {
                                    if (!cleanupScheduled) {
                                        doAfterVisit(new RemoveImport<ExecutionContext>("com.google.common.base.Objects.equal", true));
                                        cleanupScheduled = true;
                                    }
                                    return equals.apply(updateCursor(m), m.getCoordinates().replace(),
                                            m.getArguments().get(0), m.getArguments().get(1));
                                }
                                return m;
                            }
                        }, cu, ctx);
                    }
                }
                // Keep the original migration for remaining Java calls/references and other JavaSourceFile kinds.
                source = apply(new ChangeMethodName(GUAVA_EQUAL, "equals", null, null).getVisitor(), source, ctx);
                return apply(new ChangeMethodTargetToStatic(
                        "com.google.common.base.Objects equals(Object, Object)", "java.util.Objects", null, null).getVisitor(), source, ctx);
            }

            private boolean hasJavaTypeName(J.CompilationUnit cu) {
                return new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Import visitImport(J.Import anImport, AtomicBoolean shadowed) {
                        // Only the covered JDK java.util wildcard is exempt. Other ordinary wildcards
                        // may expose a java type that is absent from used-type attribution.
                        if ("java".equals(anImport.getQualid().getSimpleName()) ||
                            !anImport.isStatic() && "*".equals(anImport.getQualid().getSimpleName()) &&
                            !"java.util".equals(anImport.getPackageName())) {
                            shadowed.set(true);
                        } else if (anImport.isStatic() && "*".equals(anImport.getQualid().getSimpleName())) {
                            checkMembers(anImport.getQualid().getTarget().getType(), shadowed);
                        }
                        return anImport;
                    }

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration declaration, AtomicBoolean shadowed) {
                        if ("java".equals(declaration.getSimpleName())) {
                            shadowed.set(true);
                        }
                        checkMembers(declaration.getType(), shadowed);
                        return super.visitClassDeclaration(declaration, shadowed);
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean shadowed) {
                        if (newClass.getBody() != null) {
                            checkMembers(newClass.getType(), shadowed);
                        }
                        return super.visitNewClass(newClass, shadowed);
                    }

                    @Override
                    public J.TypeParameter visitTypeParameter(J.TypeParameter parameter, AtomicBoolean shadowed) {
                        if (parameter.getName() instanceof J.Identifier &&
                            "java".equals(((J.Identifier) parameter.getName()).getSimpleName())) {
                            shadowed.set(true);
                        }
                        return super.visitTypeParameter(parameter, shadowed);
                    }

                    private void checkMembers(JavaType type, AtomicBoolean shadowed) {
                        if (type instanceof JavaType.FullyQualified) {
                            ((JavaType.FullyQualified) type).getVisibleMembers().forEachRemaining(member -> {
                                if ("java".equals(member.getName())) {
                                    shadowed.set(true);
                                }
                            });
                        }
                    }
                }.reduce(cu, new AtomicBoolean()).get();
            }

            private Tree apply(TreeVisitor<?, ExecutionContext> visitor, Tree source, ExecutionContext ctx) {
                return visitor.visitNonNull(source, ctx, getCursor().getParentOrThrow());
            }
        };
    }
}
