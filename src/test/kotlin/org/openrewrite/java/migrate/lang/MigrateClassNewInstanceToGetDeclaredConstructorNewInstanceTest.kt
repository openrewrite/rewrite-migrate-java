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