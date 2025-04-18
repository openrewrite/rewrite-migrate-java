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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class BouncyCastleTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On"));
    }

    @DocumentExample
    @Test
    void updateBouncyCastle() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencies>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>bcprov-jdk15on</artifactId>
                      <version>1.70</version>
                    </dependency>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>bcpkix-jdk15on</artifactId>
                      <version>1.70</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              spec -> spec.after(str -> assertThat(str)
                .doesNotContainPattern("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>bcprov-jdk15on<\\/artifactId>\\s+<version>.*<\\/version>")
                .doesNotContainPattern("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>bcpkix-jdk15on<\\/artifactId>\\s+<version>.*<\\/version>")
                .containsPattern("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>bcprov-jdk18on<\\/artifactId>\\s+<version>.*<\\/version>")
                .containsPattern("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>bcpkix-jdk18on<\\/artifactId>\\s+<version>.*<\\/version>")
                .actual())
            )
          )
        );
    }


    @ParameterizedTest
    @ValueSource(strings={"bcprov", "bcutil", "bcpkix", "bcmail", "bcjmail", "bcpg", "bctls"})
    void testUpdateBouncyCastle(String value) {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              String.format("""
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencies>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>%s-jdk15on</artifactId>
                      <version>1.70</version>
                    </dependency>
                  </dependencies>
                </project>
                """, value),
              spec -> spec.after(str -> assertThat(str)
                .doesNotContainPattern(String.format("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>%s-jdk15on<\\/artifactId>\\s+<version>.*<\\/version>", value))
                .containsPattern(String.format("\\h*<groupId>org\\.bouncycastle<\\/groupId>\\s+<artifactId>%s-jdk18on<\\/artifactId>\\s+<version>.*<\\/version>", value))
                .actual())
            )
          )
        );
    }
}
