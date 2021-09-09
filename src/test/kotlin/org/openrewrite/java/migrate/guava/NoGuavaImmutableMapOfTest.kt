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

class NoGuavaImmutableMapOfTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaImmutableMapOf()

    @Test
    fun doNotChangeReturnsImmutableMap() {
        val before = """
            import com.google.common.collect.ImmutableMap;

            class Test {
                ImmutableMap<String, String> getMap() {
                    return ImmutableMap.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeFieldAssignmentToImmutableMap() {
        val before = """
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                ImmutableMap<String, String> m;
            
                {
                    this.m = ImmutableMap.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeAssignsToImmutableMap() {
        val before = """
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                ImmutableMap<String, String> m;
            
                void init() {
                    m = ImmutableMap.of();
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
            import com.google.common.collect.ImmutableMap;

            public class A {
                ImmutableMap<String, String> immutableMap;
                public A(ImmutableMap<String, String> immutableMap) {
                    this.immutableMap = immutableMap;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableMap;

            class Test {
                A a = new A(ImmutableMap.of());
            }
        """
        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeMethodInvocation() {
        val dependsOn = """
            import com.google.common.collect.ImmutableMap;

            public class A {
                ImmutableMap<String, String> immutableMap;
                public void method(ImmutableMap<String, String> immutableMap) {
                    this.immutableMap = immutableMap;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableMap;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableMap.of());
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
            import java.util.Map;
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                Map<String, String> m = ImmutableMap.of("A", "B", "C", "D");
            }
        """
        val after = """
            import java.util.Map;
            
            class Test {
                Map<String, String> m = Map.of("A", "B", "C", "D");
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun fieldAssignmentToMap() {
        val before = """
            import java.util.Map;
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                Map<String, String> m;
                {
                    this.m = ImmutableMap.of();
                }
            }
        """
        val after = """
            import java.util.Map;
            
            class Test {
                Map<String, String> m;
                {
                    this.m = Map.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun assigmentToMap() {
        val before = """
            import java.util.Map;
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                Map<String, String> m = ImmutableMap.of();
            }
        """
        val after = """
            import java.util.Map;
            
            class Test {
                Map<String, String> m = Map.of();
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun returnsMap() {
        val before = """
            import java.util.Map;
            import com.google.common.collect.ImmutableMap;
            
            class Test {
                Map<String, String> map() {
                    return ImmutableMap.of();
                }
            }
        """
        val after = """
            import java.util.Map;
            
            class Test {
                Map<String, String> map() {
                    return Map.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun newClassWithMapArgument() {
        val dependsOn = """
            import java.util.Map;

            public class A {
                Map<String, String> map;
                public A(Map<String, String> map) {
                    this.map = map;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableMap;

            class Test {
                A a = new A(ImmutableMap.of());
            }
        """
        val after = """
            import java.util.Map;

            class Test {
                A a = new A(Map.of());
            }
        """

        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun methodInvocationWithMapArgument() {
        val dependsOn = """
            import java.util.Map;

            public class A {
                Map<String, String> map;
                public void method(Map<String, String> map) {
                    this.map = map;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableMap;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableMap.of());
                }
            }
        """
        val after = """
            import java.util.Map;

            class Test {
                void method() {
                    A a = new A();
                    a.method(Map.of());
                }
            }
        """

        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
        }
    }

    @Test
    fun variableIsMap() {
        val before = """
            import java.util.HashMap;
            import java.util.Map;
            import com.google.common.collect.ImmutableMap;

            class Test {
                Map<Integer, Map<String, String>> map = new HashMap<>();
                void setMap(String value) {
                    for (int i = 0; i < 10; i++) {
                        map.getOrDefault(i, ImmutableMap.of());
                    }
                }
            }
        """
        val after = """
            import java.util.HashMap;
            import java.util.Map;

            class Test {
                Map<Integer, Map<String, String>> map = new HashMap<>();
                void setMap(String value) {
                    for (int i = 0; i < 10; i++) {
                        map.getOrDefault(i, Map.of());
                    }
                }
            }
        """
        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        if (getJavaVersion().majorVersion >= 9) {
            Assertions.assertThat(result.isEmpty()).isFalse
            Assertions.assertThat(result[0].after!!.print()).isEqualTo(after)
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
