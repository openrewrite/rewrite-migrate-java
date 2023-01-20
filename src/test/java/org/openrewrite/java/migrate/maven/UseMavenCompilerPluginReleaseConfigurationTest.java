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
package org.openrewrite.java.migrate.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class UseMavenCompilerPluginReleaseConfigurationTest implements RewriteTest {
    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new UseMavenCompilerPluginReleaseConfiguration("11"));
    }

    @Test
    void replacesSourceAndTargetConfig() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <source>1.8</source>
                      <target>1.8</target>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>11</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void deletesSourceAndTargetConfigWithNoReplacementIfDefaultAndCleansUpConfiguration() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <source>${maven.compiler.source}</source>
                      <target>${maven.compiler.target}</target>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void deletesSourceAndTargetConfigWithNoReplacementIfDefault() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <parameters>true</parameters>
                      <source>${maven.compiler.source}</source>
                      <target>${maven.compiler.target}</target>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <parameters>true</parameters>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void reusesJavaVersionVariableIfAvailable() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <java.version>11</java.version>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <source>${java.version}</source>
                      <target>${java.version}</target>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <java.version>11</java.version>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>${java.version}</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void upgradesExistingReleaseConfig() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>10</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>11</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void prefersJavaVersionIfAvailable() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <java.version>11</java.version>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>10</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <java.version>11</java.version>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>${java.version}</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void notMisledByUnrelatedProperty() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <foobar>11</foobar>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>10</release>
                      <basedir>${foobar}</basedir>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <properties>
                <foobar>11</foobar>
              </properties>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>11</release>
                      <basedir>${foobar}</basedir>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Test
    void doesNotChangeIfNoSourceOrTargetConfig() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <compilerArgs>-parameters</compilerArgs>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/169")
    @Test
    void noVersionDowngrade() {
        rewriteRun(
          //language=xml
          pomXml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>sample</artifactId>
              <version>1.0.0</version>
              
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.8.0</version>
                    <configuration>
                      <release>17</release>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
              
            </project>
            """)
        );
    }
}
