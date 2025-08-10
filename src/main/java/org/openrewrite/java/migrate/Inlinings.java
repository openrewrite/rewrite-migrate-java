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
package org.openrewrite.java.migrate;

import lombok.AccessLevel;
import lombok.Getter;
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
import org.openrewrite.java.tree.MethodCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class Inlinings extends Recipe {

    private static final String INLINE_ME = "com.google.errorprone.annotations.InlineMe";

    @Override
    public String getDisplayName() {
        return "Inline methods annotated with `@InlineMe`";
    }

    @Override
    public String getDescription() {
        return "Apply inlinings defined by Error Prone's [`@InlineMe` annotation](https://errorprone.info/docs/inlineme).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return //Preconditions.check(
//                new UsesType<>(INLINE_ME, true), // FIXME Not picked up that we're calling an annotated method
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        InlineMeValues values = findInlineMeValues(mi.getMethodType());
                        if (values == null) {
                            return mi;
                        }
                        Template template = values.template(mi);
                        if (template == null) {
                            return mi;
                        }
                        return JavaTemplate.builder(template.getString())
                                .contextSensitive()
                                .doBeforeParseTemplate(System.out::println)
                                .doAfterVariableSubstitution(System.out::println)
                                .imports(values.getImports())
                                .staticImports(values.getStaticImports())
                                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                                .build()
                                .apply(updateCursor(mi), mi.getCoordinates().replace(), template.getParameters());
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass nc = super.visitNewClass(newClass, ctx);
                        InlineMeValues values = findInlineMeValues(nc.getConstructorType());
                        if (values == null) {
                            return nc;
                        }
                        Template template = values.template(nc);
                        if (template == null) {
                            return nc;
                        }
                        return JavaTemplate.builder(template.getString())
                                .contextSensitive()
                                .doBeforeParseTemplate(System.out::println)
                                .doAfterVariableSubstitution(System.out::println)
                                .imports(values.getImports())
                                .staticImports(values.getStaticImports())
                                .javaParser(JavaParser.fromJavaVersion().classpath(JavaParser.runtimeClasspath()))
                                .build()
                                .apply(updateCursor(nc), nc.getCoordinates().replace(), template.getParameters());
                    }

                    private @Nullable InlineMeValues findInlineMeValues(JavaType.@Nullable Method methodType) {
                        if (methodType == null) {
                            return null;
                        }
                        List<JavaType.FullyQualified> annotations = methodType.getAnnotations();
                        for (JavaType.FullyQualified annotation : annotations) {
                            if (INLINE_ME.equals(annotation.getFullyQualifiedName())) {
                                return InlineMeValues.parse((JavaType.Annotation) annotation);
                            }
                        }
                        return null;
                    }
                }
                /*)*/;
    }

    @Value
    private static class InlineMeValues {
        private static final Pattern TEMPLATE_IDENTIFIER = Pattern.compile("#\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*):any\\(.*?\\)}");

        @Getter(AccessLevel.NONE)
        String replacement;

        String[] imports;
        String[] staticImports;

        static InlineMeValues parse(JavaType.Annotation annotation) {
            Map<String, Object> collect = annotation.getValues().stream()
                    .collect(Collectors.toMap(
                            e -> ((JavaType.Method) e.getElement()).getName(),
                            JavaType.Annotation.ElementValue::getValue
                    ));
            String replacement = (String) collect.get("replacement");

            // Parse imports and static imports from the annotation values
            String[] imports = parseImports(collect.get("imports"));
            String[] staticImports = parseImports(collect.get("staticImports"));

            return new InlineMeValues(replacement, imports, staticImports);
        }

        private static String[] parseImports(@Nullable Object importsValue) {
            if (importsValue instanceof List) {
                return ((List<?>) importsValue).stream()
                        .map(Object::toString)
                        .toArray(String[]::new);
            }
            return new String[]{};
        }

        @Nullable
        Template template(MethodCall original) {
            JavaType.Method methodType = original.getMethodType();
            if (methodType == null) {
                return null;
            }
            String templateString = createTemplateString(original, replacement, methodType.getParameterNames());
            List<Object> parameters = createParameters(templateString, original);
            return new Template(templateString, parameters.toArray(new Object[0]));
        }

        private static String createTemplateString(MethodCall original, String replacement, List<String> originalParameterNames) {
            String templateString = original instanceof J.MethodInvocation &&
                    ((J.MethodInvocation) original).getSelect() == null &&
                    replacement.startsWith("this.") ?
                    replacement.replaceFirst("^this.\\b", "") :
                    replacement.replaceAll("\\bthis\\b", "#{this:any()}");
            for (String parameterName : originalParameterNames) {
                // Replace parameter names with their values in the templateString
                templateString = templateString.replaceAll(
                        String.format("\\b%s\\b", parameterName),
                        String.format("#{%s:any()}", parameterName)); // TODO 2nd, 3rd etc should use shorthand `#{a}`
            }
            return templateString;
        }

        private static List<Object> createParameters(String templateString, MethodCall original) {
            Map<String, Expression> lookup = new HashMap<>();
            if (original instanceof J.MethodInvocation) {
                Expression select = ((J.MethodInvocation) original).getSelect();
                if (select != null) {
                    lookup.put("this", select);
                }
            }
            List<String> originalParameterNames = requireNonNull(original.getMethodType()).getParameterNames();
            for (int i = 0; i < originalParameterNames.size(); i++) {
                String originalName = originalParameterNames.get(i);
                Expression originalValue = original.getArguments().get(i);
                lookup.put(originalName, originalValue);
            }
            List<Object> parameters = new ArrayList<>();
            Matcher matcher = TEMPLATE_IDENTIFIER.matcher(templateString);
            while (matcher.find()) {
                Expression o = lookup.get(matcher.group(1));
                if (o != null) {
                    parameters.add(o);
                }
            }
            return parameters;
        }
    }

    @Value
    private static class Template {
        String string;
        Object[] parameters;
    }
}
