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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
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
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("jakarta", "faces", "jsf"));
    }

    private static final AnnotationMatcher MANAGED_BEAN_MATCHER = new AnnotationMatcher("*.faces.bean.ManagedBean");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Preconditions.Check(new UsesType<>("*.faces.bean.ManagedBean", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        Optional<Annotated> annotated = new Annotated.Matcher(MANAGED_BEAN_MATCHER).get(getCursor());
                        if (annotated.isPresent()) {
                            maybeRemoveImport("javax.faces.bean.ManagedBean");
                            maybeAddImport("jakarta.inject.Named");
                            maybeRemoveImport("jakarta.faces.bean.ManagedBean");
                            // Get the name from the @ManagedBean annotation
                            String beanName = annotated
                                    .flatMap(a -> a.getDefaultAttribute("name"))
                                    .map(name -> name.getValue(String.class))
                                    .orElse(null);
                            // Replace the @ManagedBean annotation with @Named
                            if (beanName != null) {
                                return JavaTemplate.builder("@Named(\"#{}\")")
                                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.inject-api-2.0.1"))
                                        .imports("jakarta.inject.Named")
                                        .build()
                                        .apply(getCursor(), annotation.getCoordinates().replace(), beanName);
                            }
                            return JavaTemplate.builder("@Named")
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "jakarta.inject-api-2.0.1"))
                                    .imports("jakarta.inject.Named")
                                    .build()
                                    .apply(getCursor(), annotation.getCoordinates().replace());
                        }
                        return annotation;
                    }
                });
    }
}
