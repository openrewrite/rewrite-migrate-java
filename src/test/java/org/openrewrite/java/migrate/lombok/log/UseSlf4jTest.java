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
package org.openrewrite.java.migrate.lombok.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseSlf4jTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseSlf4j(null))
          .parser(JavaParser.fromJavaVersion()
            .classpath("slf4j-api"));
    }

    @DocumentExample
    @Test
    void replaceSlf4j() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jAndRenameFieldUsages() {
        rewriteRun(
          // language=java
          java(
            """
              class A {
                  private static final org.slf4j.Logger renamed = org.slf4j.LoggerFactory.getLogger(A.class);

                  void test() {
                      renamed.info("test");
                  }
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {

                  void test() {
                      log.info("test");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jImportedType() {
        rewriteRun(// language=java
          java(
            """
              import org.slf4j.Logger;
              class A {
                  private static final Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jImportedLogger() {
        rewriteRun(// language=java
          java(
            """
              import org.slf4j.LoggerFactory;
              class A {
                  private static final org.slf4j.Logger log = LoggerFactory.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceSlf4jStaticallyImportedLogger() {
        rewriteRun(// language=java
          java(
            """
              import static org.slf4j.LoggerFactory.*;
              class A {
                  private static final org.slf4j.Logger log = getLogger(A.class);
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void shouldNotReplaceWhenFieldNameDiffersFromSpecifiedName() {
        rewriteRun(
          spec -> spec.recipe(new UseSlf4j("log"))
            .parser(JavaParser.fromJavaVersion()
              .classpath("slf4j-api")),

          // language=java
          java(
            """
              class A {
                  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "public static final",
      "protected static final",
      "static final",
      "private final",
      "private static",
    })
    void shouldNotReplaceWhenNotPrivateStaticFinal(String modifiers) {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
              .classpath("slf4j-api")),

          // language=java
          java(
            """
              class A {
                  %s org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """.formatted(modifiers)
          )
        );
    }

    @Test
    void replaceSlf4jWithPackage() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.yourapp;
              class A {
                  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(A.class);
              }
              """,
            """
              package com.yourorg.yourapp;

              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {
              }
              """
          )
        );
    }

    @Test
    void leaveLoggerWhoseUseSitesHaveAnotherLog() {
        //language=java
        rewriteRun(
          java(
            """
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class A {
                  private static final Logger LOGGER = LoggerFactory.getLogger(A.class);
                  void m(String log) {
                      LOGGER.info(log);
                  }
              }
              """
          )
        );
    }

    @Test
    void renameLoggerWhereAnOuterLogIsNotRead() {
        //language=java
        rewriteRun(
          java(
            """
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class A {
                  private static final Logger log = LoggerFactory.getLogger(A.class);

                  static class B {
                      private static final Logger LOGGER = LoggerFactory.getLogger(B.class);
                      void m() {
                          LOGGER.info("m");
                      }
                  }
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;

              @Slf4j
              class A {

                  @Slf4j
                  static class B {
                      void m() {
                          log.info("m");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveNestedLoggerWhoseLogWouldHideTheOuterRenamedLogger() {
        //language=java
        rewriteRun(
          java(
            """
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              class A {
                  private static final Logger LOGGER = LoggerFactory.getLogger(A.class);

                  static class B {
                      private static final Logger BLOG = LoggerFactory.getLogger(B.class);
                      void m() {
                          LOGGER.info("a");
                          BLOG.info("b");
                      }
                  }
              }
              """,
            """
              import lombok.extern.slf4j.Slf4j;
              import org.slf4j.Logger;
              import org.slf4j.LoggerFactory;

              @Slf4j
              class A {

                  static class B {
                      private static final Logger BLOG = LoggerFactory.getLogger(B.class);
                      void m() {
                          log.info("a");
                          BLOG.info("b");
                      }
                  }
              }
              """
          )
        );
    }
}
