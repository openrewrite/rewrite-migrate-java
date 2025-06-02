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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateManagedBeanToNamed extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update Faces `@ManagedBean` to use CDI `@Named`";
    }

    @Override
    public String getDescription() {
        return "Faces ManagedBean was deprecated in JSF 2.3 (EE8) and removed in Jakarta Faces 4.0 (EE10). " +
               "Replace `@ManagedBean` with `@Named` for CDI-based bean management.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final AnnotationMatcher MANAGED_BEAN_MATCHER_JAVAX = new AnnotationMatcher("javax.faces.bean.ManagedBean");
            private final AnnotationMatcher MANAGED_BEAN_MATCHER_JAKARTA = new AnnotationMatcher("jakarta.faces.bean.ManagedBean");

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (MANAGED_BEAN_MATCHER_JAVAX.matches(annotation) || MANAGED_BEAN_MATCHER_JAKARTA.matches(annotation)) {
                    // Get the name from the @ManagedBean annotation
                    String beanName = annotation.getArguments() == null ? null :
                            annotation.getArguments().stream()
                                    .filter(J.Assignment.class::isInstance)
                                    .map(J.Assignment.class::cast)
                                    .filter(arg -> arg.getVariable().toString().equals("name"))
                                    .findFirst()
                                    .map(arg -> arg.getAssignment().toString())
                                    .orElse(null);
                    maybeAddImport("jakarta.inject.Named");
                    maybeRemoveImport("javax.faces.bean.ManagedBean");
                    maybeRemoveImport("jakarta.faces.bean.ManagedBean");
                    // Replace the @ManagedBean annotation with @Named
                    if (beanName != null) {
                        return JavaTemplate.builder("@Named(\"#{}\")")
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.inject-api-2.0.1"))
                                .imports("jakarta.inject.Named")
                                .build()
                                .apply(getCursor(), annotation.getCoordinates().replace(), beanName);
                    } else {
                        return JavaTemplate.builder("@Named")
                                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.inject-api-2.0.1"))
                                .imports("jakarta.inject.Named")
                                .build()
                                .apply(getCursor(), annotation.getCoordinates().replace());
                    }
                }
                return annotation;
            }
        };
    }
}
