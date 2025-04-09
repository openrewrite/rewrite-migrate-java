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

import java.util.regex.Pattern;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeFacesOpenSourceLibrariesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.UpgradeFacesOpenSourceLibraries");
    }

    @Test
    void changeVersionAndAddClassfier() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                  <dependency>
                    <groupId>org.primefaces</groupId>
                    <artifactId>primefaces</artifactId>
                    <version>12.0.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            spec -> spec.after(actual -> {
                String version = Pattern.compile("<version>(14\\.0\\.\\d+)</version>")
                  .matcher(actual).results().reduce((a, b) -> b).orElseThrow().group(1);
                return """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>org.sample</groupId>
                    <artifactId>sample</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                      <dependency>
                        <groupId>org.primefaces</groupId>
                        <artifactId>primefaces</artifactId>
                        <version>%s</version>
                        <classifier>jakarta</classifier>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(version);
            })
          )
        );
    }
}
