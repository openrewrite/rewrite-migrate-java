package org.openrewrite.java.migrate;

import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddStaticVariableOnProducerSessionBean extends Recipe {
    @Override
    public String getDisplayName() {
        return "Adds static variable to @Produces field that are on session bean";
    }

    @Override
    public String getDescription() {
        return "Ensures that the fields annotated with @Produces which is inside the session bean (@Stateless, @Stateful, or @Singleton) are declared static.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("jakarta.enterprise.inject.Produces", false),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {

                        J.ClassDeclaration visitingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        boolean isSessionBean = visitingClass.getLeadingAnnotations().stream()
                                .map(J.Annotation::getSimpleName)
                                .anyMatch(name -> name.equals("Stateless") || name.equals("Stateful") || name.equals("Singleton"));
                        if (isSessionBean) {
                            boolean isProduces = multiVariable.getLeadingAnnotations().stream()
                                    .anyMatch(anno -> anno.getSimpleName().equals("Produces"));

                            boolean containsStatic = multiVariable.getModifiers().stream()
                                    .anyMatch(m -> m.getType() == J.Modifier.Type.Static);

                            if (!containsStatic && isProduces) {
                                List<J.Modifier> updatedModifiers = new ArrayList<>(multiVariable.getModifiers());

                                updatedModifiers.add(new J.Modifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, null, J.Modifier.Type.Static, Collections.emptyList()));
                                return multiVariable.withModifiers(updatedModifiers);
                            }
                        }
                        return super.visitVariableDeclarations(multiVariable, executionContext);
                    }
                });
    }
}
