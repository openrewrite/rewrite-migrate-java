/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.wro4j;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeWro4jMavenPluginVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void v1OnJava11() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava11"),
          // as taken from Spring PetClinic 1.5.x
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <wro4j.version>1.8.0</wro4j.version>
                </properties>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>ro.isdc.wro4j</groupId>
                      <artifactId>wro4j-maven-plugin</artifactId>
                      <version>${wro4j.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <wro4j.version>1.10.1</wro4j.version>
                </properties>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>ro.isdc.wro4j</groupId>
                      <artifactId>wro4j-maven-plugin</artifactId>
                      <version>${wro4j.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void v2OnJava17() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava17"),
          pomXml(
            //language=xml
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <wro4j.version>1.8.0</wro4j.version>
                </properties>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>ro.isdc.wro4j</groupId>
                      <artifactId>wro4j-maven-plugin</artifactId>
                      <version>${wro4j.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(actual -> {
                assertThat(actual).contains("<wro4j.version>2");
                return actual;
            })
          )
        );
    }
}
