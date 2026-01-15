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
package org.openrewrite.java.migrate.lombok;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.singleton;

public class LombokOnXToOnX_ extends Recipe {

    private static final AnnotationMatcher LOMBOK_GETTER = new AnnotationMatcher("@lombok.Getter");
    private static final AnnotationMatcher LOMBOK_SETTER = new AnnotationMatcher("@lombok.Setter");
    private static final AnnotationMatcher LOMBOK_WITH = new AnnotationMatcher("@lombok.With");
    private static final AnnotationMatcher LOMBOK_WITHER = new AnnotationMatcher("@lombok.Wither");
    private static final AnnotationMatcher LOMBOK_EQUALS_AND_HASHCODE = new AnnotationMatcher("@lombok.EqualsAndHashCode");
    private static final AnnotationMatcher LOMBOK_TO_STRING = new AnnotationMatcher("@lombok.ToString");
    private static final AnnotationMatcher LOMBOK_REQUIRED_ARGS_CONSTRUCTOR = new AnnotationMatcher("@lombok.RequiredArgsConstructor");
    private static final AnnotationMatcher LOMBOK_ALL_ARGS_CONSTRUCTOR = new AnnotationMatcher("@lombok.AllArgsConstructor");
    private static final AnnotationMatcher LOMBOK_NO_ARGS_CONSTRUCTOR = new AnnotationMatcher("@lombok.NoArgsConstructor");

    @Getter
    final String displayName = "Migrate Lombok's `@__` syntax to `onX_` for Java 8+";

    @Getter
    final String description = "Migrates Lombok's `onX` annotations from the Java 7 style using `@__` to the Java 8+ style " +
            "using `onX_`. For example, `@Getter(onMethod=@__({@Id}))` becomes `@Getter(onMethod_={@Id})`.";

    @Override
    public Set<String> getTags() {
        return singleton("lombok");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("lombok.*", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);

                        if (isLombokAnnotationWithOnX(a) && a.getArguments() != null && !a.getArguments().isEmpty()) {
                            a = a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                                if (arg instanceof J.Assignment) {
                                    J.Assignment assignment = (J.Assignment) arg;
                                    if (assignment.getVariable() instanceof J.Identifier) {
                                        J.Identifier id = (J.Identifier) assignment.getVariable();
                                        String name = id.getSimpleName();
                                        if (("onMethod".equals(name) ||
                                                "onParam".equals(name) ||
                                                "onConstructor".equals(name)) &&
                                                assignment.getAssignment() instanceof J.Annotation) {
                                            J.Annotation onXAnnotation = (J.Annotation) assignment.getAssignment();
                                            if ("__".equals(onXAnnotation.getSimpleName()) &&
                                                    onXAnnotation.getArguments() != null &&
                                                    !onXAnnotation.getArguments().isEmpty()) {
                                                // Change onMethod to onMethod_, onParam to onParam_, etc.
                                                J.Identifier on_ = id.withSimpleName(name + "_");
                                                // If there's exactly one argument, use it directly
                                                Expression newValue = onXAnnotation.getArguments().get(0);
                                                return assignment.withVariable(on_).withAssignment(newValue);
                                            }
                                        }
                                    }
                                }
                                return arg;
                            }));
                        }

                        return a;
                    }

                    private boolean isLombokAnnotationWithOnX(J.Annotation annotation) {
                        return LOMBOK_GETTER.matches(annotation) ||
                                LOMBOK_SETTER.matches(annotation) ||
                                LOMBOK_WITH.matches(annotation) ||
                                LOMBOK_WITHER.matches(annotation) ||
                                LOMBOK_EQUALS_AND_HASHCODE.matches(annotation) ||
                                LOMBOK_TO_STRING.matches(annotation) ||
                                LOMBOK_REQUIRED_ARGS_CONSTRUCTOR.matches(annotation) ||
                                LOMBOK_ALL_ARGS_CONSTRUCTOR.matches(annotation) ||
                                LOMBOK_NO_ARGS_CONSTRUCTOR.matches(annotation);
                    }
                }
        );
    }

}
