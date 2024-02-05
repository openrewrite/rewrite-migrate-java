/*
 * Copyright 2020 the original author or authors.
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
import org.jetbrains.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class HttpSessionInvalidate extends Recipe {

    @Override
    public String getDisplayName() {
        return "Updates `javax.servlet.http.HttpSession invalidate()` to `javax.servlet.http.HttpServletRequest logout()`";
    }

    @Override
    public String getDescription() {
        return "Updates `javax.servlet.http.HttpSession invalidate()` to `javax.servlet.http.HttpServletRequest logout()`";
    }

    private final MethodMatcher INVALIDATE_METHOD_PATTERN = new MethodMatcher("javax.servlet.http.HttpSession invalidate()", false);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HttpSessionVisitor();
    }

    public class HttpSessionVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
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
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ec, "javax.servlet-3.0"))
                                .build();
                maybeAddImport("javax.servlet.http.HttpServletRequest");
                method = logoutTemplate.apply(
                        getCursor(),
                        method.getCoordinates().replace(),
                        httpServletRequestDeclaration.getVariables().get(0)
                );
                return method;
            }
            return method;
        }

        /**
         * Return the param index position of the HttpServletRequest parameter object
         * @param parentMethod
         * @return
         */
        @Nullable
        private Integer getServletRequestIndex(J.MethodDeclaration parentMethod) {
            List<JavaType> params = parentMethod.getMethodType().getParameterTypes();
            for (int i = 0; i < params.size(); ++i) {
                String paramType = ((JavaType.Class) params.get(i)).getFullyQualifiedName();
                if (paramType.equals("javax.servlet.http.HttpServletRequest")) {
                    return i;
                }
            }
            return null;
        }
    }
}
