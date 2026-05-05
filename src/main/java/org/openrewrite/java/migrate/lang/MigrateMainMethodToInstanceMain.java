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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.staticanalysis.VariableReferences;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateMainMethodToInstanceMain extends ScanningRecipe<Set<JavaType.FullyQualified>> {

    private static final MethodMatcher MAIN_METHOD_MATCHER = new MethodMatcher("*..* main(String[])", false);

    String displayName = "Migrate `public static void main(String[] args)` to instance `void main()`";

    String description = "Migrate `public static void main(String[] args)` method to instance `void main()` method when the `args` parameter is unused, as supported by JEP 512 in Java 25+.";

    @Override
    public Set<JavaType.FullyQualified> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<JavaType.FullyQualified> referencedMains) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (MAIN_METHOD_MATCHER.matches(method.getMethodType())) {
                    recordDeclaringType(method.getMethodType());
                }
                return super.visitMethodInvocation(method, ctx);
            }

            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                if (MAIN_METHOD_MATCHER.matches(memberRef.getMethodType())) {
                    recordDeclaringType(memberRef.getMethodType());
                }
                return super.visitMemberReference(memberRef, ctx);
            }

            private void recordDeclaringType(JavaType.@Nullable Method methodType) {
                if (methodType != null && methodType.getDeclaringType() != null) {
                    referencedMains.add(methodType.getDeclaringType());
                }
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<JavaType.FullyQualified> referencedMains) {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(25),
                new DeclaresMethod<>(MAIN_METHOD_MATCHER)
        );
        return Preconditions.check(preconditions, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                // Check if this is a main method: public static void main(String[] args)
                if (enclosingClass == null ||
                        !MAIN_METHOD_MATCHER.matches(md, enclosingClass) ||
                        md.getReturnTypeExpression() == null ||
                        md.getReturnTypeExpression().getType() != JavaType.Primitive.Void ||
                        !md.hasModifier(J.Modifier.Type.Public) ||
                        !md.hasModifier(J.Modifier.Type.Static) ||
                        md.getBody() == null) {
                    return md;
                }

                // Check if parameter is String[] type
                J.VariableDeclarations param = (J.VariableDeclarations) md.getParameters().get(0);
                JavaType paramType = param.getType();
                if (!TypeUtils.isOfClassType(paramType, "java.lang.String") || !(paramType instanceof JavaType.Array)) {
                    return md;
                }

                // Do not migrate in any of these cases
                if (hasSpringBootApplicationAnnotation(enclosingClass) ||
                        !hasNoArgConstructor(enclosingClass) ||
                        isMainMethodReferenced(enclosingClass)) {
                    return md;
                }

                // Remove the parameter if unused
                J.Identifier variableName = param.getVariables().get(0).getName();
                if (VariableReferences.findRhsReferences(md.getBody(), variableName).isEmpty()) {
                    md = md.withParameters(emptyList());
                }
                return md.withReturnTypeExpression(md.getReturnTypeExpression().withPrefix(md.getModifiers().get(0).getPrefix()))
                        .withModifiers(emptyList());
            }

            private boolean hasSpringBootApplicationAnnotation(J.ClassDeclaration classDecl) {
                return classDecl.getLeadingAnnotations().stream()
                        .anyMatch(ann -> TypeUtils.isOfClassType(ann.getType(), "org.springframework.boot.autoconfigure.SpringBootApplication"));
            }

            private boolean hasNoArgConstructor(J.ClassDeclaration classDecl) {
                List<J.MethodDeclaration> constructors = classDecl.getBody().getStatements().stream()
                        .filter(stmt -> stmt instanceof J.MethodDeclaration)
                        .map(stmt -> (J.MethodDeclaration) stmt)
                        .filter(J.MethodDeclaration::isConstructor)
                        .collect(toList());

                // If no constructors are declared, the class has an implicit public no-arg constructor
                if (constructors.isEmpty()) {
                    return true;
                }

                // Instance main requires a public no args constructor
                return constructors.stream()
                        .anyMatch(ctor -> (ctor.getParameters().isEmpty() ||
                                (ctor.getParameters().size() == 1 && ctor.getParameters().get(0) instanceof J.Empty)) &&
                                ctor.hasModifier(J.Modifier.Type.Public));
            }

            private boolean isMainMethodReferenced(J.ClassDeclaration classDecl) {
                JavaType.FullyQualified type = classDecl.getType();
                if (type == null) {
                    return false;
                }
                for (JavaType.FullyQualified referenced : referencedMains) {
                    if (TypeUtils.isOfType(referenced, type)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
