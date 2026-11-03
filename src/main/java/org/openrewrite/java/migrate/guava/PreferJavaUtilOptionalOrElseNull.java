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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class PreferJavaUtilOptionalOrElseNull extends Recipe {

    @Getter
    final String displayName = "Prefer `java.util.Optional#orElse(null)` over `com.google.common.base.Optional#orNull()`";

    @Getter
    final String description = "Replaces `com.google.common.base.Optional#orNull()` with `java.util.Optional#orElse(null)`.";

    @Getter
    final Set<String> tags = new HashSet<>( Arrays.asList( "RSPEC-S4738", "guava" ) );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>("com.google.common.base.Optional orNull()"), new PreferJavaUtilOptionalOrElseNullVisitor());
    }

    private static class PreferJavaUtilOptionalOrElseNullVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher OPTIONAL_OR_NULL_MATCHER = new MethodMatcher("com.google.common.base.Optional orNull()");

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            maybeRemoveImport("com.google.common.base.Optional");
            maybeAddImport("java.util.Optional");
            return c;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (OPTIONAL_OR_NULL_MATCHER.matches(mi)) {
                JavaType.Method orNull = mi.getMethodType();
                JavaType.Method orElse = orNull.withName("orElse").withParameterTypes(singletonList(orNull.getReturnType()));
                mi = mi.withName(mi.getName().withSimpleName("orElse").withType(orElse))
                        .withMethodType(orElse)
                        .withArguments(singletonList(new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, null, "null", null, JavaType.Primitive.Null)));
            }
            return mi;
        }
    }
}
