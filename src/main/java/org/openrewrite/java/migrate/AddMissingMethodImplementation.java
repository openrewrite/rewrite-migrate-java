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
package org.openrewrite.java.migrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddMissingMethodImplementation extends Recipe {
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class being implemented with missing method.",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    @Option(displayName = "Method Name",
            description = "Name of required method.",
            example = "hello")
    @NonNull
    String methodName;

    @Option(displayName = "Method Template",
            description = "Template of method to add",
            example = "public String hello() { return \\\"Hello from #{}!\\\"; }")
    @NonNull
    String methodTemplateString;

    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public AddMissingMethodImplementation(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName,
                                          @NonNull @JsonProperty("methodName") String methodName,
                                          @NonNull @JsonProperty("methodTemplateString") String methodTemplateString) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.methodName = methodName;
        this.methodTemplateString = methodTemplateString;
    }

    @Override
    public String getDisplayName() {
        return "Adds missing method implementations.";
    }

    @Override
    public String getDescription() {
        return "Check for missing methods required by interfaces and adds them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // getVisitor() should always return a new instance of the visitor to avoid any state leaking between cycles
        return new SayHelloVisitor();
    }

    public class SayHelloVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate methodTemplate = JavaTemplate.builder( methodTemplateString).build();

        public boolean matchesInterface(JavaType.Class type) {
            if(type != null) {
                if (type.getFullyQualifiedName().equals(fullyQualifiedClassName)) {
                    return true;
                }

                // check for matches on super interface
                List<JavaType.FullyQualified> superInterfaces = type.getInterfaces();
                boolean foundOnSuperInterface = false;
                for(JavaType.FullyQualified superInterface: superInterfaces) {
                    if (matchesInterface((JavaType.Class) superInterface)) {
                        foundOnSuperInterface = true;
                        break;
                    }
                }
                return foundOnSuperInterface;
            }

            return false;
        }

        public boolean implementsInterface(J.ClassDeclaration classDecl) {
            List<TypeTree> implementedClasses = classDecl.getImplements();
            if (implementedClasses != null) {
                for (TypeTree implementedClass : implementedClasses) {
                    JavaType.Class type = (JavaType.Class) implementedClass.getType();
                    return matchesInterface(type);
                }
            }
            return false;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            boolean implementsInterface = implementsInterface(classDecl);
            // Don't make changes to classes that don't match the fully qualified name
            if (classDecl.getType() == null || !implementsInterface) {
                return classDecl;
            }

            // Check if the class already has a method".
            boolean methodExists = classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(methodDeclaration -> methodDeclaration.getName().getSimpleName().equals(methodName));

            // If the class already has method, don't make any changes to it.
            if (methodExists) {
                return classDecl;
            }

            classDecl = classDecl.withBody( methodTemplate.apply(new Cursor(getCursor(), classDecl.getBody()),
                    classDecl.getBody().getCoordinates().lastStatement()));

            return classDecl;
        }
    }
}
