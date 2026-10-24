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
package org.openrewrite.java.migrate.jakarta;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class UpdateGetRealPath extends Recipe {
    private static final MethodMatcher GET_REAL_PATH = new MethodMatcher("jakarta.servlet.ServletRequest* getRealPath(String)", false);

    @Getter
    final String displayName = "Updates `getRealPath()` to call `getContext()` followed by `getRealPath()`";

    @Getter
    final String description = "Updates `getRealPath()` for `jakarta.servlet.ServletRequest` and `jakarta.servlet.ServletRequestWrapper` to use `ServletContext.getRealPath(String)`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_REAL_PATH), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (GET_REAL_PATH.matches(m)) {
                    if (m.getSelect() == null) {
                        return JavaTemplate.builder("getServletContext().getRealPath(#{any(String)})")
                                .contextSensitive()
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.servlet-api-6.0.0"))
                                .build()
                                .apply(updateCursor(m), m.getCoordinates().replace(), m.getArguments().get(0));
                    }
                    return JavaTemplate.builder("#{any(jakarta.servlet.ServletRequest)}.getServletContext().getRealPath(#{any(String)})")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.servlet-api-6.0.0"))
                            .build()
                            .apply(updateCursor(m),
                                    m.getCoordinates().replace(),
                                    m.getSelect(),
                                    m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
