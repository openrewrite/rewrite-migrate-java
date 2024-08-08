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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaPrimitiveAsListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaPrimitiveAsList())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void replaceBoolean() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Booleans;
              import java.util.List;
              
              class Test {
                  List<Boolean> bools = Booleans.asList(true, false);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Boolean> bools = Arrays.asList(true, false);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceChar() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Chars;
              import java.util.List;
              
              class Test {
                  List<Character> chars = Chars.asList('a', 'b');
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Character> chars = Arrays.asList('a', 'b');
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceDouble() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Doubles;
              import java.util.List;
              
              class Test {
                  List<Double> doubles = Doubles.asList(1d, 2d);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Double> doubles = Arrays.asList(1d, 2d);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceFloat() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Floats;
              import java.util.List;
              
              class Test {
                  List<Float> floats = Floats.asList(1f, 2f);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Float> floats = Arrays.asList(1f, 2f);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceLong() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Longs;
              import java.util.List;
              
              class Test {
                  List<Long> longs = Longs.asList(1L, 2L);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Long> longs = Arrays.asList(1L, 2L);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceInt() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Ints;
              import java.util.List;
              
              class Test {
                  List<Integer> ints = Ints.asList(1, 2);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Integer> ints = Arrays.asList(1, 2);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceShort() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Shorts;
              import java.util.List;
              
              class Test {
                  List<Short> shorts = Shorts.asList((short) 1, (short) 2);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Short> shorts = Arrays.asList((short) 1, (short) 2);
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void replaceByte() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.primitives.Bytes;
              import java.util.List;
              
              class Test {
                  List<Byte> shorts = Bytes.asList((byte) 1, (byte) 2);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.List;

              class Test {
                  List<Byte> shorts = Arrays.asList((byte) 1, (byte) 2);
              }
              """
          )
        );
    }

}
