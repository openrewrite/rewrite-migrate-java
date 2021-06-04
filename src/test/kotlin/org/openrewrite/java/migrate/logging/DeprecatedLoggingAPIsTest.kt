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
package org.openrewrite.java.migrate.logging

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaRecipeTest

/**
 * Holds the declarative tests for the deprecated-logging-apis.yml composite recipe.
 */
class DeprecatedLoggingAPIsTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.DeprecatedLoggingAPIs")

    @Test
    fun loggingMXBeanInterfaceToPlatformLoggingMXBeanInterface() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.util.logging.LoggingMXBean;

            public class B {
                public static void method() {
                    LoggingMXBean loggingBean = null;
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.lang.management.PlatformLoggingMXBean;

            public class B {
                public static void method() {
                    PlatformLoggingMXBean loggingBean = null;
                }
            }
        """
    )

}
