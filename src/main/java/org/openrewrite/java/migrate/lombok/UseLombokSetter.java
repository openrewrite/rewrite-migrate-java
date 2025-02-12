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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseLombokSetter extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert setter methods to annotations";
    }

    @Override
    public String getDescription() {
        return "Convert trivial setter methods to `@Setter` annotations on their respective fields.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (LombokUtils.isSetter(getCursor(), service(AnnotationService.class))) {
                    Expression assignmentVariable = ((J.Assignment) method.getBody().getStatements().get(0)).getVariable();
                    if (assignmentVariable instanceof J.FieldAccess &&
                            ((J.FieldAccess) assignmentVariable).getName().getFieldType() != null) {
                        doAfterVisit(new FieldAnnotator(Setter.class,
                                ((J.FieldAccess) assignmentVariable).getName().getFieldType(),
                                LombokUtils.getAccessLevel(method)));
                        return null; //delete

                    } else if (assignmentVariable instanceof J.Identifier &&
                            ((J.Identifier) assignmentVariable).getFieldType() != null) {
                        doAfterVisit(new FieldAnnotator(Setter.class,
                                ((J.Identifier) assignmentVariable).getFieldType(),
                                LombokUtils.getAccessLevel(method)));
                        return null; //delete
                    }
                }
                return method;
            }
        };
    }
}
