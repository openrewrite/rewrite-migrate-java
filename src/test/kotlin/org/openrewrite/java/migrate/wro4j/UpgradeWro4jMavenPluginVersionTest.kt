/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.wro4j

import org.junit.jupiter.api.Test
import org.openrewrite.config.Environment
import org.openrewrite.maven.Assertions.pomXml
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class UpgradeWro4jMavenPluginVersionTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.wro4j")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.wro4j.UpgradeWro4jMavenPluginVersion"))
    }

    @Test
    fun property() = rewriteRun(
        // As taken from Spring PetClinic 1.5.x
        pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <wro4j.version>1.8.0</wro4j.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>ro.isdc.wro4j</groupId>
                    <artifactId>wro4j-maven-plugin</artifactId>
                    <version>${'$'}{wro4j.version}</version>
                    <executions>
                      <execution>
                        <phase>generate-resources</phase>
                        <goals>
                          <goal>run</goal>
                        </goals>
                      </execution>
                    </executions>
                    <configuration>
                      <wroManagerFactory>ro.isdc.wro.maven.plugin.manager.factory.ConfigurableWroManagerFactory</wroManagerFactory>
                      <cssDestinationFolder>${'$'}{project.build.directory}/classes/static/resources/css</cssDestinationFolder>
                      <wroFile>${'$'}{basedir}/src/main/wro/wro.xml</wroFile>
                      <extraConfigFile>${'$'}{basedir}/src/main/wro/wro.properties</extraConfigFile>
                      <contextFolder>${'$'}{basedir}/src/main/less</contextFolder>
                    </configuration>
                    <dependencies>
                      <dependency>
                        <groupId>org.webjars</groupId>
                        <artifactId>bootstrap</artifactId>
                        <version>${'$'}{webjars-bootstrap.version}</version>
                      </dependency>
                    </dependencies>
                  </plugin>
                </plugins>
              </build>
            </project>
        """,
        """
            <project>
              <modelVersion>4.0.0</modelVersion>
               
              <properties>
                <wro4j.version>1.10.1</wro4j.version>
              </properties>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>

              <build>
                <plugins>
                  <plugin>
                    <groupId>ro.isdc.wro4j</groupId>
                    <artifactId>wro4j-maven-plugin</artifactId>
                    <version>${'$'}{wro4j.version}</version>
                    <executions>
                      <execution>
                        <phase>generate-resources</phase>
                        <goals>
                          <goal>run</goal>
                        </goals>
                      </execution>
                    </executions>
                    <configuration>
                      <wroManagerFactory>ro.isdc.wro.maven.plugin.manager.factory.ConfigurableWroManagerFactory</wroManagerFactory>
                      <cssDestinationFolder>${'$'}{project.build.directory}/classes/static/resources/css</cssDestinationFolder>
                      <wroFile>${'$'}{basedir}/src/main/wro/wro.xml</wroFile>
                      <extraConfigFile>${'$'}{basedir}/src/main/wro/wro.properties</extraConfigFile>
                      <contextFolder>${'$'}{basedir}/src/main/less</contextFolder>
                    </configuration>
                    <dependencies>
                      <dependency>
                        <groupId>org.webjars</groupId>
                        <artifactId>bootstrap</artifactId>
                        <version>${'$'}{webjars-bootstrap.version}</version>
                      </dependency>
                    </dependencies>
                  </plugin>
                </plugins>
              </build>
            </project>
        """)
    )
} 
