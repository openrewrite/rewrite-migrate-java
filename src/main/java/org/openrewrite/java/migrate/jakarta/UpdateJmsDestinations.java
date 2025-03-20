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
package org.openrewrite.java.migrate.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class UpdateJmsDestinations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate JMS destinations";
    }

    @Override
    public String getDescription() {
        return "Migrate `@JMSDestinationDefinition` annotations in Java files to `jakarta` equivalents.";
    }

    private static final AnnotationMatcher JMS_MATCHER = new AnnotationMatcher("@javax.jms..*");
    

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("javax.jms..*", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (!JMS_MATCHER.matches(a)) {
                            return a;
                        }
                        return a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                            if (arg instanceof J.Assignment) {
                                J.Assignment as = (J.Assignment) arg;
                                if (as.getAssignment() instanceof J.Literal) {
                                    return as.withAssignment(maybeReplaceLiteralValue((J.Literal) as.getAssignment()));
                                }
                            } else if (arg instanceof J.Literal) {
                                return maybeReplaceLiteralValue((J.Literal) arg);
                            }
                            return arg;
                        }));
                    }

                    private J.Literal maybeReplaceLiteralValue(J.Literal arg) {
                        if (arg.getType() == JavaType.Primitive.String) {
                            String oldValue = (String) arg.getValue();
                            if (oldValue.contains("javax.")) {
                                String newValue = oldValue.replace("javax.", "jakarta.");
                                return arg.withValue(newValue).withValueSource('"' + newValue + '"');
                            }
                        }
                        return arg;
                    }
                }
        );
    }
}
