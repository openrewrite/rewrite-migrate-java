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

class ConvertAnyLogTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("io.github.timoa.lombok.log.ConvertAnyLog")
          .parser(JavaParser.fromJavaVersion()
            .classpath("slf4j-api", "log4j-api", "jboss-logging", "commons-logging"));
    }

    @DocumentExample
    @Test
    void replaceAllLoggers() {
        rewriteRun(
          // language=java
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
          ),
          // language=java
          java(
            """
              import org.apache.logging.log4j.Logger;
              import org.apache.logging.log4j.LogManager;
              class B {
                  private static final Logger log = LogManager.getLogger(B.class);
              }
              """,
            """
              import lombok.extern.log4j.Log4j2;

              @Log4j2
              class B {
              }
              """
          ),
          // language=java
          java(
            """
              import java.util.logging.Logger;
              class C {
                  private static final Logger log = Logger.getLogger(C.class.getName());
              }
              """,
            """
              import lombok.extern.java.Log;

              @Log
              class C {
              }
              """
          ),
          // language=java
          java(
            """
              import org.jboss.logging.Logger;
              class D {
                  private static final Logger log = Logger.getLogger(D.class);
              }
              """,
            """
              import lombok.extern.jbosslog.JBossLog;

              @JBossLog
              class D {
              }
              """
          ),
          // language=java
          java(
            """
              import org.apache.commons.logging.Log;
              import org.apache.commons.logging.LogFactory;
              class E {
                  private static final Log log = LogFactory.getLog(E.class);
              }
              """,
            """
              import lombok.extern.apachecommons.CommonsLog;

              @CommonsLog
              class E {
              }
              """
          )
        );
    }

    @Test
    void allInOne() {
        rewriteRun(
          // language=java
          java(
            """
              import org.apache.logging.log4j.LogManager;
              import java.util.logging.Logger;
              import org.apache.commons.logging.Log;
              import org.apache.commons.logging.LogFactory;

              class A {
                  private static final org.slf4j.Logger log1 = org.slf4j.LoggerFactory.getLogger(A.class);
                  private static final org.apache.logging.log4j.Logger log2 = org.apache.logging.log4j.LogManager.getLogger(A.class);
                  private static final Logger log3 = Logger.getLogger(A.class.getName());
                  private static final org.jboss.logging.Logger log4 = org.jboss.logging.Logger.getLogger(A.class);
                  private static final Log log5 = LogFactory.getLog(A.class);
              }
              """,
            """
              import lombok.extern.apachecommons.CommonsLog;
              import lombok.extern.java.Log;
              import lombok.extern.jbosslog.JBossLog;
              import lombok.extern.log4j.Log4j2;
              import lombok.extern.slf4j.Slf4j;

              @CommonsLog
              @JBossLog
              @Log
              @Log4j2
              @Slf4j
              class A {
              }
              """
          )
        );
    }
}
