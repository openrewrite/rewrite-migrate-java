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

class AddJaxwsDependenciesTest : MavenRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.javax.AddJaxwsDependencies")

    companion object {
        private val jaxbContextStub : String = """
            package javax.jws;

            import java.lang.annotation.Target;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.ElementType;

            @Retention(value = RetentionPolicy.RUNTIME)
            @Target(value = {ElementType.TYPE})
            public @interface WebService {
            }
        """.trimIndent()

        private val javaSourceWithJaxws : String = """
            package org.old.code;
            import javax.jws.WebService;
            
            @WebService
            public class Yep {
            }
        """.trimIndent()
        private val javaSourceWithoutJaxws : String = """
            package org.old.code;
            public class Nope {
            }
        """.trimIndent()
    }

    @Test
    fun onlyIfUsingJaxws() = assertChanged(
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
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>2.3.3</version>
                </dependency>
                <dependency>
                  <groupId>com.sun.xml.ws</groupId>
                  <artifactId>jaxws-rt</artifactId>
                  <version>2.3.4</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxws)
    )

    @Test
    fun doNotChangeIfNoJaxws() = assertUnchanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(javaSourceWithoutJaxws)
    )

    @Test
    fun migrateFromJaxwsApiToJakarta() = assertChanged(
        before = """
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>javax.xml.ws</groupId>
                  <artifactId>jaxws-api</artifactId>
                  <version>2.3.1</version>
                </dependency>
                <dependency>
                  <groupId>com.sun.xml.ws</groupId>
                  <artifactId>jaxws-rt</artifactId>
                  <version>2.3.2</version>
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
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>2.3.3</version>
                </dependency>
                <dependency>
                  <groupId>com.sun.xml.ws</groupId>
                  <artifactId>jaxws-rt</artifactId>
                  <version>2.3.4</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxws)
    )
    @Test
    fun migrateFromJaxwsToJakartaParent() = assertChanged(
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
                        <groupId>javax.xml.ws</groupId>
                        <artifactId>jaxws-api</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.sun.xml.ws</groupId>
                        <artifactId>jaxws-rt</artifactId>
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
                        <groupId>jakarta.xml.ws</groupId>
                        <artifactId>jakarta.xml.ws-api</artifactId>
                        <version>2.3.3</version>
                    </dependency>
                    <dependency>
                        <groupId>com.sun.xml.ws</groupId>
                        <artifactId>jaxws-rt</artifactId>
                        <scope>runtime</scope>
                    </dependency>
                </dependencies>
            </project>
        """.trimIndent(),
        additionalJavaFiles = arrayOf(jaxbContextStub, javaSourceWithJaxws),
        additionalMavenFiles = arrayOf(
            """
            <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-parent</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>javax.xml.ws</groupId>
                            <artifactId>jaxws-api</artifactId>
                            <version>2.3.1</version>
                        </dependency>
                        <dependency>
                            <groupId>com.sun.xml.ws</groupId>
                            <artifactId>jaxws-rt</artifactId>
                            <version>2.3.2</version>
                            <scope>runtime</scope>
                        </dependency>
                      </dependencies>
                </dependencyManagement>
            </project>
            """.trimIndent()
        )
    )

}
