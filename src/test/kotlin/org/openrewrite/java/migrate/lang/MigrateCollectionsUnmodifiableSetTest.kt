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

class MigrateCollectionsUnmodifiableSetTest: JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateCollectionsUnmodifiableSet()

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/67")
    @Test
    fun unmodifiableSet() = assertChanged(
        before = """
            import java.util.*;
            
            class Test {
                Set<Integer> s = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(1, 2, 3)));
            }
        """,
        after = """
            import java.util.Set;
            
            class Test {
                Set<Integer> s = Set.of(1, 2, 3);
            }
        """
    )
}
