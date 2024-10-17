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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;

public class AddScopeToInjectedClass extends ScanningRecipe<Set<String>> {
    private static final String JAVAX_INJECT_INJECT = "javax.inject.Inject";
    private static final String JAVAX_ENTERPRISE_CONTEXT_DEPENDENT = "javax.enterprise.context.Dependent";

    @Override
    public String getDisplayName() {
        return "Add scope annotation to injected classes";
    }

    @Override
    public String getDescription() {
        return "Finds member variables annotated with `@Inject' and applies `@Dependent` scope annotation to the variable's type.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> injectedTypes) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                if(cd.getType() != null) {
                    for (JavaType.Variable variable : cd.getType().getMembers()) {
                        if (variableTypeRequiresScope(variable)) {
                            injectedTypes.add(((JavaType.FullyQualified) variable.getType()).getFullyQualifiedName());
                        }
                    }
                }
                return cd;
            }

            private final AnnotationMatcher matcher = new AnnotationMatcher('@' + JAVAX_INJECT_INJECT);

            private boolean variableTypeRequiresScope(JavaType.@Nullable Variable memberVariable) {
                if (memberVariable == null) {
                    return false;
                }
                for (JavaType.FullyQualified fullYQualifiedAnnotation : memberVariable.getAnnotations()) {
                    if (matcher.matchesAnnotationOrMetaAnnotation(fullYQualifiedAnnotation)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> injectedTypes) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit compilationUnit, ExecutionContext ctx) {
                J.CompilationUnit cu = super.visitCompilationUnit(compilationUnit, ctx);
                for (J.ClassDeclaration aClass : cu.getClasses()) {
                    if (aClass.getType() != null && injectedTypes.contains(aClass.getType().getFullyQualifiedName())) {
                        return (J.CompilationUnit) new AnnotateTypesVisitor(JAVAX_ENTERPRISE_CONTEXT_DEPENDENT)
                                .visitNonNull(cu, injectedTypes, getCursor().getParentTreeCursor());
                    }
                }
                return cu;
            }
        };
    }
}
