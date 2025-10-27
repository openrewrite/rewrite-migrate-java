/*
 * Copyright 2025 the original author or authors.
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

class NoGuavaPredicatesEqualToTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.NoGuava")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
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
                      return Predicate.<String>isEqual("hello");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/903")
    @Test
    void inlinedPredicatesEqualToToPredicateIsEqual() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicates;
              import com.google.common.base.Predicate;

              import java.util.Collection;

              class Test {
                  public static void test(Collection<String> aCollection, Predicate<Collection<String>> anotherPredicate) {
                      Predicate<Collection<String>> combined = Predicates.and(Predicates.equalTo(aCollection), anotherPredicate);
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.function.Predicate;

              class Test {
                  public static void test(Collection<String> aCollection, Predicate<Collection<String>> anotherPredicate) {
                      Predicate<Collection<String>> combined = Predicate.<Collection<String>>isEqual(aCollection).and(anotherPredicate);
                  }
              }
              """
          )
        );
    }
}
