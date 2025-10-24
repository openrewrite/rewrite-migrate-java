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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaPredicatesAndOrTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaPredicatesAndOr())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replacePredicatesAnd() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> combined = Predicates.and(isNotNull, isNotEmpty);
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> combined = isNotNull.and(isNotEmpty);
              }
              """
          )
        );
    }

    @Test
    void replacePredicatesOr() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> combined = Predicates.or(isNotNull, isNotEmpty);
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> combined = isNotNull.or(isNotEmpty);
              }
              """
          )
        );
    }

    @Test
    void replacePredicatesAndWithLambdas() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isLong = s -> s.length() > 5;
                  Predicate<String> combined = Predicates.and(isNotNull, isLong);
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isLong = s -> s.length() > 5;
                  Predicate<String> combined = isNotNull.and(isLong);
              }
              """
          )
        );
    }

    @Test
    void replacePredicatesAndNestedCalls() {
        //language=java
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<Integer> isPositive = n -> n > 0;
                  Predicate<Integer> isEven = n -> n % 2 == 0;
                  Predicate<Integer> isLessThan100 = n -> n < 100;
                  Predicate<Integer> combined = Predicates.and(isPositive, Predicates.and(isEven, isLessThan100));
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<Integer> isPositive = n -> n > 0;
                  Predicate<Integer> isEven = n -> n % 2 == 0;
                  Predicate<Integer> isLessThan100 = n -> n < 100;
                  Predicate<Integer> combined = isPositive.and(isEven.and(isLessThan100));
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/893")
    @Test
    void replacePredicatesAndWithMoreThanTwoParameters() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> containsA = s -> s.contains("A");
                  Predicate<String> combined = Predicates.and(isNotNull, isNotEmpty, containsA);
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> containsA = s -> s.contains("A");
                  Predicate<String> combined = isNotNull.and(isNotEmpty).and(containsA);
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/893")
    @Test
    void replacePredicatesOrWithMoreThanTwoParameters() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> containsA = s -> s.contains("A");
                  Predicate<String> combined = Predicates.or(isNotNull, isNotEmpty, containsA);
              }
              """,
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> isNotNull = s -> s != null;
                  Predicate<String> isNotEmpty = s -> !s.isEmpty();
                  Predicate<String> containsA = s -> s.contains("A");
                  Predicate<String> combined = isNotNull.or(isNotEmpty).or(containsA);
              }
              """
          )
        );
    }
}
