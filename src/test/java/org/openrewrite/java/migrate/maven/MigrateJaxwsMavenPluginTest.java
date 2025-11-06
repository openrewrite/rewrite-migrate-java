/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class MigrateJaxwsMavenPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.javax.MigrateJaxBWSPlugin");
    }

    @DocumentExample
    @Test
    void migrateJaxwsPluginFromJvnetToSunXmlWs() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>jaxws-service</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.jvnet.jax-ws-commons</groupId>
                              <artifactId>jaxws-maven-plugin</artifactId>
                              <version>2.1</version>
                              <executions>
                                  <execution>
                                      <id>wsimport-from-jdk</id>
                                      <goals>
                                          <goal>wsimport</goal>
                                      </goals>
                                      <configuration>
                                          <sourceDestDir>${project.build.directory}/generated-sources/wsimport</sourceDestDir>
                                          <wsdlDirectory>${project.basedir}/src/main/resources/wsdl</wsdlDirectory>
                                          <wsdlFiles>
                                              <wsdlFile>STSSAPPrimingService.wsdl</wsdlFile>
                                          </wsdlFiles>
                                          <packageName>org.example.generated</packageName>
                                          <keep>true</keep>
                                          <nocompile>true</nocompile>
                                          <verbose>true</verbose>
                                      </configuration>
                                  </execution>
                              </executions>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.14.1</version>
                              <executions>
                                  <execution>
                                      <configuration>
                                          <nocompile>true</nocompile>
                                      </configuration>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>jaxws-service</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-maven-plugin</artifactId>
                              <version>2.3.7</version>
                              <executions>
                                  <execution>
                                      <id>wsimport-from-jdk</id>
                                      <goals>
                                          <goal>wsimport</goal>
                                      </goals>
                                      <configuration>
                                          <sourceDestDir>${project.build.directory}/generated-sources/wsimport</sourceDestDir>
                                          <wsdlDirectory>${project.basedir}/src/main/resources/wsdl</wsdlDirectory>
                                          <wsdlFiles>
                                              <wsdlFile>STSSAPPrimingService.wsdl</wsdlFile>
                                          </wsdlFiles>
                                          <packageName>org.example.generated</packageName>
                                          <keep>true</keep>
                                          <xnocompile>true</xnocompile>
                                          <verbose>true</verbose>
                                      </configuration>
                                      <phase>generate-sources</phase>
                                  </execution>
                              </executions>
                          </plugin>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.14.1</version>
                              <executions>
                                  <execution>
                                      <configuration>
                                          <nocompile>true</nocompile>
                                      </configuration>
                                  </execution>
                              </executions>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
