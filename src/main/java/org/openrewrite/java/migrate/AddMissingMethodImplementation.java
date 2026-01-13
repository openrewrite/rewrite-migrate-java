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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import static org.openrewrite.java.tree.J.ClassDeclaration.Kind.Type.Interface;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddMissingMethodImplementation extends Recipe {

    @Option(displayName = "Fully qualified class name",
            description = "A fully qualified class being implemented with missing method.",
            example = "com.yourorg.FooBar")
    String fullyQualifiedClassName;

    @Option(displayName = "Method pattern",
            description = "A method pattern for matching required method definition.",
            example = "*..* hello(..)")
    String methodPattern;

    @Option(displayName = "Method template",
            description = "Template of method to add",
            example = "public String hello() { return \\\"Hello from #{}!\\\"; }")
    String methodTemplateString;

    String displayName = "Adds missing method implementations";

    String description = "Check for missing methods required by interfaces and adds them.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(fullyQualifiedClassName, true), new ClassImplementationVisitor());
    }

    public class ClassImplementationVisitor extends JavaIsoVisitor<ExecutionContext> {

        private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, true);

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cs, ExecutionContext ctx) {
            // need to make sure we handle inner classes
            J.ClassDeclaration classDecl = super.visitClassDeclaration(cs, ctx);

            // No need to make changes to abstract classes or interfaces; only change concrete classes.
            if (classDecl.hasModifier(J.Modifier.Type.Abstract) || classDecl.getKind() == Interface) {
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

            return classDecl.withBody(JavaTemplate.builder(methodTemplateString)
                    .build()
                    .apply(new Cursor(getCursor(), classDecl.getBody()),
                            classDecl.getBody().getCoordinates().lastStatement()));
        }
    }
}
