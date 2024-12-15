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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PreferJavaStringJoinTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new PreferJavaStringJoin())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void joinStrings() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;

              class Test {
                  String s = Joiner.on(", ").join("a", "b");
              }
              """,
            """                            
              class Test {
                  String s = String.join(", ", "a", "b");
              }
              """
          )
        );
    }

    @Test
    void joinStringArray() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;

              class Test {
                  String s = Joiner.on(", ").join(new String[] {"a"});
              }
              """,
            """                            
              class Test {
                  String s = String.join(", ", new String[] {"a"});
              }
              """
          )
        );
    }

    @Test
    void joinIterables() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;
              import java.util.Set;

              class Test {
                  String s = Joiner.on(", ").join(Set.of("a"));
              }
              """,
            """                            
              import java.util.Set;

              class Test {
                  String s = String.join(", ", Set.of("a"));
              }
              """
          )
        );
    }

    @Test
    void joinMixedCharSequences() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;

              class Test {
                  String s = Joiner.on(", ").join("a", new StringBuilder("b"));
              }
              """,
            """
              class Test {
                  String s = String.join(", ", "a", new StringBuilder("b"));
              }
              """
          )
        );
    }

    @Test
    void joinEmptyIterables() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;
              import java.util.HashSet;

              class Test {
                  String s = Joiner.on(", ").join(new HashSet<String>());
              }
              """,
            """                            
              import java.util.HashSet;

              class Test {
                  String s = String.join(", ", new HashSet<String>());
              }
              """
          )
        );
    }

    @Test
    void joinMethodOnSeparateLine() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;

              class Test {
                  String s = Joiner.on(", ")
                                   .join("a", "b");
              }
              """,
            """                            
              class Test {
                  String s = String.join(", ", "a", "b");
              }
              """
          )
        );
    }

    @Test
    void dontEditJoinersNotSupportedByString() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;
              import java.util.Set;

              class Test {
                  String s1 = Joiner.on(", ").join("a", 1);
                  String s2 = Joiner.on(", ").skipNulls().join("a", "b");
                  String s3 = Joiner.on(", ").useForNull("null").join("a", "b");
                  String s4 = Joiner.on(", ").join(Set.of("a").iterator());
                  //String s5 = Joiner.on(',').join("a");
              }
              """
          )
        );
    }

    @Test
    void dontEditJoinerInstanceCaller() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Joiner;

              class Test {
                  Joiner j = Joiner.on(", ");
                  String s1 = j.join("a", "b");
              }
              """
          )
        );
    }

}
