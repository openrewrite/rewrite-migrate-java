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

import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.tree.J
import org.openrewrite.loadRecipeFromClasspath

class JaxWSToJakartaTest : JavaRecipeTest {
    override val parser: Parser<J.CompilationUnit> = JavaParser.fromJavaVersion()
        .classpath("javax", "jakarta")
        .build()

    override val recipe = loadRecipeFromClasspath(
        "org.openrewrite.java.migrate.jakarta.JaxWSMigrationToJakarta"
    )

    @Test
    fun test() = assertChanged(
        before = """
            package javax.xml.ws;

            public @interface Action {
            }
        """,
        after = """
            package jakarta.xml.ws;

            public @interface Action {
            }
        """
    )

}
