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
package org.openrewrite.java.migrate.guava

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.Result
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.marker.JavaVersion
import org.openrewrite.java.tree.J
import java.util.*

class NoGuavaImmutableListOfTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaImmutableListOf()

    @Test
    fun doNotChangeReturnsImmutableList() {
        val before = """
            import com.google.common.collect.ImmutableList;

            class Test {
                ImmutableList<String> getList() {
                    return ImmutableList.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeFieldAssignmentToImmutableList() {
        val before = """
            import com.google.common.collect.ImmutableList;
            
            class Test {
                ImmutableList<String> m;
            
                {
                    this.m = ImmutableList.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeAssignsToImmutableList() {
        val before = """
            import com.google.common.collect.ImmutableList;
            
            class Test {
                ImmutableList<String> m;
            
                void init() {
                    m = ImmutableList.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeNewClass() {
        val dependsOn = """
            import com.google.common.collect.ImmutableList;

            public class A {
                ImmutableList<String> immutableList;
                public A(ImmutableList<String> immutableList) {
                    this.immutableList = immutableList;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableList;

            class Test {
                A a = new A(ImmutableList.of());
            }
        """
        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeMethodInvocation() {
        val dependsOn = """
            import com.google.common.collect.ImmutableList;

            public class A {
                ImmutableList<String> immutableList;
                public void method(ImmutableList<String> immutableList) {
                    this.immutableList = immutableList;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableList;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableList.of());
                }
            }
        """
        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun replaceArguments() {
        val before = """
            import java.util.List;
            import com.google.common.collect.ImmutableList;
            
            class Test {
                List<String> m = ImmutableList.of("A", "B", "C", "D");
            }
        """
        val after = """
            import java.util.List;
            
            class Test {
                List<String> m = List.of("A", "B", "C", "D");
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    @Test
    fun fieldAssignmentToList() {
        val before = """
            import java.util.List;
            import com.google.common.collect.ImmutableList;
            
            class Test {
                List<String> m;
                {
                    this.m = ImmutableList.of();
                }
            }
        """
        val after = """
            import java.util.List;
            
            class Test {
                List<String> m;
                {
                    this.m = List.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    @Test
    fun assigmentToList() {
        val before = """
            import java.util.List;
            import com.google.common.collect.ImmutableList;
            
            class Test {
                List<String> m = ImmutableList.of();
            }
        """
        val after = """
            import java.util.List;
            
            class Test {
                List<String> m = List.of();
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    @Test
    fun returnsList() {
        val before = """
            import java.util.List;
            import com.google.common.collect.ImmutableList;
            
            class Test {
                List<String> list() {
                    return ImmutableList.of();
                }
            }
        """
        val after = """
            import java.util.List;
            
            class Test {
                List<String> list() {
                    return List.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    @Test
    fun newClassWithListArgument() {
        val dependsOn = """
            import java.util.List;

            public class A {
                List<String, String> list;
                public A(List<String> list) {
                    this.list = list;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableList;

            class Test {
                A a = new A(ImmutableList.of());
            }
        """
        val after = """
            import java.util.List;

            class Test {
                A a = new A(List.of());
            }
        """

        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    @Test
    fun methodInvocationWithListArgument() {
        val dependsOn = """
            import java.util.List;

            public class A {
                List<String> list;
                public void method(List<String> list) {
                    this.list = list;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableList;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableList.of());
                }
            }
        """
        val after = """
            import java.util.List;

            class Test {
                void method() {
                    A a = new A();
                    a.method(List.of());
                }
            }
        """

        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.printAll()).isEqualTo(after)
        }
    }

    private fun runRecipe(input: Array<String>): List<Result> {
        return recipe.run(sourceFilesWithJavaVersion(input), executionContext)
    }

    private fun sourceFilesWithJavaVersion(input: Array<String>): List<J.CompilationUnit> {
        return parser.parse(executionContext, *input).map { j ->
            j.withMarkers(
                j.markers.addIfAbsent(getJavaVersion())
            )
        }
    }

    private fun getJavaVersion(): JavaVersion {
        val javaRuntimeVersion = System.getProperty("java.runtime.version")
        val javaVendor = System.getProperty("java.vm.vendor")
        return JavaVersion(UUID.randomUUID(), javaRuntimeVersion, javaVendor, javaRuntimeVersion, javaRuntimeVersion)
    }

}
