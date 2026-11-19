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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class JettyUpgradeEE11Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JettyUpgradeEE11")
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package org.eclipse.jetty.ee10.websocket.jakarta.common.decoders;

              public class BooleanDecoder {
              }
              """,
            """
              package org.eclipse.jetty.ee10.servlet;

              public class ServletContextHandler {
              }
              """));
    }

    @DocumentExample
    @Test
    void changeDependencies() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty.ee10</groupId>
                          <artifactId>jetty-ee10-servlet</artifactId>
                          <version>12.0.16</version>
                      </dependency>
                      <dependency>
                          <groupId>org.eclipse.jetty.ee10.websocket</groupId>
                          <artifactId>jetty-ee10-websocket-jakarta-server</artifactId>
                          <version>12.0.16</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(actual ->
              assertThat(actual)
                .doesNotContain("ee10")
                .containsPattern("<groupId>org.eclipse.jetty.ee11</groupId>\\s*<artifactId>jetty-ee11-servlet</artifactId>\\s*<version>12\\.1\\.\\d+</version>")
                .containsPattern("<groupId>org.eclipse.jetty.ee11.websocket</groupId>\\s*<artifactId>jetty-ee11-websocket-jakarta-server</artifactId>\\s*<version>12\\.1\\.\\d+</version>")
                .actual())
          )
        );
    }

    @Test
    void changePackages() {
        rewriteRun(
          //language=java
          java(
            """
              import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
              import org.eclipse.jetty.ee10.websocket.jakarta.common.decoders.BooleanDecoder;

              class AppRuntimeHints {
                  Class<?>[] types = {BooleanDecoder.class, ServletContextHandler.class};
              }
              """,
            """
              import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
              import org.eclipse.jetty.ee11.websocket.jakarta.common.decoders.BooleanDecoder;

              class AppRuntimeHints {
                  Class<?>[] types = {BooleanDecoder.class, ServletContextHandler.class};
              }
              """
          )
        );
    }
}
