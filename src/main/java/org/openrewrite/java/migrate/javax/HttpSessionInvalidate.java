/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class HttpSessionInvalidate extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use HttpServletRequest `logout` method for programmatic security logout in Servlet 3.0";
    }

    @Override
    public String getDescription() {
        return "Do not rely on HttpSession `invalidate` method for programmatic security logout. Add the HttpServletRequest `logout` method which was introduced in Java EE 6 as part of the Servlet 3.0 specification.";
    }

    private final MethodMatcher INVALIDATE_METHOD_PATTERN = new MethodMatcher("javax.servlet.http.HttpSession invalidate()", false);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HttpSessionVisitor();
    }

    public class HttpSessionVisitor extends JavaIsoVisitor<ExecutionContext> {
        final TypeMatcher HTTP_SERVLET_REQUEST_TYPE_MATCHER = new TypeMatcher("javax.servlet.http.HttpServletRequest");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (INVALIDATE_METHOD_PATTERN.matches(method)) {
                // Get index of param for HttpServletRequest, from the encapsulating method declaration TODO: would like to make this cleaner...
                J.MethodDeclaration parentMethod = getCursor().dropParentUntil(parent -> parent instanceof J.MethodDeclaration).getValue();
                Integer servletReqParamIndex = getServletRequestIndex(parentMethod);

                // failed to find HttpServletRequest from parent MethodDeclaration
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
                        httpServletRequestDeclaration.getVariables().get(0)
                );
            }
            return super.visitMethodInvocation(method, ctx);
        }

        /**
         * @return the param index position of the HttpServletRequest parameter object
         */
        @Nullable
        private Integer getServletRequestIndex(J.MethodDeclaration parentMethod) {
            List<JavaType> params = parentMethod.getMethodType().getParameterTypes();
            for (int i = 0; i < params.size(); ++i) {
                if (HTTP_SERVLET_REQUEST_TYPE_MATCHER.matches(params.get(i))) {
                    return i;
                }
            }
            return null;
        }
    }
}
