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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@EqualsAndHashCode(callSuper = false)
@Value
public class DoesNotRequireGuavaType extends Recipe {

    @Option(displayName = "Guava type",
            description = "The fully qualified name of the Guava type to replace.",
            example = "com.google.common.base.Optional")
    String guavaType;

    @Option(displayName = "Java type",
            description = "The fully qualified name of the Java standard library type that replaces it.",
            example = "java.util.Optional")
    String javaType;

    @Option(displayName = "Replaced methods",
            description = "Method patterns of the calls that the same recipe list replaces with Java standard library calls, " +
                          "so these calls do not need the Guava type.",
            example = "com.google.common.base.Predicates and(..)",
            required = false)
    @Nullable
    List<String> replacedMethods;

    String displayName = "Check whether a Guava type can be replaced";

    String description = "Useful as a precondition to skip over source files that call a method whose parameter " +
                         "needs the Guava type, or whose returned Guava type does not fit the Java type.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof JavaSourceFile && !requiresGuavaType(((JavaSourceFile) tree).getTypesInUse())) {
                    return SearchResult.found(tree);
                }
                return tree;
            }
        };
    }

    private boolean requiresGuavaType(TypesInUse typesInUse) {
        List<MethodMatcher> replaced = replacedMethods == null ? emptyList() :
                replacedMethods.stream().map(MethodMatcher::new).collect(toList());
        Set<String> typesDeclaredInFile = new HashSet<>();
        for (JavaType.Method declared : typesInUse.getDeclaredMethods()) {
            typesDeclaredInFile.add(declared.getDeclaringType().getFullyQualifiedName());
        }
        for (JavaType.Method method : typesInUse.getUsedMethods()) {
            if (replaced.stream().anyMatch(matcher -> matcher.matches(method))) {
                continue;
            }
            String declaringType = method.getDeclaringType().getFullyQualifiedName();
            if (declaringType.equals(guavaType)) {
                // Changing the type moves a static method onto the Java type, so it counts unless the recipe list replaces it
                if (method.hasFlags(Flag.Static)) {
                    return true;
                }
                continue;
            }
            if (changesAlongWithType(declaringType, typesDeclaredInFile)) {
                continue;
            }
            for (JavaType parameterType : method.getParameterTypes()) {
                if (TypeUtils.isOfClassType(parameterType, guavaType)) {
                    return true;
                }
            }
            if (TypeUtils.isOfClassType(method.getReturnType(), guavaType) &&
                !TypeUtils.isAssignableTo(javaType, method.getReturnType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Changing the type rewrites methods declared in this file together with their calls.
     */
    private boolean changesAlongWithType(String declaringType, Set<String> typesDeclaredInFile) {
        // XXX ChangeMethodName, ChangeType and ChangeMethodTargetToStatic keep the old method's parameter types,
        // so a call they moved to a JDK method still names the Guava type. No JDK method takes a Guava type.
        return declaringType.startsWith("java.") ||
               typesDeclaredInFile.contains(declaringType);
    }
}
