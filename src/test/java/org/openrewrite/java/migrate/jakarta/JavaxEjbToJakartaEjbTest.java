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
                // javax.ejb-api replaced by jakarta.ejb-api at 4.0.x
                assertThat(pom)
                  .doesNotContain("javax.ejb")
                  .contains("jakarta.ejb-api")
                  .containsPattern("<version>4\\.0\\.");
                // maven-ejb-plugin upgraded to 3.2.1 or higher
                assertThat(pom).containsPattern(
                  "maven-ejb-plugin</artifactId>\\s+<version>3\\.2\\.[1-9]");
                // ejbVersion updated to 4.0
                return assertThat(pom).contains("<ejbVersion>4.0</ejbVersion>").actual();
            })
          )
        );
    }

    @Test
    void upgradeMavenEjbPluginWithPropertyCoupledEjbVersion() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>my-ejb</artifactId>
                <version>1.0</version>
                <properties>
                  <jee.ejb.api>3.2</jee.ejb.api>
                  <maven.ejb.version>2.5</maven.ejb.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>javax.ejb</groupId>
                    <artifactId>javax.ejb-api</artifactId>
                    <version>${jee.ejb.api}</version>
                    <scope>provided</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-ejb-plugin</artifactId>
                      <version>${maven.ejb.version}</version>
                      <configuration>
                        <ejbVersion>${jee.ejb.api}</ejbVersion>
                      </configuration>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pom -> {
                // javax.ejb-api replaced by jakarta.ejb-api, and the shared property bumped to 4.0.x
                assertThat(pom)
                  .doesNotContain("javax.ejb")
                  .contains("jakarta.ejb-api");
                assertThat(pom).containsPattern("<jee\\.ejb\\.api>4\\.0\\.");

                // maven-ejb-plugin version property updated to 3.2.1 or higher
                // (UpgradePluginVersion updates the property value; the tag still holds ${maven.ejb.version})
                assertThat(pom).containsPattern("<maven\\.ejb\\.version>3\\.2\\.[1-9]");

                // ejbVersion must be the literal "4.0" — decoupled from ${jee.ejb.api}.
                // If it were still ${jee.ejb.api}, a future bump of that property would silently
                // break the plugin config; the two values must be independent after migration.
                return assertThat(pom)
                  .contains("<ejbVersion>4.0</ejbVersion>")
                  .doesNotContain("<ejbVersion>${jee.ejb.api}</ejbVersion>")
                  .actual();
            })
          )
        );
    }
}
