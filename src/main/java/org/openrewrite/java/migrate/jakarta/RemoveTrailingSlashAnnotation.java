package org.openrewrite.java.migrate.jakarta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.List;

public class RemoveTrailingSlashAnnotation extends Recipe {

    @Option(displayName = "Annotation Type", description = "The fully qualified name of the annotation.", example = "javax.ws.rs.ApplicationPath")
    String annotationType;

    @JsonCreator
    public RemoveTrailingSlashAnnotation(@NonNull @JsonProperty("annotationType") String annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public String getDisplayName() {
        return "Remove trailing slash in Annotations";
    }

    @Override
    public String getDescription() {
        return "Remove trailing slash in annotations like `javax.ws.rs.ApplicationPath`";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveTrailingSlashAnnotation.AnnotationInvocationVisitor();
    }

    private class AnnotationInvocationVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext context) {
            String newAttributeValue, newValue = null;
            if (!TypeUtils.isOfClassType(a.getType(), annotationType)) {
                return a;
            }
            List<Expression> currentArgs = a.getArguments();
            for (Expression it : currentArgs) {
                if (it instanceof J.Assignment) {
                    J.Assignment assig = (J.Assignment) it;
                    J.Identifier var = (J.Identifier) assig.getVariable();
                    J.Literal value = (J.Literal) assig.getAssignment();
                    if (value.getValue().toString().endsWith("/*")) {
                        newValue = "\"" + value.getValue().toString().replaceAll("/\\*$", "") + "\"";
                        return a.withArguments(Collections.singletonList(assig.withAssignment(value.withValue("value").withValueSource(newValue))));
                    }
                } else if (it instanceof J.Literal) {
                    J.Literal value = (J.Literal) it;
                    if (value.getValue().toString().endsWith("/*")) {
                        newValue = "\"" + value.getValue().toString().replaceAll("/\\*$", "") + "\"";
                        return a.withArguments(Collections.singletonList(((J.Literal) it).withValue(newValue).withValueSource(newValue)));
                    }
                }
            }
            return a;
        }
    }
}
