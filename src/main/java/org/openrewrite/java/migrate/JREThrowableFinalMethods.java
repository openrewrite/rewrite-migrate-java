/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@EqualsAndHashCode(callSuper = false)
class JREThrowableFinalMethods extends Recipe {

    private final String methodPatternAddSuppressed;
    private final String methodPatternGetSuppressed;

    @JsonCreator
    public JREThrowableFinalMethods() {
        this.methodPatternAddSuppressed = "*..* addSuppressed(Throwable)";
        this.methodPatternGetSuppressed = "*..* getSuppressed()";
    }

    /**
     * Overload constructor to allow for custom method patterns used in tests only.
     */
    JREThrowableFinalMethods(String methodPatternAddSuppressed, String methodPatternGetSuppressed) {
        this.methodPatternAddSuppressed = methodPatternAddSuppressed;
        this.methodPatternGetSuppressed = methodPatternGetSuppressed;
    }

    @Override
    public String getDisplayName() {
        return "Rename final method declarations `getSuppressed()` and `addSuppressed(Throwable exception)` in classes that extend `Throwable`";
    }

    @Override
    public String getDescription() {
        return "The recipe renames  `getSuppressed()` and `addSuppressed(Throwable exception)` methods  in classes "
               + "that extend `java.lang.Throwable` to `myGetSuppressed` and `myAddSuppressed(Throwable)`."
               + "These methods were added to Throwable in Java 7 and are marked final which cannot be overridden.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesType<>("java.lang.Throwable", true)),
                new JavaIsoVisitor<ExecutionContext>() {
                    private final MethodMatcher METHOD_ADDSUPPRESSED = new MethodMatcher(methodPatternAddSuppressed, false);
                    private final MethodMatcher METHOD_GETSUPPRESSED = new MethodMatcher(methodPatternGetSuppressed, false);
                    private final String JAVA_THROWABLE_CLASS = "java.lang.Throwable";

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration methodDecl, ExecutionContext ctx) {
                        J.MethodDeclaration md = super.visitMethodDeclaration(methodDecl, ctx);
                        JavaType.Method mt = md.getMethodType();
                        if (mt != null && TypeUtils.isAssignableTo(JAVA_THROWABLE_CLASS, mt.getDeclaringType())) {
                            J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                            if (classDeclaration != null) {
                                if (METHOD_ADDSUPPRESSED.matches(md, classDeclaration)) {
                                    JavaType.Method myAddSuppressed = mt.withName("myAddSuppressed");
                                    return md.withName(md.getName().withSimpleName("myAddSuppressed").withType(myAddSuppressed))
                                            .withMethodType(myAddSuppressed);
                                } else if (METHOD_GETSUPPRESSED.matches(md, classDeclaration)) {
                                    JavaType.Method myGetSuppressed = mt.withName("myGetSuppressed");
                                    return md.withName(md.getName().withSimpleName("myGetSuppressed").withType(myGetSuppressed))
                                            .withMethodType(myGetSuppressed);
                                }
                            }
                        }
                        return md;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInv, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(methodInv, ctx);
                        JavaType.Method mt = mi.getMethodType();
                        if (mt != null && TypeUtils.isAssignableTo(JAVA_THROWABLE_CLASS, mt.getDeclaringType())) {
                            if (METHOD_ADDSUPPRESSED.matches(mi)) {
                                JavaType.Method myAddSuppressed = mt.withName("myAddSuppressed");
                                mi = mi.withName(mi.getName().withSimpleName("myAddSuppressed").withType(myAddSuppressed))
                                        .withMethodType(myAddSuppressed);
                            } else if (METHOD_GETSUPPRESSED.matches(mi)) {
                                JavaType.Method myGetSuppressed = mt.withName("myGetSuppressed");
                                mi = mi.withName(mi.getName().withSimpleName("myGetSuppressed").withType(myGetSuppressed))
                                        .withMethodType(myGetSuppressed);
                            }
                        }
                        return mi;
                    }
                }
        );
    }
}
