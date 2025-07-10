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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class HttpSessionInvalidate extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use HttpServletRequest `logout` method for programmatic security logout in Servlet 3.0";
    }

    @Override
    public String getDescription() {
        return "Do not rely on HttpSession `invalidate` method for programmatic security logout. Add the HttpServletRequest `logout` method which was introduced in Java EE 6 as part of the Servlet 3.0 specification.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher invalidateMethodMatcher = new MethodMatcher("javax.servlet.http.HttpSession invalidate()", false);
        TypeMatcher httpServletRequestTypeMatcher = new TypeMatcher("javax.servlet.http.HttpServletRequest");
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(invalidateMethodMatcher),
                        new UsesType<>("javax.servlet.http.HttpServletRequest", true)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (invalidateMethodMatcher.matches(method)) {
                            // Get index of param for HttpServletRequest, from the encapsulating method declaration TODO: would like to make this cleaner...
                            J.MethodDeclaration parentMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                            Integer servletReqParamIndex = getServletRequestIndex(parentMethod);

                            // Failed to find HttpServletRequest from parent MethodDeclaration
                            if (servletReqParamIndex == null) {
                                return method;
                            }

                            // Get the HttpServletRequest param
                            J.VariableDeclarations httpServletRequestDeclaration = (J.VariableDeclarations) parentMethod.getParameters().get(servletReqParamIndex);

                            // Replace HttpSession.invalidate() with HttpServletRequest.logout()
                            final JavaTemplate logoutTemplate =
                                    JavaTemplate.builder("#{any(javax.servlet.http.HttpServletRequest)}.logout()")
                                            .imports("javax.servlet.http.HttpServletRequest")
                                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.servlet-3.0"))
                                            .build();
                            method = logoutTemplate.apply(
                                    getCursor(),
                                    method.getCoordinates().replace(),
                                    httpServletRequestDeclaration.getVariables().get(0).getName()
                            );
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }

                    /**
                     * @return the param index position of the HttpServletRequest parameter object
                     */
                    private @Nullable Integer getServletRequestIndex(J.MethodDeclaration parentMethod) {
                        List<JavaType> params = parentMethod.getMethodType().getParameterTypes();
                        for (int i = 0; i < params.size(); ++i) {
                            if (httpServletRequestTypeMatcher.matches(params.get(i))) {
                                return i;
                            }
                        }
                        return null;
                    }
                }
        );
    }
}
