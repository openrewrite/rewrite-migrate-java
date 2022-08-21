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
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class StringFormattedTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(StringFormatted())
        spec.parser(JavaParser.fromJavaVersion().build())
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun oneArgument() = rewriteRun(
        java("""
            class A {
                String str = String.format("foo");
            }
        """,
        """
            class A {
                String str = "foo".formatted();
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun twoArguments() = rewriteRun(
        java("""
            class A {
                String str = String.format("foo %s", "a");
            }
        """,
        """
            class A {
                String str = "foo %s".formatted("a");
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun threeArguments() = rewriteRun(
        java("""
            class A {
                String str = String.format("foo %s %d", "a", 1);
            }
        """,
        """
            class A {
                String str = "foo %s %d".formatted("a", 1);
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun fourArguments() = rewriteRun(
        java("""
            class A {
                String str = String.format("foo %s %d %f", "a", 1, 2.0);
            }
        """,
        """
            class A {
                String str = "foo %s %d %f".formatted("a", 1, 2.0);
            }
        """)
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    fun splitFirstArgument() = rewriteRun(
        java("""
            class A {
                String str = String.format("foo " + "%s", "a");
            }
        """,
        """
            class A {
                String str = ("foo " + "%s").formatted("a");
            }
        """)
    )

}
