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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeToJava8Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.UpgradeToJava8"))
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @DocumentExample
    @Test
    void upgradeFromJava7ToJava8() {
        rewriteRun(
          version(
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
              ),
              //language=java
              srcMainJava(
                java(
                  """
                    package com.abc;

                    interface SomeMBean {
                        String test();
                    }
                    """,
                  """
                    package com.abc;

                    public interface SomeMBean {
                        String test();
                    }
                    """
                )
              ),
              //language=java
              srcMainJava(
                java(
                  """
                    package com.abc;

                    interface SomeMXBean {
                        String test();
                    }
                    """,
                  """
                    package com.abc;

                    public interface SomeMXBean {
                        String test();
                    }
                    """
                )
              )
            ),
            8)
        );
    }
}
