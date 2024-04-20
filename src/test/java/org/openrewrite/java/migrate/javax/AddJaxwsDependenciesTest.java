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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;

class AddJaxwsDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.javax.AddJaxwsDependencies"));
    }

    @Test
    void addJaxwsRuntimeOnce() {
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
                  implementation "jakarta.xml.ws:jakarta.xml.ws-api:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                //language=gradle
                return """
                  plugins {
                      id "java-library"
                  }
                   
                  repositories {
                      mavenCentral()
                  }
                   
                  dependencies {
                      compileOnly "com.sun.xml.ws:jaxws-rt:%s"
                                    
                      implementation "jakarta.xml.ws:jakarta.xml.ws-api:%s"
                                    
                      testImplementation "com.sun.xml.ws:jaxws-rt:%s"
                  }
                  """.formatted(rtVersion, wsApiVersion, rtVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxws</groupId>
                  <artifactId>jaxws-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.ws</groupId>
                          <artifactId>jakarta.xml.ws-api</artifactId>
                          <version>2.3.2</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxws</groupId>
                      <artifactId>jaxws-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.ws</groupId>
                              <artifactId>jakarta.xml.ws-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-rt</artifactId>
                              <version>%s</version>
                              <scope>provided</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(wsApiVersion, rtVersion);
            })
          )
        );
    }

    @Test
    void removeReferenceImplementationRuntime() {
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
                  compileOnly "com.sun.xml.ws:jaxws-ri:2.3.2"
                            
                  implementation "javax.xml.ws:jaxws-api:2.3.1"
                            
                  testImplementation "com.sun.xml.ws:jaxws-ri:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                //language=gradle
                return """
                  plugins {
                      id "java-library"
                  }
                                    
                  repositories {
                      mavenCentral()
                  }
                                    
                  dependencies {
                      implementation "com.sun.xml.ws:jaxws-rt:%s"
                                    
                      implementation "jakarta.xml.ws:jakarta.xml.ws-api:%s"
                  }
                  """.formatted(rtVersion, wsApiVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxws</groupId>
                  <artifactId>jaxws-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.ws</groupId>
                          <artifactId>jaxws-api</artifactId>
                          <version>2.3.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.sun.xml.ws</groupId>
                          <artifactId>jaxws-ri</artifactId>
                          <version>2.3.2</version>
                          <scope>provided</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxws</groupId>
                      <artifactId>jaxws-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.ws</groupId>
                              <artifactId>jakarta.xml.ws-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-rt</artifactId>
                              <version>%s</version>
                              <scope>provided</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(wsApiVersion, rtVersion);
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
                  compileOnly "com.sun.xml.ws:jaxws-ri:2.3.2"
                            
                  implementation "jakarta.xml.ws:jakarta.xml.ws-api:2.3.2"
                            
                  testImplementation "com.sun.xml.ws:jaxws-ri:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                //language=gradle
                return """
                  plugins {
                      id "java-library"
                  }
                                    
                  repositories {
                      mavenCentral()
                  }
                                    
                  dependencies {
                      implementation "com.sun.xml.ws:jaxws-rt:%s"
                                    
                      implementation "jakarta.xml.ws:jakarta.xml.ws-api:%s"
                  }
                  """.formatted(rtVersion, wsApiVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxws</groupId>
                  <artifactId>jaxws-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.ws</groupId>
                          <artifactId>jakarta.xml.ws-api</artifactId>
                          <version>2.3.2</version>
                      </dependency>
                      <dependency>
                          <groupId>com.sun.xml.ws</groupId>
                          <artifactId>jaxws-ri</artifactId>
                          <version>2.3.2</version>
                          <scope>provided</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxws</groupId>
                      <artifactId>jaxws-example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.ws</groupId>
                              <artifactId>jakarta.xml.ws-api</artifactId>
                              <version>%s</version>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-rt</artifactId>
                              <version>%s</version>
                              <scope>provided</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(wsApiVersion, rtVersion);
            })
          )
        );
    }

    @Test
    void renameAndUpdateApiAndAddRuntimeManagedDependencies() {
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
                  implementation "jakarta.xml.ws:jakarta.xml.ws-api:2.3.2"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("2\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                //language=gradle
                return """
                  plugins {
                      id "java-library"
                  }
                                    
                  repositories {
                      mavenCentral()
                  }
                                    
                  dependencies {
                      compileOnly "com.sun.xml.ws:jaxws-rt:%s"
                                    
                      implementation "jakarta.xml.ws:jakarta.xml.ws-api:%s"
                                    
                      testImplementation "com.sun.xml.ws:jaxws-rt:%s"
                  }
                  """.formatted(rtVersion, wsApiVersion, rtVersion);
            })
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxws</groupId>
                  <artifactId>jaxws-example</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>javax.xml.ws</groupId>
                              <artifactId>jaxws-api</artifactId>
                              <version>2.3.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-ri</artifactId>
                              <version>2.3.2</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.ws</groupId>
                          <artifactId>jaxws-api</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("2.\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                String wsApiVersion = version.group(0);
                assertThat(version.find()).isTrue();
                String rtVersion = version.group(0);
                //language=xml
                return """
                  <project>
                      <groupId>com.example.jaxws</groupId>
                      <artifactId>jaxws-example</artifactId>
                      <version>1.0.0</version>
                      <dependencyManagement>
                          <dependencies>
                              <dependency>
                                  <groupId>jakarta.xml.ws</groupId>
                                  <artifactId>jakarta.xml.ws-api</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>jakarta.xml.ws</groupId>
                              <artifactId>jakarta.xml.ws-api</artifactId>
                          </dependency>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-rt</artifactId>
                              <version>%s</version>
                              <scope>provided</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(wsApiVersion, rtVersion);
            })
          )
        );
    }
}
