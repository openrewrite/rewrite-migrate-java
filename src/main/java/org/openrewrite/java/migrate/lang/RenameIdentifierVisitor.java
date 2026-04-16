/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

class RenameIdentifierVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final String oldName;
    private final String newName;

    RenameIdentifierVisitor(String oldName, String newName) {
        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(
            J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        for (J.VariableDeclarations.NamedVariable v : multiVariable.getVariables()) {
            if (oldName.equals(v.getSimpleName())) {
                doAfterVisit(new RenameVariable<>(v, newName));
            }
        }
        return super.visitVariableDeclarations(multiVariable, ctx);
    }

    @Override
    public J.MethodDeclaration visitMethodDeclaration(
            J.MethodDeclaration method, ExecutionContext ctx) {
        method = super.visitMethodDeclaration(method, ctx);
        if (oldName.equals(method.getSimpleName())) {
            JavaType.Method type = method.getMethodType();
            if (type != null) {
                type = type.withName(newName);
            }
            method = method.withName(method.getName().withSimpleName(newName)
                            .withType(type))
                    .withMethodType(type);
        }
        return method;
    }

    @Override
    public J.MethodInvocation visitMethodInvocation(
            J.MethodInvocation method, ExecutionContext ctx) {
        method = super.visitMethodInvocation(method, ctx);
        if (oldName.equals(method.getSimpleName())) {
            JavaType.Method type = method.getMethodType();
            if (type != null) {
                type = type.withName(newName);
            }
            method = method.withName(method.getName().withSimpleName(newName)
                            .withType(type))
                    .withMethodType(type);
        }
        return method;
    }

    @Override
    public J.MemberReference visitMemberReference(
            J.MemberReference memberRef, ExecutionContext ctx) {
        memberRef = super.visitMemberReference(memberRef, ctx);
        if (oldName.equals(memberRef.getReference().getSimpleName())) {
            JavaType.Method type = memberRef.getMethodType();
            if (type != null) {
                type = type.withName(newName);
            }
            memberRef = memberRef.withReference(memberRef.getReference().withSimpleName(newName))
                    .withMethodType(type);
        }
        return memberRef;
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(
            J.ClassDeclaration classDecl, ExecutionContext ctx) {
        classDecl = super.visitClassDeclaration(classDecl, ctx);
        if (oldName.equals(classDecl.getSimpleName())) {
            classDecl = classDecl.withName(classDecl.getName().withSimpleName(newName));
        }
        return classDecl;
    }
}
