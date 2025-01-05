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
package org.openrewrite.java.migrate.lombok.log;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConvertToLogAnnotationTest implements RewriteTest {

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
    void replaceLog4j() {
        rewriteRun(
          spec -> spec.recipe(new UseLog4j2(null))
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)
              .classpath("log4j-api")),
          // language=java
          java(
            """
              import org.apache.logging.log4j.Logger;
              import org.apache.logging.log4j.LogManager;
              class A {
                  private static final Logger log = LogManager.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.log4j.Log4j2;

              @Log4j2
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceLog() {
        rewriteRun(
          spec -> spec.recipe(new UseLog(null))
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)),

          // language=java
          java(
            """
              import java.util.logging.Logger;
              class A {
                  private static final Logger log = Logger.getLogger(A.class.getName());
              }
              """,
            """
              import lombok.extern.java.Log;

              @Log
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceJbossLog() {
        rewriteRun(
          spec -> spec.recipe(new UseJBossLog(null))
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)
              .classpath("jboss-logging")),
          // language=java
          java(
            """
              import org.jboss.logging.Logger;
              class A {
                  private static final Logger log = Logger.getLogger(A.class);
              }
              """,
            """
              import lombok.extern.jbosslog.JBossLog;

              @JBossLog
              class A {
              }
              """
          )
        );
    }

    @Test
    void replaceCommonsLog() {
        rewriteRun(
          spec -> spec.recipe(new UseCommonsLog(null))
            .parser(JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)
              .classpath("commons-logging")),
          // language=java
          java(
            """
              import org.apache.commons.logging.Log;
              import org.apache.commons.logging.LogFactory;
              class A {
                  private static final Log log = LogFactory.getLog(A.class);
              }
              """,
            """
              import lombok.extern.apachecommons.CommonsLog;

              @CommonsLog
              class A {
              }
              """
          )
        );
    }

}
