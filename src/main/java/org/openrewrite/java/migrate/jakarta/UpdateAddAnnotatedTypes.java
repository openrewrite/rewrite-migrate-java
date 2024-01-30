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
package org.openrewrite.java.migrate.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class UpdateAddAnnotatedTypes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace `BeforeBeanDiscovery.addAnnotatedType(AnnotatedType)` with `addAnnotatedType(AnnotatedType, String)`";
    }

    @Override
    public String getDescription() {
        return "`BeforeBeanDiscovery.addAnnotatedType(AnnotatedType)` is deprecated in CDI 1.1. It is Replaced by `BeforeBeanDiscovery.addAnnotatedType(AnnotatedType, String)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodInputPattern = new MethodMatcher(
                    "*.enterprise.inject.spi.BeforeBeanDiscovery addAnnotatedType(*.enterprise.inject.spi.AnnotatedType)", false);

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (methodInputPattern.matches(method)) {
                    return JavaTemplate.builder("#{any(jakarta.enterprise.inject.spi.AnnotatedType)}, null\"")
                            .build()
                            .apply(updateCursor(method),
                                    method.getCoordinates().replaceArguments(),
                                    method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
