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
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static java.util.Comparator.comparing;
import static lombok.AccessLevel.PUBLIC;

@EqualsAndHashCode(callSuper = false)
@Value
class FieldAnnotator extends JavaIsoVisitor<ExecutionContext> {

    Class<?> annotation;
    JavaType field;
    AccessLevel accessLevel;

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
            if (variable.getName().getFieldType() == field) {
                maybeAddImport(annotation.getName());
                maybeAddImport("lombok.AccessLevel");
                String suffix = accessLevel == PUBLIC ? "" : String.format("(AccessLevel.%s)", accessLevel.name());
                return JavaTemplate.builder("@" + annotation.getSimpleName() + suffix)
                        .imports(annotation.getName(), "lombok.AccessLevel")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build().apply(getCursor(), multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }
        }
        return multiVariable;
    }
}
