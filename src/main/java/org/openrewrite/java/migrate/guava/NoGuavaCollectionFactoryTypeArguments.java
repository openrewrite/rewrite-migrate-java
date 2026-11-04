/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;

// XXX A Refaster after template cannot reuse the type arguments of the call it replaces. So the rules for these
// factories in NoGuavaRefaster write a diamond, which infers `Object` where nothing gives a target type.
public class NoGuavaCollectionFactoryTypeArguments extends Recipe {
    private static final Map<MethodMatcher, String> CONSTRUCTED_TYPES = new LinkedHashMap<>();

    static {
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Lists newArrayList()"), "java.util.ArrayList");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Lists newLinkedList()"), "java.util.LinkedList");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Lists newCopyOnWriteArrayList()"), "java.util.concurrent.CopyOnWriteArrayList");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Maps newHashMap()"), "java.util.HashMap");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Maps newLinkedHashMap()"), "java.util.LinkedHashMap");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Maps newTreeMap()"), "java.util.TreeMap");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Sets newHashSet()"), "java.util.HashSet");
        CONSTRUCTED_TYPES.put(new MethodMatcher("com.google.common.collect.Sets newLinkedHashSet()"), "java.util.LinkedHashSet");
    }

    @Getter
    final String displayName = "Keep the type arguments of Guava's collection factories";

    @Getter
    final String description = "Replace a Guava collection factory called with explicit type arguments, such as " +
            "`Lists.<String>newArrayList()`, with the constructor and the same type arguments, `new ArrayList<String>()`. " +
            "Where nothing gives the new collection a target type, a diamond would infer `Object` instead.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(CONSTRUCTED_TYPES.keySet().stream().map(UsesMethod::new).toArray(TreeVisitor[]::new)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        if (mi.getTypeParameters() == null || mi.getMethodType() == null) {
                            return mi;
                        }
                        for (Map.Entry<MethodMatcher, String> constructed : CONSTRUCTED_TYPES.entrySet()) {
                            if (constructed.getKey().matches(mi)) {
                                String type = constructed.getValue();
                                String typeArguments = mi.getTypeParameters().stream()
                                        .map(typeArgument -> typeArgument.printTrimmed(getCursor()))
                                        .collect(joining(", "));
                                maybeRemoveImport(mi.getMethodType().getDeclaringType().getFullyQualifiedName());
                                maybeAddImport(type);
                                return JavaTemplate.builder("new " + type.substring(type.lastIndexOf('.') + 1) + "<" + typeArguments + ">()")
                                        .contextSensitive()
                                        .imports(type)
                                        .build()
                                        .apply(getCursor(), mi.getCoordinates().replace());
                            }
                        }
                        return mi;
                    }
                });
    }
}
