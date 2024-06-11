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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;


public class JREThrowableFinalMethods extends Recipe {
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
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher METHOD_GETSUPPRESSED = new MethodMatcher("*..*  get1Suppressed()", false);
            private final MethodMatcher METHOD_ADDSUPPRESSED = new MethodMatcher("*..*  add1Suppressed(Throwable)", false);
            private final String JAVA_THROWABLE_CLASS = "java.lang.Throwable";

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getMethodType() != null && method.getReturnTypeExpression() != null) {
                    String sn = method.getSimpleName();
                    JavaType rte = method.getReturnTypeExpression().getType();
                    JavaType.Method t = method.getMethodType();
                    JavaType returnElementType = null;
                    if (rte instanceof JavaType.Array) {
                        returnElementType = ((JavaType.Array) rte).getElemType();
                    }
                    String superClass = method.getMethodType().getDeclaringType().getSupertype().getFullyQualifiedName();
                    if ("add1Suppressed".equals(sn) && JavaType.Primitive.Void.equals(rte) && JAVA_THROWABLE_CLASS.equals(superClass)) {
                        method = method.withName(method.getName().withSimpleName("myAddSuppressed")).withMethodType(t.withName("myAddSuppressed"));
                    }

                    if ("get1Suppressed".equals(sn) && JAVA_THROWABLE_CLASS.equals(returnElementType.toString()) && JAVA_THROWABLE_CLASS.equals(superClass)) {
                        method = method.withName(method.getName().withSimpleName("myGetSuppressed")).withMethodType(t.withName("myGetSuppressed"));
                    }
                }
                return (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (method.getMethodType() != null) {
                    String superClass = method.getMethodType().getDeclaringType().getSupertype().getFullyQualifiedName();
                    if (METHOD_ADDSUPPRESSED.matches(method) && "java.lang.Throwable".equals(superClass)) {
                        method = method.withName(method.getName().withSimpleName("myAddSuppressed"));
                    } else if (METHOD_GETSUPPRESSED.matches(method) && "java.lang.Throwable".equals(superClass)) {
                        method = method.withName(method.getName().withSimpleName("myGetSuppressed"));
                    }
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
