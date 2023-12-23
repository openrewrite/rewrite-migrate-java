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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.assertj.core.api.Assertions.assertThat;

class AddJaxbDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.javax.AddJaxbDependencies"));
    }

    @Test
    void addJaxbRuntimeOnce() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                  implementation "jakarta.xml.bind:jakarta.xml.bind-api:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                return """
                  plugins {
                      id "java-library"
                  }
                  
                  repositories {
                      mavenCentral()
                  }
                  
                  dependencies {
                      implementation "jakarta.xml.bind:jakarta.xml.bind-api:%s"
                    
                      runtimeOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  }
                  """.formatted(bindApiVersion, runtimeVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.bind</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>2.3.2</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxb</groupId>
                      <artifactId>jaxb-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.bind</groupId>
                              <artifactId>jakarta.xml.bind-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <version>%s</version>
                              <scope>runtime</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(bindApiVersion, runtimeVersion);
            })
          )
        );
    }

    @Test
    void renameRuntime() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                  implementation "jakarta.xml.bind:jakarta.xml.bind-api:2.3.3"
              
                  compileOnly "com.sun.xml.bind:jaxb-impl:2.3.3"
              
                  testImplementation "com.sun.xml.bind:jaxb-impl:2.3.3"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                return """
                  plugins {
                      id "java-library"
                  }
                  
                  repositories {
                      mavenCentral()
                  }
                  
                  dependencies {
                      implementation "jakarta.xml.bind:jakarta.xml.bind-api:%s"
                  
                      runtimeOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  
                      compileOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  
                      testImplementation "org.glassfish.jaxb:jaxb-runtime:%s"
                  }
                  """.formatted(bindApiVersion, runtimeVersion, runtimeVersion, runtimeVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.bind</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>2.3.3</version>
                      </dependency>
                      <dependency>
                          <groupId>com.sun.xml.bind</groupId>
                          <artifactId>jaxb-impl</artifactId>
                          <version>2.3.3</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxb</groupId>
                      <artifactId>jaxb-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.bind</groupId>
                              <artifactId>jakarta.xml.bind-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <version>%s</version>
                              <scope>runtime</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(bindApiVersion, runtimeVersion);
            })
          )
        );
    }

    @Test
    void renameAndUpdateApiAndRuntime() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                  implementation "javax.xml.bind:jaxb-api:2.3.1"
              
                  compileOnly "org.glassfish.jaxb:jaxb-runtime:2.3.1"
              
                  testImplementation "org.glassfish.jaxb:jaxb-runtime:2.3.1"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                return """
                  plugins {
                      id "java-library"
                  }
                  
                  repositories {
                      mavenCentral()
                  }
                  
                  dependencies {
                      implementation "jakarta.xml.bind:jakarta.xml.bind-api:%s"
                  
                      runtimeOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  
                      compileOnly "org.glassfish.jaxb:jaxb-runtime:%s"
                  
                      testImplementation "org.glassfish.jaxb:jaxb-runtime:%s"
                  }
                  """.formatted(bindApiVersion, runtimeVersion, runtimeVersion, runtimeVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.bind</groupId>
                          <artifactId>jaxb-api</artifactId>
                          <version>2.3.1</version>
                      </dependency>
                      <dependency>
                          <groupId>org.glassfish.jaxb</groupId>
                          <artifactId>jaxb-runtime</artifactId>
                          <version>2.3.1</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxb</groupId>
                      <artifactId>jaxb-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.bind</groupId>
                              <artifactId>jakarta.xml.bind-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <version>%s</version>
                              <scope>runtime</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(bindApiVersion, runtimeVersion);
            })
          )
        );
    }

    @Test
    void renameAndUpdateApiAndAddRuntimeManagedDependencies() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.xml.bind</groupId>
                              <artifactId>jaxb-api</artifactId>
                              <version>2.3.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.bind</groupId>
                              <artifactId>jaxb-impl</artifactId>
                              <version>2.3.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.bind</groupId>
                          <artifactId>jaxb-api</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String bindApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String runtimeVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxb</groupId>
                      <artifactId>jaxb-example</artifactId>
                      <version>1.0.0</version>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>jakarta.xml.bind</groupId>
                                  <artifactId>jakarta.xml.bind-api</artifactId>
                                  <version>%s</version>
                              </dependency>
                              <dependency>
                                  <groupId>org.glassfish.jaxb</groupId>
                                  <artifactId>jaxb-runtime</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.bind</groupId>
                              <artifactId>jakarta.xml.bind-api</artifactId>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish.jaxb</groupId>
                              <artifactId>jaxb-runtime</artifactId>
                              <scope>runtime</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(bindApiVersion, runtimeVersion);
            })
          )
        );
    }


    @Test
    void dontAddWhenJacksonPresent() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
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
                  api("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.16.0")
              }
              """
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-jaxb-annotations</artifactId>
                          <version>2.16.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
