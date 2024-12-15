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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ApplicationPathWildcardNoLongerAcceptedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.ws.rs-api-3.1.0"))
          .recipe(new ApplicationPathWildcardNoLongerAccepted());
    }

    @DocumentExample
    @Test
    void updateAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.ws.rs.ApplicationPath;
              import jakarta.ws.rs.core.Application;
              @ApplicationPath("should-flag/*")
              public class ApplicationPathWithWildcard extends Application {
              }
              """,
            """
              import jakarta.ws.rs.ApplicationPath;
              import jakarta.ws.rs.core.Application;
              @ApplicationPath("should-flag")
              public class ApplicationPathWithWildcard extends Application {
              }
              """
          )
        );
    }

    @Test
    void updateAnnotationValue() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.ws.rs.ApplicationPath;
              import jakarta.ws.rs.core.Application;
              @ApplicationPath(value="should-flag/*")
              public class ApplicationPathWithWildcard extends Application {
              }
              """,
            """
              import jakarta.ws.rs.ApplicationPath;
              import jakarta.ws.rs.core.Application;
              @ApplicationPath(value="should-flag")
              public class ApplicationPathWithWildcard extends Application {
              }
              """
          )
        );
    }

    @Test
    void noUpdateAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.ws.rs.ApplicationPath;
              import jakarta.ws.rs.core.Application;
              @ApplicationPath("should-not-flag*")
              public class TestAnnotate extends Application {
              }
              """
          )
        );
    }
}
