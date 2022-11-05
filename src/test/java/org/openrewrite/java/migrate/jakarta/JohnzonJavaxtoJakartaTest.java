/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class JohnzonJavaxtoJakartaTest implements RewriteTest {

    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JohnzonJavaxToJakarta"));
    }

    @Test
    void migrateJohnzonDependencies() {
        //language=xml
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example.ehcache</groupId>
                  <artifactId>johnzon-legacy</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <johnzon.version>1.2.5</johnzon.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-core</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jaxrs</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonb</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonb-extras</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonschema</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-mapper</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-websocket</artifactId>
                          <version>${johnzon.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.example.ehcache</groupId>
                  <artifactId>johnzon-legacy</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <johnzon.version>1.2.19</johnzon.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-core</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jaxrs</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonb</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonb-extras</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-jsonschema</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-mapper</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.johnzon</groupId>
                          <artifactId>johnzon-websocket</artifactId>
                          <version>${johnzon.version}</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
