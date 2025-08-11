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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NoGuavaOptionalFromJavaUtil extends Recipe {

    static final MethodMatcher METHOD_MATCHER = new MethodMatcher("com.google.common.base.Optional fromJavaUtil(java.util.Optional)");

    @Override
    public String getDisplayName() {
        return "Replace `com.google.common.base.Optional#fromJavaUtil(java.util.Optional)` with argument";
    }

    @Override
    public String getDescription() {
        return "Replaces `com.google.common.base.Optional#fromJavaUtil(java.util.Optional)` with argument.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-S4738", "guava"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(METHOD_MATCHER), new ReplaceFromJavaUtilVisitor());
    }

    private static class ReplaceFromJavaUtilVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J c = super.visitCompilationUnit(cu, ctx);
            maybeRemoveImport("com.google.common.base.Optional");
            maybeAddImport("java.util.Optional");
            return c;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J j = super.visitMethodInvocation(method, ctx);
            if (j instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) j;
                if (METHOD_MATCHER.matches(mi)) {
                    return mi.getArguments().get(0).withPrefix(mi.getPrefix());
                }
            }
            return j;
        }
    }
}
