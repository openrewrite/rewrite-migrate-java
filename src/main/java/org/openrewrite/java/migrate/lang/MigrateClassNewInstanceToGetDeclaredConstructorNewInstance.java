/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class MigrateClassNewInstanceToGetDeclaredConstructorNewInstance extends Recipe {
    private static final MethodMatcher NEW_INSTANCE_MATCHER = new MethodMatcher("java.lang.Class newInstance()");

    @Override
    public String getDisplayName() {
        return "Use `Class#getDeclaredConstructor().newInstance()`";
    }

    @Override
    public String getDescription() {
        return "`Class#newInstance()` was deprecated in Java 9.";
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("java.lang.Class newInstance()");
    }

    @Override
    protected NewInstanceToDeclaredConstructorVisitor getVisitor() {
        return new NewInstanceToDeclaredConstructorVisitor();
    }

    private static class NewInstanceToDeclaredConstructorVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaType exType = JavaType.buildType("java.lang.Exception");
        private final JavaType thType = JavaType.buildType("java.lang.Throwable");
        private static final ChangeMethodName TO_DECLARED_CONS_NEW_INSTANCE = new ChangeMethodName("java.lang.Class newInstance()", "getDeclaredConstructor().newInstance", null);

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if (NEW_INSTANCE_MATCHER.matches(mi)) {
                J.Try tri = getCursor().firstEnclosing(J.Try.class);
                J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if ((tri != null && tri.getCatches().stream().anyMatch(c -> isExceptionType(c.getParameter().getType())))
                        || (md != null && md.getThrows() != null && md.getThrows().stream().anyMatch(nt -> isExceptionType(nt.getType())))) {
                    J.MethodInvocation modifiedMethodInvocation = (J.MethodInvocation) TO_DECLARED_CONS_NEW_INSTANCE.getVisitor().visit(mi, executionContext);
                    if (modifiedMethodInvocation != null) {
                        mi = modifiedMethodInvocation;
                    }
                }
            }
            return mi;
        }

        private boolean isExceptionType(@Nullable JavaType type) {
            return TypeUtils.isOfType(type, exType)
                    || TypeUtils.isOfType(type, thType);
        }
    }
}
