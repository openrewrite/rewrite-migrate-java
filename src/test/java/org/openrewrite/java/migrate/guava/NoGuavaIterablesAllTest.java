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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaIterablesAllTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaIterablesAll())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceIterablesAll() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Collection;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterables;

              class Test {
                  boolean test(Collection<String> collection, Predicate<String> aPredicate) {
                      return Iterables.all(collection, aPredicate);
                  }
              }
              """,
            """
              import java.util.Collection;

              import com.google.common.base.Predicate;

              class Test {
                  boolean test(Collection<String> collection, Predicate<String> aPredicate) {
                      return collection.stream().allMatch(aPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void iterablesAllWithIterable() {
        //language=java
        rewriteRun(
          java(
            """
              import java.lang.Iterable;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterables;

              class Test {
                  boolean test(Iterable<String> iterable, Predicate<String> aPredicate) {
                      return Iterables.all(iterable, aPredicate);
                  }
              }
              """
          )
        );
    }
}
