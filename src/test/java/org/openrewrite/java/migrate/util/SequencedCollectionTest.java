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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SequencedCollectionTest implements RewriteTest {


    /**
     * This is a copy of the java.util.SequencedCollection interface from JDK 21, to make it available on Java 17.
     */
    //language=java
    private static final String SEQUENCED_COLLECTION = """
      package java.util;
      import java.util.Collection;
      // As per https://openjdk.org/jeps/431
      public interface SequencedCollection<E> extends Collection<E> {
          // new method
          SequencedCollection<E> reversed();
          // methods promoted from Deque
          void addFirst(E e);
          void addLast(E e);
          E getFirst();
          E getLast();
          E removeFirst();
          E removeLast();
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.util")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.util.SequencedCollection"));
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
    @Test
    void iteratorNextToGetFirst() {
        rewriteRun(
          java(SEQUENCED_COLLECTION),
          //language=java
          java(
            """
              import java.util.SequencedCollection;

              class Foo {
                  void bar(SequencedCollection<String> collection) {
                      String first = collection.iterator().next();
                  }
              }
              """,
            """
              import java.util.SequencedCollection;
                            
              class Foo {
                  void bar(SequencedCollection<String> collection) {
                      String first = collection.getFirst();
                  }
              }
              """
          )
        );
    }
}
