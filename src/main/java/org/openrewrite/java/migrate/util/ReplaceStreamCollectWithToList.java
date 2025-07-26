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
package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceStreamCollectWithToList extends Recipe {

    private static final MethodMatcher STREAM_COLLECT = new MethodMatcher("java.util.stream.Stream collect(java.util.stream.Collector)");
    private static final MethodMatcher COLLECT_TO_UNMODIFIABLE_LIST = new MethodMatcher("java.util.stream.Collectors toUnmodifiableList()");
    private static final MethodMatcher COLLECT_TO_LIST = new MethodMatcher("java.util.stream.Collectors toList()");

    @Option(displayName = "Convert mutable `Collectors.toList()` to immutable",
            description = "Also replace `Stream.collect(Collectors.toList())` with `Stream.toList()`. " +
                          "*BEWARE*: Attempts to modify the returned list, result in an `UnsupportedOperationException`!",
            required = false)
    @Nullable
    Boolean convertToList;

    @Override
    public String getDisplayName() {
        return "Replace `Stream.collect(Collectors.toUnmodifiableList())` with `Stream.toList()`";
    }

    @Override
    public String getDescription() {
        return "Replace `Stream.collect(Collectors.toUnmodifiableList())` with Java 16+ `Stream.toList()`. " +
               "Also replaces `Stream.collect(Collectors.toList())` if `convertToList` is set to `true`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Collections.singletonList("RSPEC-S6204"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(16),
                        new UsesMethod<>(STREAM_COLLECT),
                        Preconditions.or(
                                new UsesMethod<>(COLLECT_TO_UNMODIFIABLE_LIST),
                                new UsesMethod<>(COLLECT_TO_LIST))
                ),
                new ReplaceCollectorToListVisitor(Boolean.TRUE.equals(convertToList)));
    }

    @RequiredArgsConstructor
    private static final class ReplaceCollectorToListVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final boolean convertToList;

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation result = super.visitMethodInvocation(method, ctx);
            if (!STREAM_COLLECT.matches(method)) {
                return result;
            }
            Expression command = method.getArguments().get(0);
            if (COLLECT_TO_UNMODIFIABLE_LIST.matches(command) ||
                    convertToList && COLLECT_TO_LIST.matches(command)) {

                // Check if the transformation would result in incompatible types
                if (!isTypeCompatible(result)) {
                    return result;
                }

                maybeRemoveImport("java.util.stream.Collectors");
                J.MethodInvocation toList = JavaTemplate.apply("#{any(java.util.stream.Stream)}.toList()",
                        updateCursor(result), result.getCoordinates().replace(), result.getSelect());
                return toList.getPadding().withSelect(result.getPadding().getSelect());
            }
            return result;
        }

        private boolean isTypeCompatible(J.MethodInvocation method) {
            // Get the type of the collect method invocation (the resulting List type)
            JavaType methodType = method.getType();
            if (!(methodType instanceof JavaType.Parameterized)) {
                return true; // Conservative: allow transformation if we can't determine the type
            }

            // Get the stream type to determine what toList() would return
            Expression select = method.getSelect();
            if (select == null || select.getType() == null) {
                return true; // Conservative: allow transformation if we can't determine the stream type
            }

            JavaType streamType = select.getType();
            if (!(streamType instanceof JavaType.Parameterized)) {
                return true; // Conservative: allow transformation if stream type is not parameterized
            }

            JavaType streamElementType = ((JavaType.Parameterized) streamType).getTypeParameters().get(0);
            JavaType expectedElementType = ((JavaType.Parameterized) methodType).getTypeParameters().get(0);

            // Check if the stream element type and expected list element type are exactly the same
            // If they differ (e.g., Stream<Integer> but List<Number>), don't transform
            return TypeUtils.isOfType(streamElementType, expectedElementType);
        }
    }
}
