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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Comparator;
import java.util.Set;

public class AnnotateTypesVisitor extends JavaIsoVisitor<Set<String>> {
    private final String annotationToBeAdded;
    private final AnnotationMatcher annotationMatcher;
    private final JavaTemplate template;

    public AnnotateTypesVisitor(String annotationToBeAdded) {
        this.annotationToBeAdded = annotationToBeAdded;
        String[] split = this.annotationToBeAdded.split("\\.");
        String className = split[split.length - 1];
        String packageName = this.annotationToBeAdded.substring(0, this.annotationToBeAdded.lastIndexOf("."));
        this.annotationMatcher = new AnnotationMatcher("@" + this.annotationToBeAdded);
        String interfaceAsString = String.format("package %s; public @interface %s {}", packageName, className);
        this.template = JavaTemplate.builder("@" + className)
                .imports(this.annotationToBeAdded)
                .javaParser(JavaParser.fromJavaVersion().dependsOn(interfaceAsString))
                .build();
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> injectedTypes) {
        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, injectedTypes);
        if (injectedTypes.contains(TypeUtils.asFullyQualified(cd.getType()).getFullyQualifiedName())
            && cd.getLeadingAnnotations().stream().noneMatch(annotationMatcher::matches)) {
            maybeAddImport(annotationToBeAdded);
            return template.apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
        }
        return cd;
    }
}
