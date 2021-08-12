/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.migrate.javax

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.maven.MavenRecipeTest

class AddJaxbDependenciesTest : MavenRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.javax.AddJaxbDependencies")

    companion object {
        private val jaxbContextStub : String = """
            package javax.xml.bind;
            public class JAXBContext {
                public static JAXBContext newInstance(String packages) {
                    return null;
                }
                public static JAXBContext newInstance(java.lang.Class<?> packages) {
                    return null;
                }
            }
        """.trimIndent()

        private val javaSourceWithJaxb : String = """
            package org.old.code;
            import javax.xml.bind.JAXBContext;
            
            public class Yep {
                private static final JAXBContext context = JAXBContext.newInstance("org.old.code");
            }
        """.trimIndent()
        private val javaSourceWithoutJaxb : String = """
            package org.old.code;
            public class Nope {
            }
        """.trimIndent()
    }

    @Test
    fun onlyIfUsingJaxb() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent(),
        after ="""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.bind</groupId>
                  <artifactId>jakarta.xml.bind-api</artifactId>
                  <version>2.3.3</version>
                </dependency>
                <dependency>
                  <groupId>org.glassfish.jaxb</groupId>
                  <artifactId>jaxb-runtime</artifactId>
                  <version>2.3.5</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxb)
    )

    @Test
    fun doNotChangeIfNoJaxb() = assertUnchanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(javaSourceWithoutJaxb)
    )

    @Test
    fun migrateFromJaxbToJakarta() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>javax.xml.bind</groupId>
                  <artifactId>jaxb-api</artifactId>
                  <version>2.3.1</version>
                </dependency>
                <dependency>
                  <groupId>com.sun.xml.bind</groupId>
                  <artifactId>jaxb-impl</artifactId>
                  <version>2.3.4</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent(),
        after ="""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.bind</groupId>
                  <artifactId>jakarta.xml.bind-api</artifactId>
                  <version>2.3.3</version>
                </dependency>
                <dependency>
                  <groupId>org.glassfish.jaxb</groupId>
                  <artifactId>jaxb-runtime</artifactId>
                  <version>2.3.5</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxb)
    )
    @Test
    fun migrateFromJaxbToJakartaParent() = assertChanged(
        before = """
            <project>
                <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-child</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.sun.xml.bind</groupId>
                        <artifactId>jaxb-impl</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent(),
        after ="""
            <project>
                <parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-parent</artifactId>
                    <version>1</version>
                </parent>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-child</artifactId>
                <version>1</version>
                <dependencies>
                    <dependency>
                        <groupId>jakarta.xml.bind</groupId>
                        <artifactId>jakarta.xml.bind-api</artifactId>
                        <version>2.3.3</version>
                    </dependency>
                    <dependency>
                        <groupId>org.glassfish.jaxb</groupId>
                        <artifactId>jaxb-runtime</artifactId>
                        <version>2.3.5</version>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxb),
        additionalMavenFiles = arrayOf(
            """
                <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                      <dependencies>
                        <dependency>
                            <groupId>javax.xml.bind</groupId>
                            <artifactId>jaxb-api</artifactId>
                            <version>2.3.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.sun.xml.bind</groupId>
                            <artifactId>jaxb-impl</artifactId>
                          <version>2.3.4</version>
                        </dependency>
                      </dependencies>
                  </dependencyManagement>
                </project>
            """.trimIndent()
        )
    )

}
