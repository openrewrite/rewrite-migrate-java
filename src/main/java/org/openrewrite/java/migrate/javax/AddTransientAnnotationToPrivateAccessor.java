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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
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
                    List<JavaType.Variable> classVars = new ArrayList<>();
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        // Collect all class variables
                        classVars = classDecl.getBody().getStatements().stream()
                                .filter(J.VariableDeclarations.class::isInstance)
                                .map(J.VariableDeclarations.class::cast)
                                .map(J.VariableDeclarations::getVariables)
                                .flatMap(Collection::stream)
                                .map(var -> var.getName().getFieldType())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        return super.visitClassDeclaration(classDecl, ctx);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration md, ExecutionContext ctx) {
                        if (isPrivateAccessorMethodWithoutTransientAnnotation(md)) {
                            // Add @Transient annotation
                            maybeAddImport("javax.persistence.Transient");
                            return JavaTemplate.builder("@Transient")
                                    .contextSensitive()
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.persistence-api-2.2"))
                                    .imports("javax.persistence.Transient")
                                    .build()
                                    .apply(getCursor(), md.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        }
                        return md;
                    }

                    private boolean isPrivateAccessorMethodWithoutTransientAnnotation(J.MethodDeclaration method) {
                        return method.hasModifier(J.Modifier.Type.Private)
                               && method.getParameters().get(0) instanceof J.Empty
                               && method.getReturnTypeExpression().getType() != JavaType.Primitive.Void
                               && FindAnnotations.find(method, "javax.persistence.Transient").isEmpty()
                               && methodReturnsFieldFromClass(method);
                    }

                    /**
                     * Check if the given method returns a field defined in the parent class
                     */
                    private boolean methodReturnsFieldFromClass(J.MethodDeclaration method) {
                        // Get all return values from method
                        List<JavaType.Variable> returns = new ArrayList<>();
                        JavaIsoVisitor<List<JavaType.Variable>> returnValueCollector = new JavaIsoVisitor<List<JavaType.Variable>>() {
                            @Override
                            public J.Return visitReturn(J.Return ret, List<JavaType.Variable> returnedVars) {
                                Expression expression = ret.getExpression();
                                JavaType.Variable returnedVar;
                                if (expression instanceof J.FieldAccess) {
                                    returnedVar = ((J.FieldAccess) expression).getName().getFieldType();
                                    returnedVars.add(returnedVar);
                                } else if (expression instanceof J.Identifier) { // ie: return field;
                                    returnedVar = ((J.Identifier) expression).getFieldType();
                                    returnedVars.add(returnedVar);
                                }
                                return super.visitReturn(ret, returnedVars);
                            }
                        };
                        returnValueCollector.visitBlock(method.getBody(), returns);

                        // Check if any return values are a class field
                        return returns.stream().anyMatch(classVars::contains);
                    }
                }
        );
    }
}
