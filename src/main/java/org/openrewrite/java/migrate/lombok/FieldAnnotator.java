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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PUBLIC;

@EqualsAndHashCode(callSuper = false)
@Value
class FieldAnnotator extends JavaIsoVisitor<ExecutionContext> {

	private static final AnnotationMatcher OVERRIDE_MATCHER = new AnnotationMatcher("java.lang.Override");

	Class<?> annotation;
    JavaType field;
    AccessLevel accessLevel;
	List<J.Annotation> onMethodAnnotations;

	@Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
            if (variable.getName().getFieldType() == field) {
                // Check if the annotation already exists (can happen with multiple variable declarations)
                String annotationName = annotation.getSimpleName();
                if (multiVariable.getLeadingAnnotations().stream()
                        .noneMatch(ann -> annotationName.equals(ann.getSimpleName()))) {
                    maybeAddImport(annotation.getName());
                    maybeAddImport("lombok.AccessLevel");
                    String valueArg = accessLevel == PUBLIC ? "" : String.format("AccessLevel.%s", accessLevel.name());
					String suffix;
					onMethodAnnotations.removeIf(OVERRIDE_MATCHER::matches);
					if (onMethodAnnotations.isEmpty()) {
						 suffix = valueArg.isEmpty() ? "" : String.format("(%s)", valueArg);
					} else {
						String onMethodArg = String.format("onMethod_ = {%s}", onMethodAnnotations.stream().map(J.Annotation::toString).collect(joining(",")));
						suffix = valueArg.isEmpty() ? String.format("(%s)", onMethodArg) : String.format("(value = %s, %s)", valueArg,  onMethodArg);
					}

					return JavaTemplate.builder("@" + annotation.getSimpleName() + suffix)
                            .imports(annotation.getName(), "lombok.AccessLevel")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                            .build().apply(getCursor(), multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                }
                return multiVariable;
            }
        }
        return multiVariable;
    }
}
