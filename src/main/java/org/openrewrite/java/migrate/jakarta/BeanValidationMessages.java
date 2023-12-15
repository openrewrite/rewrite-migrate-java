package org.openrewrite.java.migrate.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class BeanValidationMessages extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Bean Validation messages";
    }

    @Override
    public String getDescription() {
        return "Migrate `javax.validation.constraints` messages found in Java files to `jakarta.validation.constraints` equivalents.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher constraintsAnnotationMatcher = new AnnotationMatcher("@javax.validation.constraints..*");

        // TODO: how to loop to get classes that have these annotations?
        return Preconditions.check(new UsesType<>("???", false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext executionContext) {

                // TODO: if not a match just return
                if (???) {
                    return annotation;
                }

                // TODO: how to get current attribute value of "message"
                String attributeValue = ???;

                if (!attributeValue.contains("javax.")) {
                    // exit if this doesn't have javax. in the message
                    return annotation;
                }

                // TODO: replace "message" attribute string with "jakarta."
                String newAttributeValue = replaceMessage("message", attributeValue, annotation);

                // TODO: now put the new attribute value in the annotation

                return annotation;
            }
        });
    }

    @Nullable
    private static String replaceMessage(@Nullable String attributeName, @Nullable String attributeValue, J.Annotation annotation) {
        if ((attributeValue != null) && attributeIsString(attributeName, annotation)) {
            return attributeValue.replace("javax.", "jakarta.");
        } else {
            return attributeValue;
        }
    }

    private static boolean attributeIsString(@Nullable String attributeName, J.Annotation annotation) {
        String actualAttributeName = (attributeName == null) ? "value" : attributeName;
        JavaType.Class annotationType = (JavaType.Class) annotation.getType();
        if (annotationType != null) {
            for (JavaType.Method m : annotationType.getMethods()) {
                if (m.getName().equals(actualAttributeName)) {
                    return TypeUtils.isOfClassType(m.getReturnType(), "java.lang.String");
                }
            }
        }
        return false;
    }
}