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
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class ReplaceJakartaJwsWithJakartaXmlWsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.ReplaceJakartaJwsWithJakartaXmlWs");
    }

    @DocumentExample
    @Test
    void replacesJakartaJwsApiInPlaceWithJakartaXmlWsApi() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.jws</groupId>
                          <artifactId>jakarta.jws-api</artifactId>
                          <version>3.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("jakarta.jws")
              .contains("jakarta.xml.ws-api")
              // Verify the dependency was upgraded to a 4.x version, without pinning the exact patch
              .contains("<version>4")
              .actual())
          )
        );
    }

    @Test
    void avoidDuplicateWsApi() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.jws</groupId>
                          <artifactId>jakarta.jws-api</artifactId>
                          <version>3.0.0</version>
                      </dependency>
                      <dependency>
                          <groupId>jakarta.xml.ws</groupId>
                          <artifactId>jakarta.xml.ws-api</artifactId>
                          <version>3.0.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("jakarta.jws")
              // Verify only one entry of `jakarta.xml.ws-api` exists
              .containsOnlyOnce("jakarta.xml.ws-api")
              // Verify the dependency was upgraded to a 4.x version, without pinning the exact patch
              .contains("<version>4")
              .actual())
          )
        );
    }

    @Test
    void addsJakartaXmlWsApiWhenJwsIsOnlyTransitive() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.jws-api")),
          mavenProject(
            "demo",
            //language=java
            srcMainJava(
              java(
                """
                  import jakarta.jws.WebService;

                  @WebService
                  public class HelloService {
                      public String sayHello(String name) {
                          return "Hello, " + name;
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .containsOnlyOnce("jakarta.xml.ws-api")
                .contains("<version>4")
                .actual())
            )
          )
        );
    }

    @Test
    void noChangeWhenJakartaJwsApiNotPresent() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.ws</groupId>
                          <artifactId>jakarta.xml.ws-api</artifactId>
                          <version>4.0.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
