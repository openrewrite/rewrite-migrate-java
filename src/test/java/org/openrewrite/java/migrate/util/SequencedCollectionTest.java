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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SequencedCollectionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.util")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.util.SequencedCollection"));
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
    void iteratorNextToGetFirst() {
        rewriteRun(
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
