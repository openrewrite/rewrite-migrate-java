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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxEjbToJakartaEjbTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxEjbToJakartaEjb"));
    }

    @DocumentExample
    @Test
    void upgradeMavenEjbPlugin() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>my-ejb</artifactId>
                <version>1.0</version>
                <dependencies>
                  <dependency>
                    <groupId>javax.ejb</groupId>
                    <artifactId>javax.ejb-api</artifactId>
                    <version>3.2.2</version>
                    <scope>provided</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-ejb-plugin</artifactId>
                      <version>2.5</version>
                      <configuration>
                        <ejbVersion>3.0</ejbVersion>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pom -> {
                // javax.ejb-api replaced by jakarta.ejb-api at 4.x
                assertThat(pom)
                  .doesNotContain("javax.ejb")
                  .contains("jakarta.ejb-api")
                  .containsPattern("<version>4\\.");
                // maven-ejb-plugin upgraded to 3.2.x or higher
                assertThat(pom).containsPattern(
                  "maven-ejb-plugin</artifactId>\\s+<version>3\\.[2-9]\\.");
                // ejbVersion updated to 4.0
                assertThat(pom).contains("<ejbVersion>4.0</ejbVersion>");
                return pom;
            })
          )
        );
    }
}
