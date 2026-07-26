/*
 * Copyright 2026 the original author or authors.
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

class NoGuavaUnsignedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.NoGuava")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void unsignedInts() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.UnsignedInts;

              class Test {
                  int compare = UnsignedInts.compare(1, 2);
                  int divide = UnsignedInts.divide(6, 3);
                  int remainder = UnsignedInts.remainder(7, 3);
                  int parse = UnsignedInts.parseUnsignedInt("42");
              }
              """,
            """
              class Test {
                  int compare = Integer.compareUnsigned(1, 2);
                  int divide = Integer.divideUnsigned(6, 3);
                  int remainder = Integer.remainderUnsigned(7, 3);
                  int parse = Integer.parseUnsignedInt("42");
              }
              """
          )
        );
    }

    @Test
    void radixOverloads() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.UnsignedInts;
              import com.google.common.primitives.UnsignedLongs;

              class Test {
                  int i = UnsignedInts.parseUnsignedInt("ff", 16);
                  long l = UnsignedLongs.parseUnsignedLong("ff", 16);
              }
              """,
            """
              class Test {
                  int i = Integer.parseUnsignedInt("ff", 16);
                  long l = Long.parseUnsignedLong("ff", 16);
              }
              """
          )
        );
    }

    @Test
    void unsignedLongs() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.UnsignedLongs;

              class Test {
                  int compare = UnsignedLongs.compare(1L, 2L);
                  long divide = UnsignedLongs.divide(6L, 3L);
                  long remainder = UnsignedLongs.remainder(7L, 3L);
                  long parse = UnsignedLongs.parseUnsignedLong("42");
              }
              """,
            """
              class Test {
                  int compare = Long.compareUnsigned(1L, 2L);
                  long divide = Long.divideUnsigned(6L, 3L);
                  long remainder = Long.remainderUnsigned(7L, 3L);
                  long parse = Long.parseUnsignedLong("42");
              }
              """
          )
        );
    }
}
