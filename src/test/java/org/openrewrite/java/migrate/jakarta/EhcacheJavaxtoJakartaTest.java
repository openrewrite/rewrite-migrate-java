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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class EhcacheJavaxtoJakartaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.EhcacheJavaxToJakarta"));
    }

    @DocumentExample
    @Test
    void migrateEhcacheDependencies() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example.ehcache</groupId>
                  <artifactId>ehcache-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache</artifactId>
                          <version>3.9.10</version>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-clustered</artifactId>
                          <version>3.9.10</version>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-transactions</artifactId>
                          <version>3.9.10</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.example.ehcache</groupId>
                  <artifactId>ehcache-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache</artifactId>
                          <version>3.10.8</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-clustered</artifactId>
                          <version>3.10.8</version>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-transactions</artifactId>
                          <version>3.10.8</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
