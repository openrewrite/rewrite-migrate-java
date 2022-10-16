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
package org.openrewrite.java.migrate.lang

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.java.Assertions.*
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation

@Suppress("RedundantStringFormatCall")
class StringFormattedTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(StringFormatted())
        spec.typeValidationOptions(TypeValidation().methodInvocations(false))
        spec.parser(JavaParser.fromJavaVersion())
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun oneArgument() = rewriteRun(
        version(
            java("""
                        class A {
                            String str = String.format("foo");
                        }
                    """,
                """
                        class A {
                            String str = "foo".formatted();
                        }
                    """),
            17)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun twoArguments() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s", "a");
                }
            """,
            """
                class A {
                    String str = "foo %s".formatted("a");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun threeArguments() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s %d", "a", 1);
                }
            """,
            """
                class A {
                    String str = "foo %s %d".formatted("a", 1);
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun fourArguments() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s %d %f", "a", 1, 2.0);
                }
            """,
            """
                class A {
                    String str = "foo %s %d %f".formatted("a", 1, 2.0);
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun splitFirstArgument() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo " + "%s", "a");
                }
            """,
            """
                class A {
                    String str = ("foo " + "%s").formatted("a");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun splitSecondArgument() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s", "a" + "b");
                }
            """,
            """
                class A {
                    String str = "foo %s".formatted("a" + "b");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun doNotWrapMethodInvocation() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format(someMethod(), "a");
                    String someMethod() {
                        return "foo %s";
                    }
                }
            """,
            """
                class A {
                    String str = someMethod().formatted("a");
                    String someMethod() {
                        return "foo %s";
                    }
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun doNotWrapLocalVariable() = rewriteRun(
        version(
            java("""
                class A {
                    String someMethod() {
                        String fmt = "foo %s";
                        String str = String.format(fmt, "a");
                    }
                }
            """,
            """
                class A {
                    String someMethod() {
                        String fmt = "foo %s";
                        String str = fmt.formatted("a");
                    }
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun doNotWrapField() = rewriteRun(
        version(
            java("""
                class A {
                    static final String fmt = "foo %s";
                    String str = String.format(fmt, "a");
                }
            """,
            """
                class A {
                    static final String fmt = "foo %s";
                    String str = fmt.formatted("a");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun removeStaticImport() = rewriteRun(
        version(
            java("""
                import static java.lang.String.format;
                class A {
                    String str = format("foo %s", "a");
                }
            """,
            """
                class A {
                    String str = "foo %s".formatted("a");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    fun doNotMatchLocale() = rewriteRun(
        version (
            java("""
                import java.util.Locale;
                class A {
                    String str = String.format(Locale.US, "foo %s", "a");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    fun doNotChangeLackOfWhitespace() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s %s","a","b");
                }
            """,
            """
                class A {
                    String str = "foo %s %s".formatted("a", "b");
                }
            """), 17
        )
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    fun doNotChangeWhitespaceWithNewlines() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s %s",
                        "a",
                        "b");
                }
            """,
            """
                class A {
                    String str = "foo %s %s".formatted(
                            "a",
                            "b");
                }
            """), 17)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    fun doNotChangeWhitespaceWithNewlinesAndComments() = rewriteRun(
        version(
            java("""
                class A {
                    String str = String.format("foo %s %s",
                        "a",
                        // B
                        "b");
                }
            """,
            """
                class A {
                    String str = "foo %s %s".formatted(
                            "a",
                            // B
                            "b");
                }
            """), 17
        )
    )

}
