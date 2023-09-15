/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.util;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.MethodInvocation;
import org.openrewrite.java.tree.JRightPadded;

@Value
public class ReplaceCollectWithStreamToList extends Recipe {

  private static final MethodMatcher STREAM_COLLECT = new MethodMatcher("java.util.stream.Stream collect(java.util.stream.Collector)");
  private static final MethodMatcher COLLECT_TO_UNMODIFIABLE_LIST = new MethodMatcher("java.util.stream.Collectors toUnmodifiableList()");
  private static final MethodMatcher COLLECT_TO_LIST = new MethodMatcher("java.util.stream.Collectors toList()");

    @Option(displayName = "Should the recipe also apply to mutable toList?",
        description = "Also replace Java 11 `Stream.collect(Collectors.toList())` with Java 16 `Stream.toList()`. BEWARE: Enabling this potentially causes exceptions at runtime!",
        required = false)
    @Nullable
    Boolean includeMutable;

    @Override
    public String getDisplayName() {
        return "Replace Stream.collect(Collectors.toUnmodifiableList()) with Stream.toList()";
    }

    @Override
    public String getDescription() {
        return "Replace Java 11 `Stream.collect(Collectors.toUnmodifiableList())` with Java 16 `Stream.toList()`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-6204"));
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
        new JavaVisitor<ExecutionContext>() {

            private final JavaTemplate template = JavaTemplate
                    .builder("#{any(java.util.stream.Stream)}.toList()")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation result = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (STREAM_COLLECT.matches(method)) {
                    Expression command = method.getArguments().get(0);
                    if (COLLECT_TO_UNMODIFIABLE_LIST.matches(command)){
                        result = replaceCollector(result);
                    } else if (COLLECT_TO_LIST.matches(command) && Boolean.TRUE.equals(includeMutable)) {
                        result = replaceCollector(result);
                    }
                }
                return result;
            }

            @NotNull
            private J.MethodInvocation replaceCollector(MethodInvocation result) {
                JRightPadded<Expression> select = result.getPadding().getSelect();
                result = template.apply(updateCursor(result), result.getCoordinates().replace(), result.getSelect());
                result = result.getPadding().withSelect(select);
                maybeRemoveImport("java.util.stream.Collectors");
                return result;
            }
        });
    }
}
