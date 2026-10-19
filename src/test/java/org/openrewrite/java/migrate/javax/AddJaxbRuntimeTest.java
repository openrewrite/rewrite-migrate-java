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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;

class AddJaxbRuntimeTest implements RewriteTest {

    // language=java
    private static final String CLASS_USING_XML_BIND = """
      import javax.xml.bind.annotation.XmlElement;

      public class Test {
          @XmlElement
          private String name;
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJaxbRuntime("glassfish"))
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jaxb-api"))
          .beforeRecipe(withToolingApi());
    }

    @Test
    void doNotAddToBuildscriptDependencies() {
        rewriteRun(
          java(CLASS_USING_XML_BIND),
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
                  implementation "jakarta.xml.bind:jakarta.xml.bind-api:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("jaxb-runtime:(2\\.3\\.\\d+)").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                //language=gradle
                return """
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
                      implementation "jakarta.xml.bind:jakarta.xml.bind-api:2.3.2"

                      runtimeOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  }
                  """.formatted(version.group(1));
            })
          )
        );
    }

    @Test
    void addTopLevelDependenciesBlockWhenOnlyBuildscriptHasOne() {
        rewriteRun(
          java(CLASS_USING_XML_BIND),
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
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("jaxb-runtime:(2\\.3\\.\\d+)").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                //language=gradle
                return """
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

                  dependencies {
                      runtimeOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  }
                  """.formatted(version.group(1));
            })
          ),
          buildGradle(
            //language=gradle
            """
              dependencies {
                  implementation "jakarta.xml.bind:jakarta.xml.bind-api:2.3.2"
              }
              """,
            spec -> spec.path("dependencies.gradle")
          )
        );
    }
}
