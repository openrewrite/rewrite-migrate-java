/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.jakarta;

import lombok.RequiredArgsConstructor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;

public class ApplicationPathWildcardNoLongerAccepted extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove trailing slash from `jakarta.ws.rs.ApplicationPath` values";
    }

    @Override
    public String getDescription() {
        return "Remove trailing `/*` from `jakarta.ws.rs.ApplicationPath` values.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ApplicationPathTrailingSlashVisitor();
    }

    @RequiredArgsConstructor
    private static class ApplicationPathTrailingSlashVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final AnnotationMatcher APPLICATION_PATH = new AnnotationMatcher("@jakarta.ws.rs.ApplicationPath");

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext context) {
            J.Annotation a = super.visitAnnotation(annotation, context);
            if (!APPLICATION_PATH.matches(a) || a.getArguments() == null || a.getArguments().isEmpty()) {
                return a;
            }

            Expression it = a.getArguments().get(0);
            if (it instanceof J.Assignment) {
                J.Assignment assig = (J.Assignment) it;
                if (assig.getAssignment() instanceof J.Literal) {
                    J.Literal literal = (J.Literal) assig.getAssignment();
                    String value = literal.getValue().toString();
                    if (value.endsWith("/*")) {
                        String newValue = "\"" + value.substring(0, value.length() - 2) + "\"";
                        return a.withArguments(Collections.singletonList(assig.withAssignment(literal.withValue(newValue).withValueSource(newValue))));
                    }
                } // Should we handle constants?
            } else if (it instanceof J.Literal) {
                J.Literal literal = (J.Literal) it;
                String value = literal.getValue().toString();
                if (value.endsWith("/*")) {
                    String newValue = "\"" + value.substring(0, value.length() - 2) + "\"";
                    return a.withArguments(Collections.singletonList(((J.Literal) it).withValue(newValue).withValueSource(newValue)));
                }
            }

            return a;
        }
    }
}
