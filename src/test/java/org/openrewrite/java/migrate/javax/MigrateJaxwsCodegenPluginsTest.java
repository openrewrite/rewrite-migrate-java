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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class MigrateJaxwsCodegenPluginsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.javax.MigrateJaxBWSPlugin");
    }

    private static String pom(String group, String artifact, String version) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>codegen</artifactId>
              <version>1.0</version>
              <build>
                  <plugins>
                      <plugin>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                          <configuration>
                              <packageName>com.example.generated</packageName>
                              <wsdlDirectory>src/main/resources/wsdl</wsdlDirectory>
                          </configuration>
                          <executions>
                              <execution>
                                  <id>generate</id>
                                  <phase>generate-test-sources</phase>
                                  <goals><goal>%s</goal></goals>
                                  <configuration><nocompile>true</nocompile></configuration>
                              </execution>
                          </executions>
                          <dependencies>
                              <dependency>
                                  <groupId>javax.xml.ws</groupId>
                                  <artifactId>jaxws-api</artifactId>
                                  <version>2.3.1</version>
                              </dependency>
                          </dependencies>
                      </plugin>
                  </plugins>
              </build>
          </project>
          """.formatted(group, artifact, version, "jaxws-maven-plugin".equals(artifact) ? "wsimport" : "generate");
    }

    @Test
    void upgradeMojoHausPreservingConfiguration() {
        rewriteRun(pomXml(
          pom("org.codehaus.mojo", "jaxws-maven-plugin", "2.4.1"),
          pom("org.codehaus.mojo", "jaxws-maven-plugin", "2.6")
        ));
    }

    @Test
    void upgradeCodecentricPreservingConfiguration() {
        rewriteRun(pomXml(
          pom("de.codecentric", "cxf-spring-boot-starter-maven-plugin", "1.0.7.RELEASE"),
          pom("de.codecentric", "cxf-spring-boot-starter-maven-plugin", "2.3.0.RELEASE")
        ));
    }

    @Test
    void preserveCurrentAndNewerVersions() {
        rewriteRun(
          pomXml(pom("org.codehaus.mojo", "jaxws-maven-plugin", "2.6"), spec -> spec.path("mojo-current/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "jaxws-maven-plugin", "2.7"), spec -> spec.path("mojo-newer/pom.xml")),
          pomXml(pom("de.codecentric", "cxf-spring-boot-starter-maven-plugin", "2.3.0.RELEASE"), spec -> spec.path("cxf-current/pom.xml")),
          pomXml(pom("de.codecentric", "cxf-spring-boot-starter-maven-plugin", "2.3.1-jaxb-jaxws-3.0.0-M4"), spec -> spec.path("cxf-newer/pom.xml"))
        );
    }

    @Test
    void preservePropertyAndPluginManagement() {
        String before = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>managed-codegen</artifactId>
              <version>1.0</version>
              <properties>
                  <codegen.version>1.0.7.RELEASE</codegen.version>
              </properties>
              <build>
                  <pluginManagement>
                      <plugins>
                          <plugin>
                              <groupId>de.codecentric</groupId>
                              <artifactId>cxf-spring-boot-starter-maven-plugin</artifactId>
                              <version>${codegen.version}</version>
                              <configuration><packageName>com.example.generated</packageName></configuration>
                          </plugin>
                      </plugins>
                  </pluginManagement>
              </build>
          </project>
          """;
        String mojo = before.replace("de.codecentric", "org.codehaus.mojo")
          .replace("cxf-spring-boot-starter-maven-plugin", "jaxws-maven-plugin")
          .replace("1.0.7.RELEASE", "2.4.1");
        rewriteRun(
          pomXml(before, before.replace("1.0.7.RELEASE", "2.3.0.RELEASE"), spec -> spec.path("cxf/pom.xml")),
          pomXml(mojo, mojo.replace("2.4.1", "2.6"), spec -> spec.path("mojo/pom.xml"))
        );
    }

    @Test
    void doNotAddUnusedPlugins() {
        rewriteRun(pomXml("""
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>no-codegen</artifactId>
              <version>1.0</version>
          </project>
          """));
    }
}
