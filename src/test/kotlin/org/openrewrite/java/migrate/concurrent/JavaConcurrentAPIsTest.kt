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
package org.openrewrite.java.migrate.concurrent

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaRecipeTest

@Suppress("deprecation")
class JavaConcurrentAPIsTest : JavaRecipeTest {
    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.concurrent")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.concurrent.JavaConcurrentAPIs")

    @Test
    fun atomicBooleanWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicBoolean;
            public class Test {
                public boolean method() {
                    AtomicBoolean value = new AtomicBoolean();
                    return value.weakCompareAndSet(true, true);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicBoolean;
            public class Test {
                public boolean method() {
                    AtomicBoolean value = new AtomicBoolean();
                    return value.weakCompareAndSetPlain(true, true);
                }
            }
        """
    )

    @Test
    fun atomicIntegerWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicInteger;
            public class Test {
                public boolean method() {
                    AtomicInteger value = new AtomicInteger();
                    return value.weakCompareAndSet(0, 1);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicInteger;
            public class Test {
                public boolean method() {
                    AtomicInteger value = new AtomicInteger();
                    return value.weakCompareAndSetPlain(0, 1);
                }
            }
        """
    )

    @Test
    fun atomicIntegerArrayWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicIntegerArray;
            public class Test {
                public boolean method() {
                    AtomicIntegerArray value = new AtomicIntegerArray(2);
                    return value.weakCompareAndSet(0, 1, 2);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicIntegerArray;
            public class Test {
                public boolean method() {
                    AtomicIntegerArray value = new AtomicIntegerArray(2);
                    return value.weakCompareAndSetPlain(0, 1, 2);
                }
            }
        """
    )

    @Test
    fun atomicLongWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicLong;
            public class Test {
                public boolean method() {
                    AtomicLong value = new AtomicLong();
                    return value.weakCompareAndSet(0L, 1L);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicLong;
            public class Test {
                public boolean method() {
                    AtomicLong value = new AtomicLong();
                    return value.weakCompareAndSetPlain(0L, 1L);
                }
            }
        """
    )

    @Test
    fun atomicLongArrayWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicLongArray;
            public class Test {
                public boolean method() {
                    AtomicLongArray value = new AtomicLongArray(2);
                    return value.weakCompareAndSet(0, 1L, 2L);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicLongArray;
            public class Test {
                public boolean method() {
                    AtomicLongArray value = new AtomicLongArray(2);
                    return value.weakCompareAndSetPlain(0, 1L, 2L);
                }
            }
        """
    )

    @Test
    fun atomicReferenceWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicReference;
            public class Test {
                public boolean method() {
                    AtomicReference<Integer> value = new AtomicReference<>();
                    return value.weakCompareAndSet(0, 1);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicReference;
            public class Test {
                public boolean method() {
                    AtomicReference<Integer> value = new AtomicReference<>();
                    return value.weakCompareAndSetPlain(0, 1);
                }
            }
        """
    )

    @Test
    fun atomicReferenceArrayWeakCompareAndSet() = assertChanged(
        before = """
            import java.util.concurrent.atomic.AtomicReferenceArray;
            public class Test {
                public boolean method() {
                    AtomicReferenceArray<Integer> value = new AtomicReferenceArray<>(2);
                    return value.weakCompareAndSet(0, 1, 2);
                }
            }
        """,
        after = """
            import java.util.concurrent.atomic.AtomicReferenceArray;
            public class Test {
                public boolean method() {
                    AtomicReferenceArray<Integer> value = new AtomicReferenceArray<>(2);
                    return value.weakCompareAndSetPlain(0, 1, 2);
                }
            }
        """
    )
}
