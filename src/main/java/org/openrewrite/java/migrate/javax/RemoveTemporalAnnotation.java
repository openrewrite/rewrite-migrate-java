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

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveTemporalAnnotation extends Recipe {
    /*
     * This rule scans for the following annotation-attribute combinations where data does not need to be converted
     * and the Temporal annotation must be removed to avoid an EclipseLink error:
     *
     *     A javax.persistence.Temporal(TemporalType.DATE) annotation on a java.sql.Date attribute
     *     A the javax.persistence.Temporal(TemporalType.TIME) annotation on a java.sql.Date attribute
     *     A the javax.persistence.Temporal(TemporalType.DATE) annotation on a java.sql.Time attribute
     *     A the javax.persistence.Temporal(TemporalType.TIME) annotation on a java.sql.Time attribute
     *     A the javax.persistence.Temporal(TemporalType.TIMESTAMP) annotation on a java.sql.Time
     *     A the javax.persistence.Temporal(TemporalType.TIMESTAMP) annotation on a java.sql.Timestamp attribute
     *
     * NOTES: @Temporal has a required argument, which can only be TemporalType.DATE/TIME/TIMESTAMP
     */

    @Override
    public String getDisplayName() {
        return "Remove the `@Temporal` annotation for some `java.sql` attributes";
    }

    @Override
    public String getDescription() {
        return "OpenJPA persists the fields of attributes of type `java.sql.Date`, `java.sql.Time`, or `java.sql.Timestamp` " +
               "that have a `javax.persistence.Temporal` annotation, whereas EclipseLink throws an exception. " +
               "Remove the `@Temporal` annotation so the behavior in EclipseLink will match the behavior in OpenJPA.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern temporalPattern = Pattern.compile(".*TemporalType\\.(TIMESTAMP|DATE|TIME)");
        final String JAVA_SQL_TIMESTAMP = "java.sql.Timestamp";
        final String JAVA_SQL_TIME = "java.sql.Time";
        final String JAVA_SQL_DATE = "java.sql.Date";

        Set<String> javaSqlDateTimeTypes = Stream.of(
                JAVA_SQL_TIMESTAMP,
                JAVA_SQL_TIME,
                JAVA_SQL_DATE
        ).collect(Collectors.toSet());
        // Combinations of TemporalType and java.sql classes that do not need removal
        Map<String, String> doNotRemove = Stream.of(new String[][]{
                {"DATE", JAVA_SQL_TIMESTAMP},
                {"TIME", JAVA_SQL_TIMESTAMP},
                {"TIMESTAMP", JAVA_SQL_DATE}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));
        // TODO: maybe future recipe to handle these by creating a converter class
        // https://wiki.eclipse.org/EclipseLink/Examples/JPA/Migration/OpenJPA/Mappings#.40Temporal_on_java.sql.Date.2FTime.2FTimestamp_fields

        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("javax.persistence.Temporal", true),
                        Preconditions.or(
                                new UsesType<>(JAVA_SQL_DATE, true),
                                new UsesType<>(JAVA_SQL_TIME, true),
                                new UsesType<>(JAVA_SQL_TIMESTAMP, true)
                        )
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if no @Temporal annotation, or var is not java.sql.Date/Time/Timestamp
                        String varClass = multiVariable.getType().toString();
                        Set<J.Annotation> temporalAnnos = FindAnnotations.find(multiVariable, "javax.persistence.Temporal");
                        if (temporalAnnos.isEmpty() || !javaSqlDateTimeTypes.contains(varClass)) {
                            return multiVariable;
                        }

                        // Get TemporalType
                        J.Annotation temporal = temporalAnnos.iterator().next();
                        String temporalArg = temporal.getArguments().iterator().next().toString();
                        Matcher temporalMatch = temporalPattern.matcher(temporalArg);
                        if (!temporalMatch.find()) {
                            return multiVariable;
                        }
                        String temporalType = temporalMatch.group(1);

                        // Check combination of attribute and var's class
                        if (doNotRemove.get(temporalType).equals(varClass)) {
                            return multiVariable;
                        }

                        // Remove @Temporal annotation
                        return (J.VariableDeclarations) new RemoveAnnotation("javax.persistence.Temporal").getVisitor().visit(multiVariable, ctx);
                    }
                }
        );
    }
}
