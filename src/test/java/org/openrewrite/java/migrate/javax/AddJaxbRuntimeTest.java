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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

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
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jaxb-api-2.3.1"));
    }

    @DocumentExample
    @Test
    void swapRuntimeVersionedByParentProperty() {
        rewriteRun(
          mavenProject("parent",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <properties>
                        <jaxb.version>2.2.11</jaxb.version>
                    </properties>
                    <modules>
                        <module>child</module>
                    </modules>
                </project>
                """,
              //language=xml
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <properties>
                        <jaxb.version>2.3.9</jaxb.version>
                    </properties>
                    <modules>
                        <module>child</module>
                    </modules>
                </project>
                """
            ),
            mavenProject("child",
              pomXml(
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.sun.xml.bind</groupId>
                              <artifactId>jaxb-impl</artifactId>
                              <version>${jaxb.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>child</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <version>${jaxb.version}</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void swapRuntimeVersionedByGradleProperty() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          properties(
            "jaxbVersion=2.2.11",
            "jaxbVersion=2.3.9",
            spec -> spec.path("gradle.properties")
          ),
          buildGradle(
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  runtimeOnly "com.sun.xml.bind:jaxb-impl:${jaxbVersion}"
              }
              """,
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  runtimeOnly "org.glassfish.jaxb:jaxb-runtime:${jaxbVersion}"
              }
              """
          )
        );
    }

    @Test
    void doNotAddToBuildscriptDependencies() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                        implementation "jakarta\\.xml\\.bind:jakarta\\.xml\\.bind-api:2\\.3\\.2"

                        runtimeOnly "org\\.glassfish\\.jaxb:jaxb-runtime:2\\.3\\.\\d+"
                    }""")
                  .actual())
          )
        );
    }

    @Test
    void addTopLevelDependenciesBlockWhenOnlyBuildscriptHasOne() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                        runtimeOnly "org\\.glassfish\\.jaxb:jaxb-runtime:2\\.3\\.\\d+"
                    }""")
                  .actual())
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
