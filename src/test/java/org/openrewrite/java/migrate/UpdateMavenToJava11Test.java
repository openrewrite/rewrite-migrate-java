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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMavenToJava11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath()
          .build().activateRecipes("org.openrewrite.java.migrate.JavaVersion11"));
    }

    @DocumentExample
    @Test
    void changeJavaVersion() {
        //language=xml
        rewriteRun(
          version(
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <properties>
                    <java.version>1.8</java.version>
                  </properties>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <properties>
                    <java.version>11</java.version>
                  </properties>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """
            ),
            8
          )
        );
    }

    @Test
    void changeMavenCompiler() {
        //language=xml
        rewriteRun(
          version(
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <properties>
                    <maven.compiler.source>1.8</maven.compiler.source>
                    <maven.compiler.target>1.8</maven.compiler.target>
                  </properties>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                  </properties>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """
            ),
            8
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/169")
    @Test
    void noDowngrade() {
        rewriteRun(
          version(
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <properties>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                  </properties>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """
            ),
            17
          )
        );
    }
}
