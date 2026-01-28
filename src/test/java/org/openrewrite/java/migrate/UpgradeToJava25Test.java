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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeToJava25Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25");
    }

    @DocumentExample
    @Test
    void addsLombokAnnotationProcessor() {
        rewriteRun(
          spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.40</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> {
                    assertThat(actual)
                      .contains("<maven.compiler.release>25</maven.compiler.release>")
                      // check we have the expected annotation processor
                      .containsPattern("<annotationProcessorPaths>(.|\\n)*<path>(.|\\n)*<groupId>org.projectlombok")
                      .containsPattern("<annotationProcessorPaths>(.|\\n)*<path>(.|\\n)*<artifactId>lombok");
                    return actual;
                }
              )
            )
          )
        );
    }
}
