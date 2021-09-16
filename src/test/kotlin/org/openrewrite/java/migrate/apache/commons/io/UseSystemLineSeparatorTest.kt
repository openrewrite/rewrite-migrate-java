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
package org.openrewrite.java.migrate.apache.commons.io

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class UseSystemLineSeparatorTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .classpath("commons-io")
        .build()

    override val recipe: Recipe =
        UseSystemLineSeparator()

    @Test
    fun migratesQualifiedField() = assertChanged(
        before = """
            import org.apache.commons.io.IOUtils;
            class A {
                public String lineSeparator() {
                    return IOUtils.LINE_SEPARATOR;
                }
            }
        """,
        after = """
            class A {
                public String lineSeparator() {
                    return System.lineSeparator();
                }
            }
        """
    )

    @Test
    fun migratesStaticImportedField() = assertChanged(
        before = """
            import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
            class A {
                public String lineSeparator() {
                    return LINE_SEPARATOR;
                }
            }
        """,
        after = """
            class A {
                public String lineSeparator() {
                    return System.lineSeparator();
                }
            }
        """
    )

    @Test
    fun ignoreUnrelatedFields() = assertUnchanged(
        before = """
            class A {
                private static final String LINE_SEPARATOR = System.lineSeparator();
                public String lineSeparator() {
                    return LINE_SEPARATOR;
                }
            }
        """
    )
}
