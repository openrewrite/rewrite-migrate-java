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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class JettyUpgradeEE9Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JettyUpgradeEE9");
    }

    @Test
    void alignsServerServletAndJavaVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <properties>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty</groupId>
                          <artifactId>jetty-server</artifactId>
                          <version>9.4.58.v20250814</version>
                      </dependency>
                      <dependency>
                          <groupId>org.eclipse.jetty</groupId>
                          <artifactId>jetty-servlet</artifactId>
                          <version>9.4.58.v20250814</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("9.4.58.v20250814", ">11</maven.compiler.")
              .contains("jetty-ee9-servlet", ">17</maven.compiler.")
              .containsPattern("<version>12\\.0\\.\\d+</version>")
              .actual())
          )
        );
    }

    @Test
    void relocatesServletTypes() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package org.eclipse.jetty.servlet; public class ServletContextHandler {}")),
          java(
            """
              import org.eclipse.jetty.servlet.ServletContextHandler;
              class ServerTest { ServletContextHandler context; }
              """,
            """
              import org.eclipse.jetty.ee9.servlet.ServletContextHandler;
              class ServerTest { ServletContextHandler context; }
              """
          )
        );
    }

    @Test
    void castsLegacyConnectorPortAccess() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package org.eclipse.jetty.server; public interface Connector { int getPort(); }")),
          java(
            """
              import org.eclipse.jetty.server.Connector;
              class ServerTest { int port(Connector connector) { return connector.getPort(); } }
              """,
            """
              import org.eclipse.jetty.server.Connector;
              import org.eclipse.jetty.server.NetworkConnector;

              class ServerTest { int port(Connector connector) { return ((NetworkConnector) connector).getPort(); } }
              """
          )
        );
    }

    @Test
    void retainsSharedJettySecurityTypes() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package org.eclipse.jetty.security; public class HashLoginService {}")),
          java(
            """
              import org.eclipse.jetty.security.HashLoginService;
              class ServerTest { HashLoginService login; }
              """
          )
        );
    }

    @Test
    void doesNotChangeJavaVersionWithoutJetty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <properties>
                      <maven.compiler.source>8</maven.compiler.source>
                      <maven.compiler.target>8</maven.compiler.target>
                  </properties>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotDowngradeNewerJetty() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <properties>
                      <maven.compiler.source>21</maven.compiler.source>
                      <maven.compiler.target>21</maven.compiler.target>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty</groupId>
                          <artifactId>jetty-server</artifactId>
                          <version>12.1.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
    @Test
    void leavesStandaloneJettyCoreAlone() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <properties>
                      <maven.compiler.source>8</maven.compiler.source>
                      <maven.compiler.target>8</maven.compiler.target>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty</groupId>
                          <artifactId>jetty-util</artifactId>
                          <version>9.4.58.v20250814</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
    @Test
    void leavesTransitiveServletDependenciesAlone() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <properties>
                      <maven.compiler.source>8</maven.compiler.source>
                      <maven.compiler.target>8</maven.compiler.target>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty</groupId>
                          <artifactId>jetty-deploy</artifactId>
                          <version>9.4.58.v20250814</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

}
