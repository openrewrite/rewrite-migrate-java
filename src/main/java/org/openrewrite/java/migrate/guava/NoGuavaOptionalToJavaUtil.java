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

public class NoGuavaOptionalToJavaUtil extends Recipe {

    static final MethodMatcher METHOD_MATCHER = new MethodMatcher("com.google.common.base.Optional toJavaUtil()");

    @Override
    public String getDisplayName() {
        return "Remove `com.google.common.base.Optional#toJavaUtil()`";
    }

    @Override
    public String getDescription() {
        return "Remove calls to `com.google.common.base.Optional#toJavaUtil()`.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-4738", "guava"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(METHOD_MATCHER), new ReplaceToJavaUtilVisitor());
    }

    private static class ReplaceToJavaUtilVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J c = super.visitCompilationUnit(cu, ctx);
            maybeAddImport("java.util.Optional");
            maybeRemoveImport("com.google.common.base.Optional");
            return c;
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J j = super.visitMethodInvocation(method, ctx);
            if (j instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) j;
                if (METHOD_MATCHER.matches(mi)) {
                    return mi.getSelect();
                }
            }
            return j;
        }
    }
}
