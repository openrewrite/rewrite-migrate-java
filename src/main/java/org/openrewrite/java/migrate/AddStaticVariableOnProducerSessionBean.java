/*
 * Copyright 2025 the original author or authors.
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

import lombok.val;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

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
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration cs, ExecutionContext ctx) {
                boolean isSessionBean = cs.getLeadingAnnotations().stream()
                        .map(J.Annotation::getSimpleName)
                        .anyMatch(name -> name.equals("Stateless") || name.equals("Stateful") || name.equals("Singleton"));

                if (isSessionBean) {
                    J.Block classBody = cs.getBody();
                    val statements = classBody.getStatements();
                    for (J statement : statements) {
                        if (statement instanceof J.VariableDeclarations) {
                            J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) statement;
                            for (J.Annotation annotations : variableDeclarations.getLeadingAnnotations()) {
                                if (annotations.getSimpleName().equals("Produces")) {
                                    JavaType type = variableDeclarations.getType();
                                    if (type instanceof JavaType.FullyQualified) {
                                        String fqTypeName = ((JavaType.FullyQualified) type).getFullyQualifiedName();

                                        JavaTemplate template = JavaTemplate.builder(
                                                        "@Produces\nprivate static #{any(" + fqTypeName + ")} #{any(java.lang.String)};")
                                                .imports("javax.enterprise.inject.Produces")
                                                .build();

                                        return template.apply(getCursor(),
                                                variableDeclarations.getCoordinates().replace(),
                                                variableDeclarations.getTypeExpression(),
                                                variableDeclarations.getVariables().get(0).getName());
                                    }
                                }
                            }

                        }
                    }

                }
                return super.visitClassDeclaration(cs, ctx);
            }
        };
    }
}
