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
package org.openrewrite.java.migrate.lombok.log;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseCommonsLogTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseCommonsLog(null))
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("commons-logging"));
    }

    @DocumentExample
    @Test
    void replaceCommonsLog() {
        rewriteRun(
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
