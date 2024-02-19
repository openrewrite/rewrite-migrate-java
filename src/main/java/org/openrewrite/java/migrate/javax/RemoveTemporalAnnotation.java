package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper=false)
public class RemoveTemporalAnnotation extends Recipe {
    /*
     *  This rule scans for the following annotation-attribute combinations where data does not need to be converted and the Temporal annotation must be removed to avoid an EclipseLink error:
     *
     *     A javax.persistence.Temporal(TemporalType.DATE) annotation on a java.sql.Date attribute
     *     A the javax.persistence.Temporal(TemporalType.TIME) annotation on a java.sql.Date attribute
     *     A the javax.persistence.Temporal(TemporalType.DATE) annotation on a java.sql.Time attribute
     *     A the javax.persistence.Temporal(TemporalType.TIME) annotation on a java.sql.Time attribute
     *     A the javax.persistence.Temporal(TemporalType.TIMESTAMP) annotation on a java.sql.Time
     *     A the javax.persistence.Temporal(TemporalType.TIMESTAMP) annotation on a java.sql.Timestamp attribute
     *
     */

    @Override
    public String getDisplayName() {
        return "Remove the `@Temporal` annotation for some `java.sql` attributes";
    }

    @Override
    public String getDescription() {
        return "OpenJPA persists the fields of attributes of type `java.sql.Date`, `java.sql.Time`, or `java.sql.Timestamp` that have a `javax.persistence.Temporal` annotation, whereas EclipseLink throws an exception.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern temporalPattern = Pattern.compile(".*TemporalType\\.(TIMESTAMP|DATE|TIME)");
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("javax.persistence.Temporal", true),
                        Preconditions.or(
                                new UsesType<>("java.sql.Date", true),
                                new UsesType<>("java.sql.Time", true),
                                new UsesType<>("java.sql.Timestamp", true)
                        )
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // T && (A || B || C
                        // !(T && (A || B || C)
                        // !T || !(A || B || C)
                        // !T || !A && !B && !C
                        // Exit if no @Temporal annotation, or var is not java.sql.Date/Time/Timestamp
                        String varClass = multiVariable.getType().toString();
                        Set<J.Annotation> temporalAnnos = FindAnnotations.find(multiVariable, "javax.persistence.Temporal");
                        if (temporalAnnos.isEmpty()
                            || !varClass.equals("java.sql.Date")
                            && !varClass.equals("java.sql.Time")
                            && !varClass.equals("java.sql.Timestamp")) {
                            return multiVariable;
                        }

                        // Get TemporalType
                        J.Annotation temporal = temporalAnnos.iterator().next();
                        String temporalDef = temporal.getArguments().iterator().next().toString();
                        Matcher temporalMatch = temporalPattern.matcher(temporalDef);
                        if (!temporalMatch.find()) {
                            return multiVariable;
                        }
                        String temporalType = temporalMatch.group(1);

                        // Check combination of attribute and var's class
                        switch (varClass) {
                            case "java.sql.Date":
                                if (!temporalType.equals("DATE") && !temporalType.equals("TIME")) {
                                    return multiVariable;
                                }
                                break;
                            case "java.sql.Timestamp":
                                if (!temporalType.equals("TIMESTAMP")) {
                                    return multiVariable;
                                }
                                break;
                            default: // TIME does not work with any
                                break;
                        }

                        // Remove @Temporal annotation on this var
                        return (J.VariableDeclarations) new RemoveAnnotation("javax.persistence.Temporal").getVisitor().visit(multiVariable, ctx);
                    }
                }
        );
    }
}