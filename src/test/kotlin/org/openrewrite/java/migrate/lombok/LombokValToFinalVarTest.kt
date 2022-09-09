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
package org.openrewrite.java.migrate.lombok

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions.*
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("RedundantOperationOnEmptyContainer", "StatementWithEmptyBody")
class LombokValToFinalVarTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(LombokValToFinalVar())
        spec.parser(JavaParser.fromJavaVersion().classpath("lombok"))
    }

    @Test
    fun replaceAssignment() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                class A {
                    void bar() {
                        val foo = "foo";
                    }
                }
                """,
                """
                class A {
                    void bar() {
                        final var foo = "foo";
                    }
                }
                """),
                17))

    @Test
    fun valInForEachStatement() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                import java.util.List;
                import java.util.ArrayList;
                
                class A {
                    void bar() {
                        List<String> lst = new ArrayList<>();
                        for (val s : lst) {}
                    }
                }
                """,
                """
                import java.util.List;
                import java.util.ArrayList;
                
                class A {
                    void bar() {
                        List<String> lst = new ArrayList<>();
                        for (final var s : lst) {}
                    }
                }
                """),
                17))

    @Test
    fun retainPrefixComment() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                class A {
                    void bar() {
                        // Prefix
                        val foo = "foo";
                    }
                }
                """,
                """
                class A {
                    void bar() {
                        // Prefix
                        final var foo = "foo";
                    }
                }
                """),
                17))

    @Test
    fun retainInfixComment() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                class A {
                    void bar() {
                        val foo = 
                        // Infix
                        "foo";
                    }
                }
                """,
                """
                class A {
                    void bar() {
                        final var foo = 
                        // Infix
                        "foo";
                    }
                }
                """),
                17))

    @Test
    fun retainSuffixComment() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                class A {
                    void bar() {
                        val foo = 43;
                        // Suffix
                    }
                }
                """,
                """
                class A {
                    void bar() {
                        final var foo = 43;
                        // Suffix
                    }
                }
                """),
                17))

    @Test
    fun retainWhitespace() = rewriteRun(
        version(
            java(
                """
                import lombok.val;
                class A {
                    void bar() {
                        val foo =
                            "foo";
                    }
                }
                """,
                """
                class A {
                    void bar() {
                        final var foo =
                            "foo";
                    }
                }
                """),
                17))

}
