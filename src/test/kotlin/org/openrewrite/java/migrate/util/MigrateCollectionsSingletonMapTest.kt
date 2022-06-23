/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.util

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

class MigrateCollectionsSingletonMapTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateCollectionsSingletonMap()

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    fun singletonMap() = assertChanged(
        before = """
            import java.util.*;
            
            class Test {
                Map<String,String> set = Collections.singletonMap("hello", "world");
            }
        """,
        after = """
            import java.util.Map;
            
            class Test {
                Map<String,String> set = Map.of("hello", "world");
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    fun singletonMapCustomType() = assertChanged(
        before = """
            import java.util.*;
            import java.time.LocalDate;
            
            class Test {
                Map<String,LocalDate> map = Collections.singletonMap("date", LocalDate.now());
            }
        """,
        after = """
            import java.util.Map;
            import java.time.LocalDate;
            
            class Test {
                Map<String,LocalDate> map = Map.of("date", LocalDate.now());
            }
        """
    )

}
