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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class JettyUpgradeEE10Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JettyUpgradeEE10");
    }

    @DocumentExample
    @Test
    void securityAndJettyWebSocketMoveToCoreModules() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty.ee9</groupId>
                          <artifactId>jetty-ee9-security</artifactId>
                          <version>12.0.16</version>
                      </dependency>
                      <dependency>
                          <groupId>org.eclipse.jetty.ee9.websocket</groupId>
                          <artifactId>jetty-ee9-websocket-jetty-api</artifactId>
                          <version>12.0.16</version>
                      </dependency>
                      <dependency>
                          <groupId>org.eclipse.jetty.ee9.websocket</groupId>
                          <artifactId>jetty-ee9-websocket-jetty-client</artifactId>
                          <version>12.0.16</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("ee9")
              .containsPattern("<groupId>org.eclipse.jetty</groupId>\\s*<artifactId>jetty-security</artifactId>\\s*<version>12\\.0\\.\\d+</version>")
              .containsPattern("<groupId>org.eclipse.jetty.websocket</groupId>\\s*<artifactId>jetty-websocket-jetty-api</artifactId>\\s*<version>12\\.0\\.\\d+</version>")
              .containsPattern("<groupId>org.eclipse.jetty.websocket</groupId>\\s*<artifactId>jetty-websocket-jetty-client</artifactId>\\s*<version>12\\.0\\.\\d+</version>")
              .actual())
          )
        );
    }
}
