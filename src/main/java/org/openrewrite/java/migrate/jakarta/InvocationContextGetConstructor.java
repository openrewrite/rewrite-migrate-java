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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;

public class InvocationContextGetConstructor extends Recipe {
    @Getter
    final String displayName = "Complete InvocationContext decorators";
    @Getter
    final String description = "Delegate getConstructor() when a legacy InvocationContext wrapper delegates getMethod().";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (n.getBody() != null && n.getClazz() != null &&
                        TypeUtils.isOfClassType(n.getClazz().getType(), "jakarta.interceptor.InvocationContext")) {
                    n = n.withBody(complete(n.getBody(), updateCursor(n)));
                }
                return n;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (c.getKind() == J.ClassDeclaration.Kind.Type.Class &&
                        TypeUtils.isAssignableTo("jakarta.interceptor.InvocationContext", c.getType()) &&
                        c.getType() != null && (c.getType().getSupertype() == null ||
                        "java.lang.Object".equals(c.getType().getSupertype().getFullyQualifiedName()))) {
                    c = c.withBody(complete(c.getBody(), updateCursor(c)));
                }
                return c;
            }

            private J.Block complete(J.Block body, Cursor parent) {
                Expression delegate = null;
                for (Statement statement : body.getStatements()) {
                    if (!(statement instanceof J.MethodDeclaration)) {
                        continue;
                    }
                    J.MethodDeclaration method = (J.MethodDeclaration) statement;
                    if ("getConstructor".equals(method.getSimpleName()) && method.getMethodType() != null &&
                            method.getMethodType().getParameterTypes().isEmpty()) {
                        return body;
                    }
                    if ("getMethod".equals(method.getSimpleName()) && method.getMethodType() != null &&
                            method.getMethodType().getParameterTypes().isEmpty() && method.getBody() != null &&
                            method.getBody().getStatements().size() == 1 &&
                            method.getBody().getStatements().get(0) instanceof J.Return) {
                        Expression returned = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
                        if (returned instanceof J.MethodInvocation) {
                            J.MethodInvocation call = (J.MethodInvocation) returned;
                            Expression select = call.getSelect();
                            if ("getMethod".equals(call.getSimpleName()) &&
                                    (select instanceof J.Identifier || select instanceof J.FieldAccess) &&
                                    TypeUtils.isAssignableTo("jakarta.interceptor.InvocationContext", select.getType())) {
                                delegate = select;
                            }
                        }
                    }
                }
                if (delegate == null) {
                    return body;
                }
                maybeAddImport("java.lang.reflect.Constructor");
                return JavaTemplate.builder("@Override public Constructor<?> getConstructor() { return #{any(jakarta.interceptor.InvocationContext)}.getConstructor(); }")
                        .contextSensitive().imports("java.lang.reflect.Constructor")
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(
                                "package jakarta.interceptor; public interface InvocationContext { java.lang.reflect.Method getMethod(); java.lang.reflect.Constructor<?> getConstructor(); }"))
                        .build().apply(new Cursor(parent, body), body.getCoordinates().lastStatement(), delegate);
            }
        };
    }
}
