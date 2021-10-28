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
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

@Suppress("deprecation")
class MigrateClassNewInstanceToGetDeclaredConstructorNewInstanceTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateClassNewInstanceToGetDeclaredConstructorNewInstance()

    @Test
    fun doesNotThrowExceptionOrThrowable() = assertUnchanged(
        before = """
            package com.abc;

            class A {
               public void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
                   Class<?> clazz = Class.forName("org.openrewrite.Test");
                   clazz.newInstance();
               }
            }
        """
    )

    @Test
    fun methodThrowsThrowable() = assertChanged(
        before = """
            package com.abc;

            class A {
               public void test() throws Throwable {
                   Class<?> clazz = Class.forName("org.openrewrite.Test");
                   clazz.newInstance();
               }
            }
        """,
        after = """
            package com.abc;

            class A {
               public void test() throws Throwable {
                   Class<?> clazz = Class.forName("org.openrewrite.Test");
                   clazz.getDeclaredConstructor().newInstance();
               }
            }
        """
    )

    @Test
    fun tryBlockCatchesException() = assertChanged(
        before = """
            package com.abc;

            class A {
                public void test() {
                    try {
                        Class<?> clazz = Class.forName("org.openrewrite.Test");
                        clazz.newInstance();
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        """,
        after = """
            package com.abc;

            class A {
                public void test() {
                    try {
                        Class<?> clazz = Class.forName("org.openrewrite.Test");
                        clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        """
    )
}