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

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaRecipeTest

@Suppress("deprecation")
class JavaLangAPIsTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.lang")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.lang.JavaLangAPIs")

    @Test
    fun characterIsJavaLetterToIsJavaIdentifierStart() = assertChanged(
        before = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isJavaLetter('b');
               }
            }
        """,
        after = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isJavaIdentifierStart('b');
               }
            }
        """
    )

    @Test
    fun characterIsJavaLetterOrDigitToIsJavaIdentifierPart() = assertChanged(
        before = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isJavaLetterOrDigit('b');
               }
            }
        """,
        after = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isJavaIdentifierPart('b');
               }
            }
        """
    )

    @Test
    fun characterIsSpaceToIsWhitespace() = assertChanged(
        before = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isSpace('b');
               }
            }
        """,
        after = """
            package com.abc;

            class A {
               public void test() {
                   boolean result = Character.isWhitespace('b');
               }
            }
        """
    )

    @Disabled
    @Test
    fun classNewInstanceToGetDeclaredConstructorNewInstance() = assertChanged(
        before = """
            package com.abc;

            class A {
               public void test() {
                   Class<?> clazz = Class.forName("org.openrewrite.Test");
                   clazz.newInstance();
               }
            }
        """,
        after = """
            package com.abc;

            class A {
               public void test() {
                   Class<?> clazz = Class.forName("org.openrewrite.Test");
                   clazz.getDeclaredConstructor().newInstance();
               }
            }
        """
    )

    @Test
    fun runtimeVersionMajorToFeature() = assertChanged(
        before = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.major();
                }
            }
        """,
        after = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.feature();
                }
            }
        """
    )

    @Test
    fun runtimeVersionMinorToInterim() = assertChanged(
        before = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.minor();
                }
            }
        """,
        after = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.interim();
                }
            }
        """
    )

    @Test
    fun runtimeVersionSecurityToUpdate() = assertChanged(
        before = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.security();
                }
            }
        """,
        after = """
            package com.abc;

            import java.lang.Runtime.Version;
            class A {
                public void test() {
                    Runtime.Version runtimeVersion = Runtime.version();
                    int version = runtimeVersion.update();
                }
            }
        """
    )

}
