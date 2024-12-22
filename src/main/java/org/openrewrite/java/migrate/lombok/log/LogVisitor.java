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
package org.openrewrite.java.migrate.lombok.log;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static java.util.Comparator.comparing;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
abstract class LogVisitor extends JavaIsoVisitor<ExecutionContext> {
    public static final String CLASS_NAME = "CLASS_NAME";

    final String fieldName;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

        getCursor().putMessage(CLASS_NAME, classDecl.getSimpleName());

        J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);

        //if nothing changed -> return
        if (visitClassDeclaration == classDecl) {
            return classDecl;
        }

        switchImports();
        return getLombokTemplate().apply(
                updateCursor(visitClassDeclaration),
                visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
    }

    protected abstract JavaTemplate getLombokTemplate();

    protected JavaTemplate getLombokTemplate(String name, String import_) {
        return JavaTemplate
                .builder("@"+name+"\n")
                .javaParser(JavaParser.fromJavaVersion()
                        .classpath("lombok"))
                .imports(import_)
                .build();
    }

    protected abstract void switchImports();

    protected abstract boolean methodPath(String path);
    protected abstract String expectedLoggerPath();

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

        //there must be exactly one Logger per line
        //declaring two or more in one line is possible, but I don't care to support that
        if (multiVariable.getVariables().size() != 1)
            return multiVariable;

        J.VariableDeclarations.NamedVariable var = multiVariable.getVariables().get(0);

        JavaType.Variable type = var.getVariableType();
        if (!type.hasFlags(Flag.Private, Flag.Static, Flag.Final)) {
            return multiVariable;
        }

        JavaType.FullyQualified type0 = multiVariable.getTypeAsFullyQualified();
        String path = type0.getFullyQualifiedName();
        if (!expectedLoggerPath().equals(path))
            return multiVariable;

        //name needs to match the name of the field that lombok creates todo write name normalization recipe
        if (fieldName != null && !fieldName.equals(var.getSimpleName()))
            return multiVariable;

        J.MethodInvocation methodCall = (J.MethodInvocation) var.getInitializer();

        String leftSide = methodCall.getMethodType().getDeclaringType().getFullyQualifiedName() + "." +  methodCall.getMethodType().getName();

        //method call must match
        if (!methodPath(leftSide)) {
            return multiVariable;
        }

        //argument must match
        String className = getCursor().pollNearestMessage(CLASS_NAME);
        if (methodCall.getArguments().size() != 1 ||
                !methodCall.getArguments().get(0).toString().equals(getFactoryParameter(className)
                )) {
            return multiVariable;
        }

        return null;
    }

    protected String getFactoryParameter(String className) {
        return className + ".class";
    }
}
