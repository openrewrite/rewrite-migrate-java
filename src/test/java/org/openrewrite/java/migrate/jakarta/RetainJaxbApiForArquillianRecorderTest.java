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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class RetainJaxbApiForArquillianRecorderTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RetainJaxbApiForArquillianRecorder());
    }

    @Test
    void doesNotAddWithoutJakartaJaxbApi() {
        rewriteRun(pomXml(
          """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>example</groupId>
                <artifactId>app</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>org.arquillian.extension</groupId>
                        <artifactId>arquillian-recorder-spi</artifactId>
                        <version>1.1.6.Final</version>
                    </dependency>
                </dependencies>
            </project>
            """
        ));
    }

    @Test
    void doesNotRetainForMigratedReactorDependency() {
        rewriteRun(
          mavenProject("recorder", pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.arquillian.extension</groupId>
                  <artifactId>arquillian-recorder-spi</artifactId>
                  <version>1.1.6.Final</version>
              </project>
              """
          )),
          mavenProject("app", pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.arquillian.extension</groupId>
                          <artifactId>arquillian-recorder-spi</artifactId>
                          <version>1.1.6.Final</version>
                      </dependency>
                      <dependency>
                          <groupId>jakarta.xml.bind</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>3.0.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          ))
        );
    }
}
