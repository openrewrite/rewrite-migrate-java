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
package org.openrewrite.java.migrate.lang;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;

public class MigrateClassNewInstanceToGetDeclaredConstructorNewInstance extends Recipe {
    private static final MethodMatcher NEW_INSTANCE_MATCHER = new MethodMatcher("java.lang.Class newInstance()");

    @Getter
    final String displayName = "Use `Class#getDeclaredConstructor().newInstance()`";

    @Getter
    final String description = "Use `Class#getDeclaredConstructor().newInstance()` instead of the deprecated `Class#newInstance()` in Java 9 or higher.";

    @Getter
    final Set<String> tags = singleton( "deprecated" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(
                        new UsesJavaVersion<>(9),
                        new UsesMethod<>("java.lang.Class newInstance()")),
                new NewInstanceToDeclaredConstructorVisitor());
    }

    private static class NewInstanceToDeclaredConstructorVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final ChangeMethodName TO_DECLARED_CONS_NEW_INSTANCE = new ChangeMethodName("java.lang.Class newInstance()", "getDeclaredConstructor().newInstance", null, false);
        private final JavaType exType = JavaType.buildType("java.lang.Exception");
        private final JavaType thType = JavaType.buildType("java.lang.Throwable");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (NEW_INSTANCE_MATCHER.matches(mi)) {
                J.Try tri = getCursor().firstEnclosing(J.Try.class);
                J.Try.Catch catch_ = getCursor().firstEnclosing(J.Try.Catch.class);
                J.MethodDeclaration md = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if ((catch_ == null && tri != null && tri.getCatches().stream().anyMatch(c -> isExceptionType(c.getParameter().getType()))) ||
                    (md != null && md.getThrows() != null && md.getThrows().stream().anyMatch(nt -> isExceptionType(nt.getType())))) {
                    mi = (J.MethodInvocation) TO_DECLARED_CONS_NEW_INSTANCE.getVisitor().visitNonNull(mi, ctx);
                }
            }
            return mi;
        }

        private boolean isExceptionType(@Nullable JavaType type) {
            return TypeUtils.isOfType(type, exType) ||
                   TypeUtils.isOfType(type, thType);
        }
    }
}
