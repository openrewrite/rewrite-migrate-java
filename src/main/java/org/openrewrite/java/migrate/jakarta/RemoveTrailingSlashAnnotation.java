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
