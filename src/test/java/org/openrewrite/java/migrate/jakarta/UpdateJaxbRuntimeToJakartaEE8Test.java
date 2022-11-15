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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class UpdateJaxbRuntimeToJakartaEE8Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new UpdateJaxbRuntimeToJakartaEE8("sun"))
        ;
    }

    @Test
    void addJaxbRuntimeOnce() {

        rewriteRun(
            pomXml(
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
                            <dependency>
                                <groupId>com.sun.xml.bind</groupId>
                                <artifactId>jaxb-impl</artifactId>
                                <version>2.3.2</version>
                                <scope>provided</scope>
                            </dependency>
                        </dependencies>
                    </project>
                """
            )
        );
    }

    @Test
    void renameRuntime() {

        rewriteRun(
          pomXml(
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
                        <dependency>
                            <groupId>org.glassfish.jaxb</groupId>
                            <artifactId>jaxb-runtime</artifactId>
                            <version>2.3.2</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """,
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
                        <dependency>
                            <groupId>com.sun.xml.bind</groupId>
                            <artifactId>jaxb-impl</artifactId>
                            <version>2.3.2</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """
          )
        );
    }

    @Test
    void renameAndUpdateApiAndRuntime() {

        rewriteRun(
          pomXml(
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
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """,
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
                        <dependency>
                            <groupId>com.sun.xml.bind</groupId>
                            <artifactId>jaxb-impl</artifactId>
                            <version>2.3.2</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """
          )
        );
    }

    @Test
    void renameAndUpdateApiAndAddRuntimeManagedDependencies() {

        rewriteRun(
          spec -> spec.cycles(3),
          pomXml(
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
                                <groupId>org.glassfish.jaxb</groupId>
                                <artifactId>jaxb-runtime</artifactId>
                                <version>2.3.1</version>
                                <scope>provided</scope>
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
            """
                <project>
                    <groupId>com.example.jaxb</groupId>
                    <artifactId>jaxb-example</artifactId>
                    <version>1.0.0</version>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>jakarta.xml.bind</groupId>
                                <artifactId>jakarta.xml.bind-api</artifactId>
                                <version>2.3.2</version>
                            </dependency>
                            <dependency>
                                <groupId>com.sun.xml.bind</groupId>
                                <artifactId>jaxb-impl</artifactId>
                                <version>2.3.2</version>
                                <scope>provided</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.xml.bind</groupId>
                            <artifactId>jakarta.xml.bind-api</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>com.sun.xml.bind</groupId>
                            <artifactId>jaxb-impl</artifactId>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
            """
          )
        );
    }
}
