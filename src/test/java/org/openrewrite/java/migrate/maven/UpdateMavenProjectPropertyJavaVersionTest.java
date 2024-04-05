/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateMavenProjectPropertyJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateMavenProjectPropertyJavaVersion(17));
    }

    @Test
    void basic() {
        rewriteRun(
          //language=xml
          pomXml("""
            <project>
                <groupId>com.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0.0</version>
                <modelVersion>4.0</modelVersion>
                <properties>
                    <java.version>11</java.version>
                    <jdk.version>11</jdk.version>
                    <javaVersion>11</javaVersion>
                    <jdkVersion>11</jdkVersion>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                    <maven.compiler.release>11</maven.compiler.release>
                    <release.version>11</release.version>
                </properties>
            </project>
            """,
            """
            <project>
                <groupId>com.example</groupId>
                <artifactId>foo</artifactId>
                <version>1.0.0</version>
                <modelVersion>4.0</modelVersion>
                <properties>
                    <java.version>17</java.version>
                    <jdk.version>17</jdk.version>
                    <javaVersion>17</javaVersion>
                    <jdkVersion>17</jdkVersion>
                    <maven.compiler.source>17</maven.compiler.source>
                    <maven.compiler.target>17</maven.compiler.target>
                    <maven.compiler.release>17</maven.compiler.release>
                    <release.version>17</release.version>
                </properties>
            </project>
            """)
        );
    }

    @Test
    void bringsDownExplicitlyUsedPropertyFromRemoteParent() {
        rewriteRun(
          //language=xml
          pomXml("""
            <project>
                <groupId>com.example</groupId>
                <artifactId>example-parent</artifactId>
                <version>1.0.0</version>
                <modelVersion>4.0</modelVersion>
                <properties>
                    <java.version>11</java.version>
                    <jdk.version>11</jdk.version>
                    <javaVersion>11</javaVersion>
                    <jdkVersion>11</jdkVersion>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                    <maven.compiler.release>11</maven.compiler.release>
                    <release.version>11</release.version>
                </properties>
            </project>
            """,
            SourceSpec::skip),
          mavenProject("example-child",
            //language=xml
            pomXml("""
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>example-parent</artifactId>
                        <version>1.0.0</version>
                        <!-- lookup parent from remote repository -->
                        <relativePath/>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>example-child</artifactId>
                    <version>1.0.0</version>
                    <modelVersion>4.0</modelVersion>
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
                """,
               """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>example-parent</artifactId>
                        <version>1.0.0</version>
                        <!-- lookup parent from remote repository -->
                        <relativePath/>
                    </parent>
                    <groupId>com.example</groupId>
                    <artifactId>example-child</artifactId>
                    <version>1.0.0</version>
                    <modelVersion>4.0</modelVersion>
                    <properties>
                        <java.version>17</java.version>
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
                """)
          )
        );
    }
}
