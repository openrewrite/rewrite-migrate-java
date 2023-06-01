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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddImport;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class MigrateURLDecoderDecode extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.net.URLDecoder decode(String)");

    @Override
    public String getDisplayName() {
        return "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)`";
    }

    @Override
    public String getDescription() {
        return "Use `java.net.URLDecoder#decode(String, StandardCharsets.UTF_8)` instead of the deprecated `java.net.URLDecoder#decode(String)` in Java 10 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesJavaVersion<>(10), new UsesMethod<>(MATCHER)), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                if (MATCHER.matches(m)) {
                    m = JavaTemplate.builder("#{any(String)}, StandardCharsets.UTF_8")
                            .contextSensitive()
                            .imports("java.nio.charset.StandardCharsets")
                            .build().apply(
                                    getCursor(),
                                    m.getCoordinates().replaceArguments(),
                                    m.getArguments().toArray());
                    // forcing an import, otherwise maybeAddImport appears to be having trouble recognizing importing this
                    // believe it may have to do with this being a field, or possibly this is incorrect usage // todo
                    doAfterVisit(new AddImport<>("java.nio.charset.StandardCharsets", null, false));
                }
                return super.visitMethodInvocation(m, ctx);
            }
        });
    }
}
