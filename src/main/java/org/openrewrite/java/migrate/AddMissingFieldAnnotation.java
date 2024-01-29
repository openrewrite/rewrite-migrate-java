package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.*;
import org.openrewrite.java.search.DeclaresMethod;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddMissingFieldAnnotation extends Recipe {
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class being implemented with missing method.",
            required = false,
            example = "java.lang.Override")
    @NonNull
    String fullyQualifiedClassName;

    @Option(displayName = "Method Pattern",
            description = "A method pattern for matching required method definition.",
            required = false,
            example = "*..* hello(..)")
    @NonNull
    String methodPattern;

    @Option(displayName = "Annotation Template",
            description = "Template of annotation to add",
            example = "java.lang.Override")
    @NonNull
    String annotationTemplateString;

    @Override
    public String getDisplayName() {
        return "Adds missing annotation.";
    }

    @Override
    public String getDescription() {
        return "Check for missing annotation required by method and adds them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(new UsesType<>("javax.persistence.GeneratedValue", false), new JavaIsoVisitor<ExecutionContext>() {
            private final AnnotationMatcher GENERATED_VALUE = new AnnotationMatcher("@javax.persistence.GeneratedValue");
            private final AnnotationMatcher GENERATED_VALUE_AUTO = new AnnotationMatcher("@javax.persistence.GeneratedValue(strategy=javax.persistence.GenerationType.AUTO)");

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                Set<J.Annotation> generatedValueAnnotations = FindAnnotations.find(multiVariable, "@javax.persistence.GeneratedValue");
                boolean addAnnotation = false;
                if (!generatedValueAnnotations.isEmpty()) {
                    J.Annotation generatedValueAnnotation = generatedValueAnnotations.iterator().next();
                    List<Expression> args = generatedValueAnnotation.getArguments();
                    addAnnotation = args == null || args.isEmpty() || GENERATED_VALUE_AUTO.matches(generatedValueAnnotation);
                }

                if (addAnnotation) {
                    J.VariableDeclarations updatedVariable = JavaTemplate.apply(
                            "@javax.persistence.TableGenerator(name = \"OPENJPA_SEQUENCE_TABLE\", table = \"OPENJPA_SEQUENCE_TABLE\", pkColumnName = \"ID\", valueColumnName = \"SEQUENCE_VALUE\", pkColumnValue = \"0\")",
                            getCursor(),
                            multiVariable.getCoordinates().addAnnotation(Comparator.comparing(
                                    J.Annotation::getSimpleName)
                            )
                    );
                    return super.visitVariableDeclarations(updatedVariable, ctx);
                }
                return multiVariable;
            }

            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                if (!GENERATED_VALUE.matches(annotation) && !GENERATED_VALUE_AUTO.matches(annotation)) {
                    return annotation;
                }

                return JavaTemplate.builder("strategy = javax.persistence.GenerationType.TABLE, generator = \"OPENJPA_SEQUENCE_TABLE\"")
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), annotation.getCoordinates().replaceArguments());
            }
        });
    }
}
