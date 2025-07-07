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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                        String rawReplacement = values.getReplacement();
                        Object[] parameters = new Object[0];
                        // TODO Process replacement to turn parameter names into templated values using actual arguments
                        return JavaTemplate.builder(rawReplacement)
                                .imports(values.getImports())
                                .staticImports(values.getStaticImports())
                                .build()
                                .apply(updateCursor(mi), mi.getCoordinates().replace(), parameters);
                    }

                    private @Nullable InlineMeValues findInlineMeValues(JavaType.@Nullable Method methodType) {
                        if (methodType == null) {
                            return null;
                        }
                        List<JavaType.FullyQualified> annotations = methodType.getAnnotations();
                        for (JavaType.FullyQualified annotation : annotations) {
                            if (INLINE_ME.equals(annotation.getFullyQualifiedName())) {
                                Map<String, Object> collect = ((JavaType.Annotation) annotation).getValues().stream()
                                        .collect(Collectors.toMap(
                                                e -> ((JavaType.Method) e.getElement()).getName(),
                                                JavaType.Annotation.ElementValue::getValue
                                        ));
                                return new InlineMeValues(
                                        (String) collect.get("replacement"),
                                        new String[0],
                                        new String[0]);
                            }
                        }
                        return null;
                    }
                }
                /*)*/;
    }


    @Value
    private static class InlineMeValues {
        String replacement;
        String[] imports;
        String[] staticImports;
    }
}
