/*
 * Copyright 2022 the original author or authors.
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

package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class StringFormatted extends Recipe {

  private static final MethodMatcher STRING_FORMAT =
      new MethodMatcher("java.lang.String format(String, ..)");

  @Override
  public String getDisplayName() {
    return "Prefer `String.formatted(Object...)`";
  }

  @Override
  public String getDescription() {
    return "Prefer `String.formatted(Object...)` over `String.format(String, Object...)` in Java 17 or higher.";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return Preconditions.check(
        Preconditions.and(new UsesJavaVersion<>(17), new UsesMethod<>(STRING_FORMAT)),
        new StringFormattedVisitor());
  }

  private static class StringFormattedVisitor extends JavaVisitor<ExecutionContext> {
    @Override
    public J visitMethodInvocation(J.MethodInvocation m, ExecutionContext ctx) {
      m = (J.MethodInvocation) super.visitMethodInvocation(m, ctx);
      if (!STRING_FORMAT.matches(m) || m.getMethodType() == null) {
        return m;
      }
      m = makeFirstArgumentPrefixAsEmpty(m);
      List<Expression> arguments = m.getArguments();

      maybeRemoveImport("java.lang.String.format");
      J.MethodInvocation mi = m.withName(m.getName().withSimpleName("formatted"));
      JavaType.Method formatted = m.getMethodType().getDeclaringType().getMethods().stream()
          .filter(it -> it.getName().equals("formatted"))
          .findAny()
          .orElse(null);
      mi = mi.withMethodType(formatted);
      if (mi.getName().getType() != null) {
        mi = mi.withName(mi.getName().withType(mi.getMethodType()));
      }
      boolean wrapperNotNeeded = wrapperNotNeeded(arguments.get(0));
      Expression select = wrapperNotNeeded ? arguments.get(0) :
          new J.Parentheses<>(randomId(), Space.EMPTY, Markers.EMPTY,
              JRightPadded.build(arguments.get(0)));
      mi = mi.withSelect(select);
      mi = mi.withArguments(arguments.subList(1, arguments.size()));
      if (mi.getArguments().isEmpty()) {
        // To store spaces between the parenthesis of a method invocation argument list
        // Ensures formatting recipes chained together with this one will still work as expected
        mi = mi.withArguments(singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)));
      }

      return maybeAutoFormat(m, mi, ctx);
    }

    private static J.MethodInvocation makeFirstArgumentPrefixAsEmpty(J.MethodInvocation m) {
      List<Expression> newArgs = m.getArguments();
      newArgs.set(0, m.getArguments().get(0).withPrefix(Space.EMPTY));
      return m.withArguments(newArgs);
    }

    private static boolean wrapperNotNeeded(Expression expression) {
      return expression instanceof J.Identifier
          || expression instanceof J.Literal
          || expression instanceof J.MethodInvocation
          || expression instanceof J.FieldAccess;
    }
  }

  @Override
  public Duration getEstimatedEffortPerOccurrence() {
    return Duration.ofMinutes(1);
  }
}
