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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Comparator.comparing;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
class LogVisitor extends JavaIsoVisitor<ExecutionContext> {

    private final String logType;
    private final String factoryMethodPattern;
    private final String logAnnotation;
    @Nullable
    private final String fieldName;

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);
        if (visitClassDeclaration != classDecl) {
            maybeRemoveImport(logType);
            maybeRemoveImport(factoryMethodPattern.substring(0, factoryMethodPattern.indexOf(' ')));
            maybeAddImport(logAnnotation);
            return JavaTemplate
                    .builder("@" + logAnnotation.substring(logAnnotation.lastIndexOf('.') + 1) + "\n")
                    .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                    .imports(logAnnotation)
                    .build()
                    .apply(
                            updateCursor(visitClassDeclaration),
                            visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
        }
        return classDecl;
    }

    @Override
    public J.@Nullable VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

        //there must be exactly one Logger per line
        //declaring two or more in one line is possible, but I don't care to support that
        if (multiVariable.getVariables().size() != 1) {
            return multiVariable;
        }

        if (!multiVariable.hasModifier(J.Modifier.Type.Private)||
            !multiVariable.hasModifier(J.Modifier.Type.Static)||
            !multiVariable.hasModifier(J.Modifier.Type.Final)) {
            return multiVariable;
        }

        if (!TypeUtils.isAssignableTo(logType, multiVariable.getType())) {
            return multiVariable;
        }

        //name needs to match the name of the field that lombok creates
        J.VariableDeclarations.NamedVariable var = multiVariable.getVariables().get(0);
        if (fieldName != null && !fieldName.equals(var.getSimpleName())) {
            return multiVariable;
        }

        //method call must match
        if (!new MethodMatcher(factoryMethodPattern).matches(var.getInitializer())) {
            return multiVariable;
        }

        //argument must match
        J.MethodInvocation methodCall = (J.MethodInvocation) var.getInitializer();
        String className = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class).getSimpleName();
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
