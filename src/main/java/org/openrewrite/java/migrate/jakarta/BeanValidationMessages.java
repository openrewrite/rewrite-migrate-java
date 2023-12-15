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

public class BeanValidationMessages extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Bean Validation messages";
    }

    @Override
    public String getDescription() {
        return "Migrate `javax.validation.constraints` messages found in Java files to `jakarta.validation.constraints` equivalents.";
    }

    private static final AnnotationMatcher JAVAX_MATCHER = new AnnotationMatcher("@javax.validation.constraints..*");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("javax.validation.constraints..*", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {
                        J.Annotation a = super.visitAnnotation(annotation, executionContext);
                        if (!JAVAX_MATCHER.matches(a)) {
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