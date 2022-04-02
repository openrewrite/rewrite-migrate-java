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
package org.openrewrite.java.migrate.lang

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

class MigrateCollectionsSingletonListTest: JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateCollectionsSingletonList()

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    fun singletonList() = assertChanged(
        before = """
            import java.util.*;
            
            class Test {
                List<String> list = Collections.singletonList("ABC");
            }
        """,
        after = """
            import java.util.List;
            
            class Test {
                List<String> list = List.of("ABC");
            }
        """
    )

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    fun singletonListCustomType() = assertChanged(
        before = """
            import java.util.*;
            import java.time.LocalDate;
            
            class Test {
                List<LocalDate> list = Collections.singletonList(LocalDate.now());
            }
        """,
        after = """
            import java.util.List;
            import java.time.LocalDate;
            
            class Test {
                List<LocalDate> list = List.of(LocalDate.now());
            }
        """
    )

}
