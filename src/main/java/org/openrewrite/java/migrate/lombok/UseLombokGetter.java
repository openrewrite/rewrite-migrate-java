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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Comparator.comparing;
import static lombok.AccessLevel.PUBLIC;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseLombokGetter extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert getter methods to annotations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Convert trivial getter methods to `@Getter` annotations on their respective fields.";
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
                if (LombokUtils.isGetter(method)) {
                    Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
                    if (returnExpression instanceof J.Identifier &&
                            ((J.Identifier) returnExpression).getFieldType() != null) {
                        doAfterVisit(new FieldAnnotator(
                                ((J.Identifier) returnExpression).getFieldType(),
                                LombokUtils.getAccessLevel(method)));
                        return null;
                    } else if (returnExpression instanceof J.FieldAccess &&
                            ((J.FieldAccess) returnExpression).getName().getFieldType() != null) {
                        doAfterVisit(new FieldAnnotator(
                                ((J.FieldAccess) returnExpression).getName().getFieldType(),
                                LombokUtils.getAccessLevel(method)));
                        return null;
                    }
                }
                return method;
            }
        };
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    static class FieldAnnotator extends JavaIsoVisitor<ExecutionContext> {

        JavaType field;
        AccessLevel accessLevel;

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                if (variable.getName().getFieldType() == field) {
                    maybeAddImport("lombok.Getter");
                    maybeAddImport("lombok.AccessLevel");
                    String suffix = accessLevel == PUBLIC ? "" : String.format("(AccessLevel.%s)", accessLevel.name());
                    return JavaTemplate.builder("@Getter" + suffix)
                            .imports("lombok.Getter", "lombok.AccessLevel")
                            .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                            .build().apply(getCursor(), multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                }
            }
            return multiVariable;
        }
    }
}
