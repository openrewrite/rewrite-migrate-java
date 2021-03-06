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

class MigrateClassLoaderDefineClassTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateClassLoaderDefineClass()

    @Test
    fun migrateDefineClass() = assertChanged(
        before = """
            package org.openrewrite;

            class Test extends ClassLoader {
                public void method() {
                    byte[] b = new byte[]{};
                    super.defineClass(b, 10, 10);
                }
            }
        """,
        after = """
            package org.openrewrite;

            class Test extends ClassLoader {
                public void method() {
                    byte[] b = new byte[]{};
                    super.defineClass(null, b, 10, 10);
                }
            }
        """
    )
}
