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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;

public class UpdateBeanManagerMethods extends Recipe {
    @Override
    public String getDisplayName() {
        return "Update `fireEvent()` and `createInjectionTarget()` calls";
    }

    @Override
    public String getDescription() {
        return " Updates `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher fireEventMatcher = new MethodMatcher("*.enterprise.inject.spi.BeanManager fireEvent(..)", false);
            private final MethodMatcher createInjectionTargetMatcher = new MethodMatcher("*.enterprise.inject.spi.BeanManager createInjectionTarget(..)", false);

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                List<Expression> arguments = mi.getArguments();
                if (fireEventMatcher.matches(method) && mi.getSelect() != null) {
                    if (arguments.size() <= 1) {
                        return JavaTemplate.builder("#{any(jakarta.enterprise.inject.spi.BeanManager)}.getEvent()" +
                                                    ".fire(#{any(jakarta.enterprise.inject.spi.BeforeBeanDiscovery)})")
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.enterprise.cdi-api-3.0.0-M4"))
                                .build()
                                .apply(updateCursor(mi), mi.getCoordinates().replace(), mi.getSelect(), arguments.get(0));
                    }

                    Object[] args = new Expression[arguments.size() + 1];
                    args[0] = mi.getSelect();
                    for (int i = 1; i < arguments.size(); i++) {
                        args[i] = arguments.get(i);
                    }
                    args[arguments.size()] = arguments.get(0);

                    String template = "#{any(jakarta.enterprise.inject.spi.BeanManager)}.getEvent()" +
                                      ".select(" + String.join(", ", Collections.nCopies(arguments.size() - 1, "#{any(java.lang.annotation.Annotation)}")) + ')' +
                                      ".fire(#{any(jakarta.enterprise.inject.spi.BeforeBeanDiscovery)})";
                    return JavaTemplate.builder(template)
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.enterprise.cdi-api-3.0.0-M4"))
                            .build()
                            .apply(updateCursor(mi), mi.getCoordinates().replace(), args);
                } else if (createInjectionTargetMatcher.matches(method) && mi.getSelect() != null) {
                    return JavaTemplate.builder("#{any(jakarta.enterprise.inject.spi.BeanManager)}.getInjectionTargetFactory(#{any(jakarta.enterprise.inject.spi.AnnotatedType)}).createInjectionTarget(null)")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.enterprise.cdi-api-3.0.0-M4"))
                            .build()
                            .apply(updateCursor(mi), mi.getCoordinates().replace(), mi.getSelect(), arguments.get(0));
                }
                return mi;
            }

        };
    }
}
