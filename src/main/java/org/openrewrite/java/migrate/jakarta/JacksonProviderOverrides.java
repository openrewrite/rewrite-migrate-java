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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class JacksonProviderOverrides extends Recipe {
    @Getter
    final String displayName = "Migrate Jackson provider configuration overrides";
    @Getter
    final String description = "Add and forward the default view parameter required by Jakarta Jackson providers.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher oldMethod = new MethodMatcher(
                    "com.fasterxml.jackson.jaxrs.base.ProviderBase _configForWriting(com.fasterxml.jackson.databind.ObjectMapper, java.lang.annotation.Annotation[])", true);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                if (!oldMethod.matches(m.getMethodType()) || m.getParameters().size() != 2) {
                    return m;
                }
                String viewName = VariableNameUtils.generateVariableName("defaultView", getCursor(),
                        VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER);
                m = JavaTemplate.builder("#{}, #{}, Class<?> " + viewName).contextSensitive().build()
                        .apply(updateCursor(m), m.getCoordinates().replaceParameters(),
                                m.getParameters().get(0).printTrimmed(getCursor()),
                                m.getParameters().get(1).printTrimmed(getCursor()));
                m = m.withParameters(ListUtils.concat(method.getParameters(), m.getParameters().get(2)));
                J.VariableDeclarations.NamedVariable view = ((J.VariableDeclarations) m.getParameters().get(2)).getVariables().get(0);
                if (m.getBody() != null) {
                    m = m.withBody((J.Block) new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, ExecutionContext ctx) {
                            J.MethodInvocation mi = super.visitMethodInvocation(invocation, ctx);
                            if (oldMethod.matches(mi) && mi.getSelect() instanceof J.Identifier &&
                                    "super".equals(((J.Identifier) mi.getSelect()).getSimpleName())) {
                                return mi.withArguments(ListUtils.concat(mi.getArguments(), view.getName().withPrefix(org.openrewrite.java.tree.Space.SINGLE_SPACE)))
                                        .withMethodType(addView(mi.getMethodType(), view.getType(), viewName));
                            }
                            return mi;
                        }
                    }.visitNonNull(m.getBody(), ctx, updateCursor(m)));
                }
                JavaType.Method updatedType = addView(method.getMethodType(), view.getType(), viewName);
                return m.withMethodType(updatedType).withName(m.getName().withType(updatedType));
            }

            private JavaType.Method addView(JavaType.Method method, JavaType type, String name) {
                return method.withParameterTypes(ListUtils.concat(method.getParameterTypes(), type))
                        .withParameterNames(ListUtils.concat(method.getParameterNames(), name));
            }
        };
    }
}
