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
package org.openrewrite.java.migrate.jacoco;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;


class UpgradeJaCoCoMavenPluginVersionTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.jacoco")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.jacoco.UpgradeJaCoCoMavenPluginVersion"));
    }

    @Test
    void property() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <properties>
                  <jacoco.version>0.8.1</jacoco.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <properties>
                  <jacoco.version>0.8.8</jacoco.version>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """
          )
        );
    }
}
