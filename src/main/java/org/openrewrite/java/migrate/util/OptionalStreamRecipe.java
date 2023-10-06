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
package org.openrewrite.java.migrate.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OptionalStreamRecipe extends Recipe {
  @Override
  public String getDisplayName() {
    return "Stream<Optional> idiom recipe";
  }

  @Override
  public String getDescription() {
    return "Migrate Java 8 optional stream idiom .filter(Optional::isPresent).map(Optional::get) to Java 11 .flatMap(Optional::stream).";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new OptionalStreamVisitor();
  }

  private static class OptionalStreamVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final MethodMatcher mapMatcher = new MethodMatcher("java.util.stream.Stream map(..)");
    private static final MethodMatcher filterMatcher = new MethodMatcher("java.util.stream.Stream filter(..)");
    private static final MethodMatcher optionalGetMatcher = new MethodMatcher("java.util.Optional get()");
    private static final MethodMatcher optionalIsPresentMatcher = new MethodMatcher("java.util.Optional isPresent()");
    private static final JavaTemplate template =
        JavaTemplate.builder("#{any(java.util.stream.Stream)}.flatMap(Optional::stream)")
            .imports("java.util.Optional")
            .build();

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, ExecutionContext ctx) {
      return getInvocation(invocation, mapMatcher)
          .flatMap(mapInvocation -> getInvocation(mapInvocation.getSelect(), filterMatcher)
          .flatMap(filterInvocation -> getReference(mapInvocation.getArguments().get(0), optionalGetMatcher)
          .flatMap(optionalGetReference -> getReference(filterInvocation.getArguments().get(0), optionalIsPresentMatcher))
          .flatMap(optionalIsPresentReference -> {
            final JRightPadded<Expression> filterSelect = filterInvocation.getPadding().getSelect();
            final JRightPadded<Expression> mapSelect = mapInvocation.getPadding().getSelect();
            final JavaType.Method mapInvocationType = mapInvocation.getMethodType();
            if (filterSelect != null && mapSelect != null && mapInvocationType != null) {
              final Space flatMapComments = getFlatMapComments(mapSelect, filterSelect);
              J.MethodInvocation flatMapInvocation = template
                  .apply(updateCursor(mapInvocation), mapInvocation.getCoordinates().replace(), filterInvocation.getSelect());
              flatMapInvocation = flatMapInvocation.getPadding().withSelect(filterSelect.withAfter(flatMapComments))
                  .withMethodType(mapInvocationType.withName("flatMap"))
                  .withPrefix(mapInvocation.getPrefix());
              return Optional.of(super.visitMethodInvocation(flatMapInvocation, ctx));
            }
            return Optional.empty();
          })))
          .orElse(super.visitMethodInvocation(invocation, ctx));
    }

    private Optional<J.MemberReference> getReference(@Nullable Expression expr, MethodMatcher matcher) {
      if (expr instanceof  J.MemberReference) {
        J.MemberReference reference = (J.MemberReference) expr;
        if (matcher.matches(reference)) {
          return Optional.of(reference);
        }
      }
      return Optional.empty();
    }

    private Optional<J.MethodInvocation> getInvocation(@Nullable Expression expr, MethodMatcher matcher) {
      if (expr instanceof  J.MethodInvocation) {
        J.MethodInvocation invocation = (J.MethodInvocation) expr;
        if (matcher.matches(invocation)) {
          return Optional.of(invocation);
        }
      }
      return Optional.empty();
    }

    @NotNull
    private static Space getFlatMapComments(JRightPadded<Expression> mapSelect, JRightPadded<Expression> filterSelect) {
      final List<Comment> commentsBetweenMethods = mapSelect.getAfter().getComments().stream()
          .map(OptionalStreamVisitor::prefixComment)
          .collect(Collectors.toList());
      final List<Comment> commentsBefore = filterSelect.getAfter().getComments();
      final List<Comment> comments = new ArrayList<>();
      comments.addAll(commentsBefore);
      comments.addAll(commentsBetweenMethods);

      return filterSelect.getAfter().withComments(comments);
    }

    private static Comment prefixComment(Comment comment) {
      final String refactoringInfo = " TODO this block was automatically refactor, check if the comment is still relevant: ";
      if (comment instanceof TextComment) {
        TextComment textComment = (TextComment) comment;
        return textComment.withText(refactoringInfo + textComment.getText().trim()
            + (textComment.isMultiline() ? " " : ""));
      } else {
        return comment;
      }
    }
  }
}