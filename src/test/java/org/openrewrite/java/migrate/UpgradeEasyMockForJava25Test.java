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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeEasyMockForJava25Test implements RewriteTest {
    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>org.example</groupId>
          <artifactId>example</artifactId>
          <version>1</version>
          <dependencies>
              <dependency>
                  <groupId>org.easymock</groupId>
                  <artifactId>easymock</artifactId>
                  <version>5.1.0</version>
                  <scope>test</scope>
              </dependency>
          </dependencies>
      </project>
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @Test
    void upgradeProxyImplementation() {
        rewriteRun(pomXml(POM, POM.replace("5.1.0", "5.6.0")));
    }

    @Test
    void preserveNewerVersion() {
        rewriteRun(pomXml(POM.replace("5.1.0", "5.7.0")));
    }

    @Test
    void preserveCurrentVersion() {
        rewriteRun(pomXml(POM.replace("5.1.0", "5.6.0")));
    }
}
