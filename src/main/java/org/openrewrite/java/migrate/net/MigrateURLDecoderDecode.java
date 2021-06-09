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
package org.openrewrite.java.migrate.net;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MigrateURLDecoderDecode extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.net.URLDecoder decode(String)");

    @Override
    public String getDisplayName() {
        return "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)`";
    }

    @Override
    public String getDescription() {
        return "`java.net.URLDecoder#decode(String)` is platform-dependent. It's advised to specify an encoding.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateURLDecoderDecodeVisitor();
    }

    private static class MigrateURLDecoderDecodeVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            if (MATCHER.matches(m)) {
                m = m.withTemplate(
                        template("#{any(String)}, StandardCharsets.UTF_8")
                                .imports("java.nio.charset.StandardCharsets")
                                .build(),
                        m.getCoordinates().replaceArguments(),
                        m.getArguments().toArray()
                );
                // forcing an import, otherwise maybeAddImport appears to be having trouble recognizing importing this
                // believe it may have to do with this being a field, or possibly this is incorrect usage // todo
                doAfterVisit(new AddImport<>("java.nio.charset.StandardCharsets", null, false));
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }

}
