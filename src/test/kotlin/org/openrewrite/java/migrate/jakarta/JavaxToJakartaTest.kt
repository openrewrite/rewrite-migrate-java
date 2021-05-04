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
package org.openrewrite.java.migrate.jakarta

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.config.Environment
import org.openrewrite.java.ChangeType
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.maven.AddDependency
import org.openrewrite.maven.MavenParser

class JavaxToJakartaTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")

    @Test
    fun dontAddImportWhenNoChangesWereMade() = assertUnchanged(
        before = "public class B {}"
    )

    @ParameterizedTest
    @MethodSource("changeTypeWithWildCardTest")
    fun changeImport(source: String, target: String, pkg: String, className: String) {
        assertChanged(
            before = """
                package org.A;

                import $source$pkg$className;

                public class B {
                }
            """,
            after = """
                package org.A;

                import $target$pkg$className;

                public class B {
                }
            """
        )
    }

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun changeQualifiedFieldAccess(source: String, target: String, pkg: String, className: String) {
        assertChanged(
            before = """
                package org.A;

                public class B {
                    $source$pkg$className name =  new $source$pkg$className();
                }
            """,
            after = """
                package org.A;

                public class B {
                    $target$pkg$className name =  new $target$pkg$className();
                }
            """
        )
    }

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun fullyQualifiedName(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = "public class B extends $source$pkg$className {}",
        after = "public class B extends $target$pkg$className {}"
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun annotation(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = "@$source$pkg$className public class B {}",
        after = "@$target$pkg$className public class B {}"
    )

    // array types and new arrays
    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun array(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className[] a = new $className[0];
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className[] a = new $className[0];
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun classDecl(source: String, target: String, pkg: String, className: String) = assertChanged(
        dependsOn = arrayOf(
            "public interface I1 {}",
            "public interface I2 {}"
        ),
        recipe = recipe.doNext(
            ChangeType("I1", "I2")
        ),
        before = """
            import $source$pkg$className;
            public class B extends $className implements I1 {}
        """,
        after = """
            import $target$pkg$className;
            public class B extends $className implements I2 {}
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun method(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public $className foo() throws $className { return null; }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public $className foo() throws $className { return null; }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun methodInvocationTypeParametersAndWildcard(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public <T extends $className> T generic(T n, List<? super $className> in);
               public void test() {
                   $className.stat();
                   this.<$className>generic(null, null);
               }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public <T extends $className> T generic(T n, List<? super $className> in);
               public void test() {
                   $className.stat();
                   this.<$className>generic(null, null);
               }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun multiCatch(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               public void test() {
                   try {}
                   catch($className | RuntimeException e) {}
               }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               public void test() {
                   try {}
                   catch($className | RuntimeException e) {}
               }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun multiVariable(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className f1, f2;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className f1, f2;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun newClass(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = new $className();
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = new $className();
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun parameterizedType(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               Map<$className, $className> m;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               Map<$className, $className> m;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun typeCast(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = ($className) null;
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = ($className) null;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun classReference(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class A {
                Class<?> clazz = $className.class;
            }
        """,
        after = """
            import $target$pkg$className;
            public class A {
                Class<?> clazz = $className.class;
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun methodSelect(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import $source$pkg$className;
            public class B {
               $className a = null;
               public void test() { a.foo(); }
            }
        """,
        after = """
            import $target$pkg$className;
            public class B {
               $className a = null;
               public void test() { a.foo(); }
            }
        """
    )

    @ParameterizedTest
    @MethodSource("changeTypeTest")
    fun staticImport(source: String, target: String, pkg: String, className: String) = assertChanged(
        before = """
            import static $source$pkg$className.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """,
        after = """
            import static $target$pkg$className.stat;
            public class B {
                public void test() {
                    stat();
                }
            }
        """
    )

    @Test
    fun onlyIfUsing() {
        val recipe = AddDependency(
            "jakarta.xml.bind",
            "jakarta.xml.bind-api",
            "3.0.0",
            null,
            true,
            null,
            null,
            null,
            null,
            listOf("jakarta.xml.bind.*")
        )
        val javaSource = JavaParser.fromJavaVersion().build().parse("""
            package org.openrewrite.java.testing;
            import jakarta.xml.bind.MarshalException;
            public class A {
                MarshalException getMap() {
                    return new MarshalException();
                }
            }
        """)[0]
        val mavenSource = MavenParser.builder().build().parse("""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
              </dependencies>
            </project>
        """.trimIndent())[0]

        val sources: List<SourceFile> = listOf(javaSource, mavenSource)
        val results = recipe.run(sources, InMemoryExecutionContext{ error: Throwable -> throw error})
        val mavenResult = results.find { it.before === mavenSource }
        Assertions.assertThat(mavenResult).isNotNull

        Assertions.assertThat(mavenResult?.after?.print()).isEqualTo("""
            <project>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.bind</groupId>
                  <artifactId>jakarta.xml.bind-api</artifactId>
                  <version>3.0.0</version>
                </dependency>
              </dependencies>
            </project>
        """.trimIndent())
    }

    // TODO: reduce test brevity. Currently, exhaustive to test all the recipes together once they're written.
    companion object{
        @JvmStatic
        fun changeTypeTest() = listOf(
            Arguments.of("javax.", "jakarta.", "xml.bind.annotation.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "xml.ws.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "transaction.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "activation.", "TestClass"),
        )

        @JvmStatic
        fun changeTypeWithWildCardTest() = changeTypeTest() + listOf(
            Arguments.of("javax.", "jakarta.", "xml.bind.annotation.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "xml.ws.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "transaction.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "activation.", "TestClass"),
            Arguments.of("javax.", "jakarta.", "activation.", "*"),
        )
    }
}
