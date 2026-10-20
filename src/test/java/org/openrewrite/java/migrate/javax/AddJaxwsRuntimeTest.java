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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class AddJaxwsRuntimeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJaxwsRuntime())
          .beforeRecipe(withToolingApi());
    }

    @Test
    void doNotAddToBuildscriptDependencies() {
        rewriteRun(
          buildGradle(
            //language=gradle
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                  }
              }

              apply plugin: 'java'

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "jakarta.xml.ws:jakarta.xml.ws-api:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle ->
                assertThat(buildGradle)
                  .contains(
                    //language=gradle
                    """
                    buildscript {
                        repositories {
                            mavenCentral()
                        }
                        dependencies {
                        }
                    }
                    """)
                  .containsPattern("""
                    (?m)^dependencies \\{
                        compileOnly "com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+"

                        implementation "jakarta\\.xml\\.ws:jakarta\\.xml\\.ws-api:2\\.3\\.2"

                        testImplementation "com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+"
                    }""")
                  .actual())
          )
        );
    }

    @Test
    void addTopLevelDependenciesBlockWhenOnlyBuildscriptHasOne() {
        rewriteRun(
          buildGradle(
            //language=gradle
            """
              buildscript {
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                  }
              }

              apply plugin: 'java'

              repositories {
                  mavenCentral()
              }

              apply from: "dependencies.gradle"
              """,
            spec -> spec.after(buildGradle ->
                assertThat(buildGradle)
                  .contains(
                    //language=gradle
                    """
                    buildscript {
                        repositories {
                            mavenCentral()
                        }
                        dependencies {
                        }
                    }
                    """)
                  .containsPattern("""
                    apply from: "dependencies\\.gradle"

                    dependencies \\{
                        compileOnly "com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+"

                        testImplementation "com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+"
                    }""")
                  .actual())
          ),
          buildGradle(
            //language=gradle
            """
              dependencies {
                  implementation "jakarta.xml.ws:jakarta.xml.ws-api:2.3.2"
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }
}
