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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesField;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddTransientAnnotationToPrivateAccessor extends Recipe {

    @Override
    public String getDisplayName() {
        return "Private accessor methods must have a `@Transient` annotation";
    }

    @Override
    public String getDescription() {
        return "According to the JPA 2.1 specification, when property access is used, the property accessor methods " +
               "must be public or protected. OpenJPA ignores any private accessor methods, whereas EclipseLink persists " +
               "those attributes. To ignore private accessor methods in EclipseLink, the methods must have a " +
               "`@Transient` annotation.";
    }


    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("javax.persistence.Entity", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                        // TODO: is this needed to filter to JPA classes?
                        // Exit if parent class is not tagged for JPA
                        J.ClassDeclaration parentClass = getCursor().dropParentUntil(parent -> parent instanceof J.ClassDeclaration).getValue();
                        if (FindAnnotations.find(parentClass, "javax.persistence.Entity").isEmpty()) {
                            return method;
                        }
                        // Exit if not private accessor method
                        if (!isPrivateAccessorMethod(method)) {
                            return method;
                        }
                        // Add @Transient annotation
                        // TODO: why is this running twice? I have the javaParser specifying the jar
                        maybeAddImport("javax.persistence.Transient");
                        return JavaTemplate.builder("@Transient")
                                .contextSensitive()
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.persistence-api-2.2"))
                                .imports("javax.persistence.Transient")
                                .build()
                                .apply(getCursor(), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                    }
                }
        );
    }

    private boolean isPrivateAccessorMethod(J.MethodDeclaration method) {
        if (method.hasModifier(J.Modifier.Type.Private)
            && method.getParameters().get(0) instanceof J.Empty
            && !method.getReturnTypeExpression().toString().equals("void")
            && method.getBody().getStatements().size() == 1
            && method.getBody().getStatements().get(0) instanceof J.Return) {
            return true;
        }
        return false;
    }
}