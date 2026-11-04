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

class NoGuavaStringsIsNullOrEmptyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaStringsIsNullOrEmpty())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void isNullOrEmptyOfParameter() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Strings;

              class A {
                  String orDefault(String s) {
                      if (Strings.isNullOrEmpty(s)) {
                          return "default";
                      }
                      return s;
                  }
              }
              """,
            """
              class A {
                  String orDefault(String s) {
                      if (s == null || s.isEmpty()) {
                          return "default";
                      }
                      return s;
                  }
              }
              """
          )
        );
    }

    @Test
    void negatedCall() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Strings;

              class A {
                  boolean hasText(String s) {
                      return !Strings.isNullOrEmpty(s);
                  }
              }
              """,
            """
              class A {
                  boolean hasText(String s) {
                      return s != null && !s.isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAccessesThroughStaticImportParenthesizedInsideAnd() {
        //language=java
        rewriteRun(
          java(
            """
              import static com.google.common.base.Strings.isNullOrEmpty;

              class A {
                  String name;
                  A parent;

                  boolean unnamed() {
                      return isNullOrEmpty(this.name) && isNullOrEmpty(parent.name);
                  }
              }
              """,
            """
              class A {
                  String name;
                  A parent;

                  boolean unnamed() {
                      return (this.name == null || this.name.isEmpty()) && (parent.name == null || parent.name.isEmpty());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesArgumentsThatMayChangeBetweenReadsAlone() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Strings;

              class A {
                  volatile String shared;

                  String name() {
                      return "a";
                  }

                  boolean f(String s) {
                      return Strings.isNullOrEmpty(name()) || Strings.isNullOrEmpty(shared) || Strings.isNullOrEmpty(s);
                  }
              }
              """,
            """
              import com.google.common.base.Strings;

              class A {
                  volatile String shared;

                  String name() {
                      return "a";
                  }

                  boolean f(String s) {
                      return Strings.isNullOrEmpty(name()) || Strings.isNullOrEmpty(shared) || (s == null || s.isEmpty());
                  }
              }
              """
          )
        );
    }
}
