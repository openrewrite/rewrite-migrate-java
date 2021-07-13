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

class NoGuavaMapsNewLinkedHashMapTest: JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaMapsNewLinkedHashMap()

    @Test
    fun replaceWithNewLinkedHashMap() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> cardinalsWorldSeries = Maps.newLinkedHashMap();
            }
        """,
        after = """
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> cardinalsWorldSeries = new LinkedHashMap<>();
            }
        """
    )

    @Test
    fun replaceWithNewLinkedHashMapWithMap() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.Collections;
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> m = Collections.emptyMap();
                Map<Integer, Integer> cardinalsWorldSeries = Maps.newLinkedHashMap(m);
            }
        """,
        after = """
            import java.util.Collections;
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> m = Collections.emptyMap();
                Map<Integer, Integer> cardinalsWorldSeries = new LinkedHashMap<>(m);
            }
        """
    )

    @Test
    fun replaceWithNewLinkedHashMapWithCapacity() = assertChanged(
        before = """
            import com.google.common.collect.*;
            
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> cardinalsWorldSeries = Maps.newLinkedHashMapWithExpectedSize(2);
            }
        """,
        after = """
            import java.util.LinkedHashMap;
            import java.util.Map;
            
            class Test {
                Map<Integer, Integer> cardinalsWorldSeries = new LinkedHashMap<>(2);
            }
        """
    )
}
