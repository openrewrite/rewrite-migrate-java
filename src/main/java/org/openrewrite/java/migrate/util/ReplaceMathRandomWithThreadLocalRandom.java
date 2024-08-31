/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceMathRandomWithThreadLocalRandom extends Recipe {

  private static final MethodMatcher MATH_RANDOM = new MethodMatcher("java.lang.Math random()");

  @Override
  public String getDisplayName() {
    return "Replace `java.lang.Math random()` with `ThreadLocalRandom nextDouble()`";
  }

  @Override
  public String getDescription() {
    return "Replace `java.lang.Math random()` with `ThreadLocalRandom nextDouble()`.";
  }

  @Override
  public Duration getEstimatedEffortPerOccurrence() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Set<String> getTags() {
    return new HashSet<>(Collections.singletonList("ThreadLocalRandom"));
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new ReplaceMathRandomWithThreadLocalRandomVisitor();
  }

  @RequiredArgsConstructor
  private static final class ReplaceMathRandomWithThreadLocalRandomVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final JavaTemplate template = JavaTemplate
        .builder("ThreadLocalRandom.current().nextDouble()")
        .imports("java.util.concurrent.ThreadLocalRandom")
        .build();

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
      J.MethodInvocation result = super.visitMethodInvocation(method, ctx);
      if (!MATH_RANDOM.matches(method)) {
        return result;
      }
      maybeRemoveImport("java.lang.Math");
      maybeAddImport("java.util.concurrent.ThreadLocalRandom");
      J.MethodInvocation nextDoubleInvocation = template.apply(updateCursor(result), result.getCoordinates().replace());
      return nextDoubleInvocation;
    }
  }
}
