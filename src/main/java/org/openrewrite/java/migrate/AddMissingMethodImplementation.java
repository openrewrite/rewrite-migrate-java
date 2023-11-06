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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddMissingMethodImplementation extends Recipe {
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class being implemented with missing method.",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    @Option(displayName = "Method Pattern",
            description = "A method pattern for matching required method definition.",
            example = "*..* hello(..)")
    @NonNull
    String methodPattern;

    @Option(displayName = "Method Template",
            description = "Template of method to add",
            example = "public String hello() { return \\\"Hello from #{}!\\\"; }")
    @NonNull
    String methodTemplateString;

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
        return Preconditions.check(new UsesType<>(fullyQualifiedClassName, true), new ClassImplementationVisitor());
    }

    public class ClassImplementationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final JavaTemplate methodTemplate = JavaTemplate.builder(methodTemplateString).build();
        private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, true);

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cs, ExecutionContext executionContext) {
            // need to make sure we handle sub-classes
            J.ClassDeclaration classDecl = super.visitClassDeclaration(cs, executionContext);

            // No need to make changes to abstract classes; only change concrete classes.
            if (classDecl.hasModifier(J.Modifier.Type.Abstract)) {
                return classDecl;
            }
            // Don't make changes to classes that don't match the fully qualified name
            if (!TypeUtils.isAssignableTo(fullyQualifiedClassName, classDecl.getType())) {
                return classDecl;
            }
            // If the class already has method, don't make any changes to it.
            if (classDecl.getBody().getStatements().stream()
                    .filter(statement -> statement instanceof J.MethodDeclaration)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(methodDeclaration -> methodMatcher.matches(methodDeclaration, classDecl))) {
                return classDecl;
            }

            return classDecl.withBody(methodTemplate.apply(new Cursor(getCursor(), classDecl.getBody()),
                    classDecl.getBody().getCoordinates().lastStatement()));
        }
    }
}
