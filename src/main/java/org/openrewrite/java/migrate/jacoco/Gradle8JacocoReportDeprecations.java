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
package org.openrewrite.java.migrate.jacoco;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class Gradle8JacocoReportDeprecations extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace Gradle 8 introduced deprecations in JaCoCo report task";
    }

    @Override
    public String getDescription() {
        return "Set the `enabled` to `required` and the `destination` to `outputLocation` for Reports deprecations that were removed in gradle 8. " +
                "See [the gradle docs on this topic](https://docs.gradle.org/current/userguide/upgrading_version_7.html#report_and_testreport_api_cleanup).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J. Assignment visitAssignment(J. Assignment assignment, ExecutionContext ctx) {
                String prefix = getCursor().getNearestMessage("path");
                String prefixOrEmpty = prefix == null ? "" : prefix + ".";
                if (assignment.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();
                    String fieldName = getFieldName(fieldAccess);
                    return replaceDeprecations(assignment, prefixOrEmpty + fieldName);
                } else if (assignment.getVariable() instanceof J.Identifier) {
                    J.Identifier identifier = (J.Identifier) assignment.getVariable();
                    return replaceDeprecations(assignment, prefixOrEmpty + identifier.getSimpleName());
                }
                return assignment;
            }

            @Override
            public J. MethodInvocation visitMethodInvocation(J. MethodInvocation method, ExecutionContext ctx) {
                String parent = getCursor().getNearestMessage("path");
                if (parent == null) {
                    getCursor().putMessage("path", method.getSimpleName());
                } else {
                    getCursor().putMessage("path", parent + "." + method.getSimpleName());
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private J.Assignment replaceDeprecations(J. Assignment assignment, String path) {
                String[] parts = path.split("\\.");
                if (parts.length == 4 && "jacocoTestReport".equalsIgnoreCase(parts[0]) && "reports".equalsIgnoreCase(parts[1])) {
                    String reportType = parts[2];
                    if ("xml".equalsIgnoreCase(reportType) || "csv".equalsIgnoreCase(reportType) || "html".equalsIgnoreCase(reportType)) {
                        if (assignment.getVariable() instanceof J.FieldAccess) {
                            J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();
                            if ("enabled".equalsIgnoreCase(parts[3])) {
                                return assignment.withVariable(fieldAccess.withName(fieldAccess.getName().withSimpleName("required")));
                            } else if ("destination".equalsIgnoreCase(parts[3])) {
                                return assignment.withVariable(fieldAccess.withName(fieldAccess.getName().withSimpleName("outputLocation")));
                            }
                        } else if (assignment.getVariable() instanceof J.Identifier) {
                            J.Identifier identifier = (J.Identifier) assignment.getVariable();
                            if ("enabled".equalsIgnoreCase(parts[3])) {
                                return assignment.withVariable(identifier.withSimpleName("required"));
                            } else if ("destination".equalsIgnoreCase(parts[3])) {
                                return assignment.withVariable(identifier.withSimpleName("outputLocation"));
                            }
                        }
                    }
                }
                return assignment;
            }

            private String getFieldName(J. FieldAccess fieldAccess) {
                String fieldName = fieldAccess.getSimpleName();
                Expression target = fieldAccess;
                while (target instanceof J.FieldAccess) {
                    target = ((J.FieldAccess) target).getTarget();
                    if (target instanceof J.Identifier) {
                        fieldName = ((J.Identifier) target).getSimpleName() + "." + fieldName;
                    } else if (target instanceof J.FieldAccess){
                        fieldName = ((J.FieldAccess) target).getSimpleName() + "." + fieldName;
                    }
                }
                return fieldName;
            }
        });
    }
}
