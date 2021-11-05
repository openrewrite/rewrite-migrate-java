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

class NoGuavaImmutableSetOfTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaImmutableSetOf()

    @Test
    fun doNotChangeReturnsImmutableSet() {
        val before = """
            import com.google.common.collect.ImmutableSet;

            class Test {
                ImmutableSet<String> getSet() {
                    return ImmutableSet.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeFieldAssignmentToImmutableSet() {
        val before = """
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                ImmutableSet<String> m;
            
                {
                    this.m = ImmutableSet.of();
                }
            }
        """

        val result = runRecipe(arrayOf(before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeAssignsToImmutableSet() {
        val before = """
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                ImmutableSet<String> m;
            
                void init() {
                    m = ImmutableSet.of();
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
            import com.google.common.collect.ImmutableSet;

            public class A {
                ImmutableSet<String> immutableSet;
                public A(ImmutableSet<String> immutableSet) {
                    this.immutableSet = immutableSet;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableSet;

            class Test {
                A a = new A(ImmutableSet.of());
            }
        """
        val result = runRecipe(arrayOf(dependsOn, before))
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result.isEmpty()).isTrue
    }

    @Test
    fun doNotChangeMethodInvocation() {
        val dependsOn = """
            import com.google.common.collect.ImmutableSet;

            public class A {
                ImmutableSet<String> immutableSet;
                public void method(ImmutableSet<String> immutableSet) {
                    this.immutableSet = immutableSet;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableSet;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableSet.of());
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
            import java.util.Set;
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                Set<String> m = ImmutableSet.of("A", "B", "C", "D");
            }
        """
        val after = """
            import java.util.Set;
            
            class Test {
                Set<String> m = Set.of("A", "B", "C", "D");
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
    fun fieldAssignmentToSet() {
        val before = """
            import java.util.Set;
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                Set<String> m;
                {
                    this.m = ImmutableSet.of();
                }
            }
        """
        val after = """
            import java.util.Set;
            
            class Test {
                Set<String> m;
                {
                    this.m = Set.of();
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
    fun assigmentToSet() {
        val before = """
            import java.util.Set;
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                Set<String> m = ImmutableSet.of();
            }
        """
        val after = """
            import java.util.Set;
            
            class Test {
                Set<String> m = Set.of();
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
    fun returnsSet() {
        val before = """
            import java.util.Set;
            import com.google.common.collect.ImmutableSet;
            
            class Test {
                Set<String> set() {
                    return ImmutableSet.of();
                }
            }
        """
        val after = """
            import java.util.Set;
            
            class Test {
                Set<String> set() {
                    return Set.of();
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
    fun newClassWithSetArgument() {
        val dependsOn = """
            import java.util.Set;

            public class A {
                Set<String> set;
                public A(Set<String> set) {
                    this.set = set;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableSet;

            class Test {
                A a = new A(ImmutableSet.of());
            }
        """
        val after = """
            import java.util.Set;

            class Test {
                A a = new A(Set.of());
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
    fun methodInvocationWithSetArgument() {
        val dependsOn = """
            import java.util.Set;

            public class A {
                Set<String> set;
                public void method(Set<String> set) {
                    this.set = set;
                }
            }
        """
        val before = """
            import com.google.common.collect.ImmutableSet;

            class Test {
                void method() {
                    A a = new A();
                    a.method(ImmutableSet.of());
                }
            }
        """
        val after = """
            import java.util.Set;

            class Test {
                void method() {
                    A a = new A();
                    a.method(Set.of());
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
