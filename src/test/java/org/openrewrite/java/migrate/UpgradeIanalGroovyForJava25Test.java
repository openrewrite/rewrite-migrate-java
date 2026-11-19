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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeIanalGroovyForJava25Test implements RewriteTest {
    private static final String CAMEL_COMPONENT = """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>org.apache.camel</groupId>
          <artifactId>components</artifactId>
          <version>2.15.5</version>
          <relativePath/>
        </parent>
        <artifactId>component</artifactId>
      </project>
      """;

    @Test
    void overrideInheritedPluginDependenciesWithoutChangingExecution() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeIanalGroovyForJava25()),
          pomXml(CAMEL_COMPONENT, CAMEL_COMPONENT.replace("</project>", """
          \s\s<build>
              <plugins>
                <plugin>
                  <groupId>org.codehaus.mojo</groupId>
                  <artifactId>ianal-maven-plugin</artifactId>
                  <version>1.0-alpha-1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.codehaus.groovy.maven</groupId>
                      <artifactId>gmaven-mojo</artifactId>
                      <version>1.0-rc-3</version>
                      <exclusions>
                        <exclusion>
                          <groupId>org.codehaus.groovy</groupId>
                          <artifactId>groovy-all-minimal</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                    <dependency>
                      <groupId>org.codehaus.groovy</groupId>
                      <artifactId>groovy</artifactId>
                      <version>3.0.25</version>
                    </dependency>
                    <dependency>
                      <groupId>org.codehaus.groovy</groupId>
                      <artifactId>groovy-ant</artifactId>
                      <version>3.0.25</version>
                    </dependency>
                  </dependencies>
                </plugin>
              </plugins>
            </build>
          </project>"""))
        );
    }

    @Test
    void leavesExecutionWithoutGoalsAlone() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeIanalGroovyForJava25()),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>example</groupId>
                <artifactId>no-goals</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.codehaus.mojo</groupId>
                      <artifactId>ianal-maven-plugin</artifactId>
                      <version>1.0-alpha-1</version>
                      <executions>
                        <execution>
                          <id>no-goals</id>
                          <phase>verify</phase>
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

    @Test
    void leaveCustomPluginDependenciesAlone() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeIanalGroovyForJava25()),
          pomXml(CAMEL_COMPONENT.replace("</project>", """
          \s\s<build><plugins><plugin>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>ianal-maven-plugin</artifactId>
              <dependencies><dependency>
                <groupId>org.codehaus.groovy</groupId>
                <artifactId>groovy</artifactId>
                <version>3.0.26</version>
              </dependency></dependencies>
            </plugin></plugins></build>
          </project>"""))
        );
    }
}
