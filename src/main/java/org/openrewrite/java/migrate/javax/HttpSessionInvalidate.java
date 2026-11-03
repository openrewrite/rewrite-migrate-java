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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class HttpSessionInvalidate extends Recipe {
    private static final MethodMatcher INVALIDATE = new MethodMatcher("javax.servlet.http.HttpSession invalidate()", false);
    private static final MethodMatcher LOGOUT = new MethodMatcher("javax.servlet.http.HttpServletRequest logout()", false);
    private static final TypeMatcher HTTP_SERVLET_REQUEST = new TypeMatcher("javax.servlet.http.HttpServletRequest");
    private static final List<String> SERVLET_EXCEPTION_AND_SUPERTYPES = Arrays.asList("javax.servlet.ServletException", "java.lang.Exception", "java.lang.Throwable");

    String displayName = "Call `HttpServletRequest.logout()` before `HttpSession.invalidate()`";

    String description = "Add a call to `HttpServletRequest#logout()`, introduced in Servlet 3.0, before `HttpSession#invalidate()`, " +
            "so that a programmatic logout also clears the authenticated user, not only the session. " +
            "Only applies to standalone `invalidate()` statements in methods with an `HttpServletRequest` parameter " +
            "where the `ServletException` thrown by `logout()` is already declared or caught.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(INVALIDATE), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                Cursor blockCursor = getCursor();
                List<Statement> statements = b.getStatements();
                for (int i = 0; i < statements.size(); i++) {
                    Statement statement = statements.get(i);
                    if (!isCallTo(INVALIDATE, statement) || (i > 0 && isCallTo(LOGOUT, statements.get(i - 1)))) {
                        continue;
                    }
                    J.Identifier request = requestHandlingServletException(blockCursor);
                    if (request == null) {
                        return b;
                    }
                    b = JavaTemplate.builder("#{any(javax.servlet.http.HttpServletRequest)}.logout();")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.servlet-3.0"))
                            .build()
                            .apply(updateCursor(b), statement.getCoordinates().before(), request);
                }
                return b;
            }
        });
    }

    private static boolean isCallTo(MethodMatcher matcher, Statement statement) {
        return statement instanceof J.MethodInvocation && matcher.matches((J.MethodInvocation) statement);
    }

    private static J.@Nullable Identifier requestHandlingServletException(Cursor blockCursor) {
        boolean servletExceptionCaught = false;
        Object child = null;
        for (Iterator<Object> path = blockCursor.getPath(); path.hasNext(); ) {
            Object parent = path.next();
            if (parent instanceof J.Try && ((J.Try) parent).getBody() == child) {
                servletExceptionCaught |= catchesServletException((J.Try) parent);
            } else if (parent instanceof J.MethodDeclaration) {
                J.MethodDeclaration method = (J.MethodDeclaration) parent;
                return servletExceptionCaught || declaresServletException(method) ? requestParameter(method) : null;
            } else if (parent instanceof J.Lambda || parent instanceof J.ClassDeclaration || parent instanceof J.NewClass) {
                return null;
            }
            if (parent instanceof J) {
                child = parent;
            }
        }
        return null;
    }

    private static boolean catchesServletException(J.Try tryable) {
        for (J.Try.Catch catchClause : tryable.getCatches()) {
            TypeTree caught = catchClause.getParameter().getTree().getTypeExpression();
            if (caught instanceof J.MultiCatch) {
                for (NameTree alternative : ((J.MultiCatch) caught).getAlternatives()) {
                    if (isServletExceptionOrSupertype(alternative.getType())) {
                        return true;
                    }
                }
            } else if (caught != null && isServletExceptionOrSupertype(caught.getType())) {
                return true;
            }
        }
        return false;
    }

    private static boolean declaresServletException(J.MethodDeclaration method) {
        if (method.getThrows() != null) {
            for (NameTree thrown : method.getThrows()) {
                if (isServletExceptionOrSupertype(thrown.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isServletExceptionOrSupertype(@Nullable JavaType type) {
        for (String fqn : SERVLET_EXCEPTION_AND_SUPERTYPES) {
            if (TypeUtils.isOfClassType(type, fqn)) {
                return true;
            }
        }
        return false;
    }

    private static J.@Nullable Identifier requestParameter(J.MethodDeclaration method) {
        for (Statement parameter : method.getParameters()) {
            if (parameter instanceof J.VariableDeclarations && HTTP_SERVLET_REQUEST.matches(((J.VariableDeclarations) parameter).getType())) {
                return ((J.VariableDeclarations) parameter).getVariables().get(0).getName();
            }
        }
        return null;
    }
}
