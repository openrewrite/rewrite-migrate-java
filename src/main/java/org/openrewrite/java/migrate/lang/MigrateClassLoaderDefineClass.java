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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class MigrateClassLoaderDefineClass extends Recipe {
    private static final MethodMatcher DEFINE_CLASS_MATCHER = new MethodMatcher("java.lang.ClassLoader defineClass(byte[], int, int)");

    @Override
    public String getDisplayName() {
        return "Use `ClassLoader#defineClass(String, byte[], int, int)`";
    }

    @Override
    public String getDescription() {
        return "Use `ClassLoader#defineClass(String, byte[], int, int)` instead of the deprecated `ClassLoader#defineClass(byte[], int, int)` in Java 1.1 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate template = JavaTemplate.builder("null, #{anyArray(byte)}, #{any(int)}, #{any(int)}")
                    .contextSensitive()
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (DEFINE_CLASS_MATCHER.matches(m) && m.getArguments().size() == 3) {
                    m = template.apply(
                            updateCursor(m),
                            m.getCoordinates().replaceArguments(),
                            m.getArguments().get(0),
                            m.getArguments().get(1),
                            m.getArguments().get(2));
                }
                return m;
            }
        };
    }
}
