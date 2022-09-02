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
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.Assertions.version
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

@Suppress("Java9CollectionFactory")
class MigrateCollectionsUnmodifiableListTest : RewriteTest {
    override fun defaults(spec: RecipeSpec) {
        spec.recipe(MigrateCollectionsUnmodifiableList())
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/67")
    @Test
    fun unmodifiableList() = rewriteRun(
        version(
            java("""
            import java.util.*;
            
            class Test {
                List<Integer> l = Collections.unmodifiableList(Arrays.asList(1, 2, 3));
            }
        """,
        """
            import java.util.List;
            
            class Test {
                List<Integer> l = List.of(1, 2, 3);
            }
        """), 9)
    )

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/67")
    @Test
    fun unmodifiableListTyped() = rewriteRun(
        version(
            java("""
            import java.util.*;
            import java.time.LocalDate;
            
            class Test {
                List<LocalDate> s = Collections.unmodifiableList(Arrays.asList(LocalDate.of(2010,1,1),LocalDate.now()));
            }
        """,
        """
            import java.util.List;
            import java.time.LocalDate;
            
            class Test {
                List<LocalDate> s = List.of(LocalDate.of(2010, 1, 1), LocalDate.now());
            }
        """), 9)
    )
}
