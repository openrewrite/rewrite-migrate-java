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
package org.openrewrite.java.migrate.joda;

import org.junit.jupiter.api.Test;
import org.openrewrite.Cursor;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JodaTimeFlowSpecTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(toRecipe(() -> new JavaIsoVisitor<>() {
              Map<Expression, List<J.Identifier>> exprVarBindings = new HashMap<>();

              @Override
              public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                  if (variable.getInitializer() == null) {
                      return super.visitVariable(variable, ctx);
                  }
                  updateSinks(variable.getInitializer(), variable.getName());
                  return super.visitVariable(variable, ctx);
              }

              @Override
              public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                  if (!(assignment.getVariable() instanceof J.Identifier)) {
                      return super.visitAssignment(assignment, ctx);
                  }
                  updateSinks(assignment.getAssignment(), (J.Identifier) assignment.getVariable());
                  return super.visitAssignment(assignment, ctx);
              }

              @Override
              public Expression visitExpression(Expression expression, ExecutionContext ctx) {
                  List<J.Identifier> identifiers = exprVarBindings.get(expression);
                  if (identifiers == null || identifiers.isEmpty()) {
                      return expression;
                  }
                  String desc = identifiers.stream().map(J.Identifier::getSimpleName).collect(Collectors.joining(", "));
                  return SearchResult.found(expression, desc);
              }

              private void updateSinks(Expression expr, J.Identifier identifier) {
                  Cursor cursor = new Cursor(getCursor(), expr);
                  Dataflow.startingAt(cursor).findSinks(new JodaTimeFlowSpec())
                    .foreachDoEffect(sinkFlow -> {
                        for (Expression sink : sinkFlow.getExpressionSinks()) {
                            exprVarBindings.computeIfAbsent(sink, e -> new ArrayList<>()).add(identifier);
                        }
                    });
              }
          }))
          .parser(JavaParser.fromJavaVersion().classpath("joda-time"));
    }

    @DocumentExample
    @Test
    void jodaTimeUsageWithVarBindings() {
        rewriteRun(
          // language=java
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Interval;
              
              class A {
                  public void foo() {
                      DateTime dateTime = new DateTime(), _dateTime = DateTime.now();
                      System.out.println(dateTime);
                      DateTime dateTimePlus2 = dateTime.plusDays(2);
                      System.out.println(dateTimePlus2);
                      dateTime = dateTime.minusDays(1);
                      _dateTime = dateTime;
                      Interval interval = new Interval(_dateTime, dateTimePlus2);
                      System.out.println(interval);
                  }
              }
              """,
            """
              import org.joda.time.DateTime;
              import org.joda.time.Interval;
              
              class A {
                  public void foo() {
                      DateTime dateTime = /*~~(dateTime)~~>*/new DateTime(), _dateTime = /*~~(_dateTime)~~>*/DateTime.now();
                      System.out.println(/*~~(dateTime)~~>*/dateTime);
                      DateTime dateTimePlus2 = /*~~(dateTimePlus2)~~>*//*~~(dateTime)~~>*/dateTime.plusDays(2);
                      System.out.println(/*~~(dateTimePlus2)~~>*/dateTimePlus2);
                      dateTime = /*~~(dateTime)~~>*//*~~(dateTime)~~>*/dateTime.minusDays(1);
                      _dateTime = /*~~(dateTime, _dateTime)~~>*/dateTime;
                      Interval interval = /*~~(interval)~~>*/new Interval(/*~~(dateTime, _dateTime)~~>*/_dateTime, /*~~(dateTimePlus2)~~>*/dateTimePlus2);
                      System.out.println(/*~~(interval)~~>*/interval);
                  }
              }
              """
          )
        );
    }
}
