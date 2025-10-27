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

class PreferJavaUtilPredicateTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
            "/META-INF/rewrite/no-guava.yml",
            "org.openrewrite.java.migrate.guava.PreferJavaUtilPredicate")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void changeTypeAndMethodName() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;

              class A {
                  public static Predicate<String> makeStringPredicate() {
                      return new Predicate<String>() {
                          @Override
                          public boolean apply(String input) {
                              return input.isEmpty();
                          }
                      };
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class A {
                  public static Predicate<String> makeStringPredicate() {
                      return new Predicate<String>() {
                          @Override
                          public boolean test(String input) {
                              return input.isEmpty();
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void predicatesNotToPredicate() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class A {
                  public static Predicate<String> notEmptyPredicate() {
                      Predicate<String> isEmpty = String::isEmpty;
                      return Predicates.not(isEmpty);
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class A {
                  public static Predicate<String> notEmptyPredicate() {
                      Predicate<String> isEmpty = String::isEmpty;
                      return Predicate.not(isEmpty);
                  }
              }
              """
          )
        );
    }

    @Test
    void predicatesEqualToToPredicateIsEqual() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class A {
                  public static Predicate<String> isHelloPredicate() {
                      return Predicates.equalTo("hello");
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class A {
                  public static Predicate<String> isHelloPredicate() {
                      return Predicate.isEqual("hello");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/899")
    @Test
    void doNotChangeWhenUsingCollectionsFilter() {
        // Collections2.filter requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Collections2;
              import java.util.Collection;

              class Test {
                  Predicate<String> notEmpty = new Predicate<String>() {
                      @Override public boolean apply(String s) {
                          return !s.isEmpty();
                      }
                  };

                  public Collection<String> filterCollection(Collection<String> input) {
                      return Collections2.filter(input, notEmpty);
                  }
              }
              """
          )
        );
    }
}
