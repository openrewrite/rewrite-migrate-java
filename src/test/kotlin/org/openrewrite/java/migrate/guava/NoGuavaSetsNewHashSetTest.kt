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
package org.openrewrite.java.migrate.guava

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class NoGuavaSetsNewHashSetTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaSetsNewHashSet()

    @Test
    fun replaceWithNewHashSet() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.Set;
            
            class Test {
                Set<Integer> cardinalsWorldSeries = Sets.newHashSet();
            }
        """,
        after = """
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                Set<Integer> cardinalsWorldSeries = new HashSet<>();
            }
        """
    )

    @Test
    fun replaceWithNewHashSetIterable() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.Collections;
            import java.util.List;
            import java.util.Set;
            
            class Test {
                List<Integer> l = Collections.emptyList();
                Set<Integer> cardinalsWorldSeries = Sets.newHashSet(l);
            }
        """,
        after = """
            import java.util.Collections;
            import java.util.HashSet;
            import java.util.List;
            import java.util.Set;
            
            class Test {
                List<Integer> l = Collections.emptyList();
                Set<Integer> cardinalsWorldSeries = new HashSet<>(l);
            }
        """
    )

    @Test
    fun replaceWithNewHashSetVarargs() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.Set;
            
            class Test {
                Set<Integer> cardinalsWorldSeries = Sets.newHashSet(2006, 2011);
            }
        """,
        after = """
            import java.util.Arrays;
            import java.util.HashSet;
            import java.util.Set;
            
            class Test {
                Set<Integer> cardinalsWorldSeries = new HashSet<>(Arrays.asList(2006, 2011));
            }
        """
    )
}
