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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JerseyLoggingFilterToFeatureTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JerseyLoggingFilterToFeature())
          .parser(JavaParser.fromJavaVersion().dependsOn("""
            package org.glassfish.jersey.filter;
            public class LoggingFilter {
                public LoggingFilter(java.util.logging.Logger logger, boolean entities) {}
            }
            """, """
            package javax.ws.rs.core;
            public interface Configurable<T> { T register(Object component); }
            """));
    }

    @Test
    void preservesLoggerAndEntityLoggingFlag() {
        rewriteRun(
          java(
            """
              import org.glassfish.jersey.filter.LoggingFilter;
              import java.util.logging.Logger;
              import javax.ws.rs.core.Configurable;

              class Test {
                  Object filter(Configurable<?> config, Logger logger, boolean entities) {
                      return config.register(new LoggingFilter(logger, entities));
                  }
              }
              """,
            """
              import java.util.logging.Level;
              import java.util.logging.Logger;

              import javax.ws.rs.core.Configurable;

              import org.glassfish.jersey.logging.LoggingFeature;

              class Test {
                  Object filter(Configurable<?> config, Logger logger, boolean entities) {
                      return config.register(new LoggingFeature(logger, Level.INFO, entities ? LoggingFeature.Verbosity.PAYLOAD_ANY : LoggingFeature.Verbosity.HEADERS_ONLY, 8192));
                  }
              }
              """
          )
        );
    }
    @Test
    void leavesTypedDeclarationsAndReturnValuesForManualMigration() {
        rewriteRun(
          java(
            """
              import org.glassfish.jersey.filter.LoggingFilter;
              import java.util.logging.Logger;

              class Test {
                  LoggingFilter filter(Logger logger, boolean entities) {
                      LoggingFilter filter = new LoggingFilter(logger, entities);
                      return filter;
                  }
                  Object untyped(Logger logger) {
                      return new LoggingFilter(logger, true);
                  }
                  Object unrelated(Registrar registrar, Logger logger) {
                      return registrar.register(new LoggingFilter(logger, true));
                  }
              }
              class Registrar {
                  Object register(Object component) { return component; }
              }
              """
          )
        );
    }

}
