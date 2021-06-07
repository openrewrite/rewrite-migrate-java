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
package org.openrewrite.java.migrate.net

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

class MigrateMulticastSocketSetTTLToSetTimeToLiveTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateMulticastSocketSetTTLToSetTimeToLive()

    @Test
    fun multicastSocketSetTTLToSetTimeToLive() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.net.MulticastSocket;

            public class Test {
                public static void method() {
                    MulticastSocket s = new MulticastSocket(0);
                    s.setTTL((byte) 1);
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.net.MulticastSocket;

            public class Test {
                public static void method() {
                    MulticastSocket s = new MulticastSocket(0);
                    s.setTimeToLive(Byte.valueOf((byte) 1).intValue());
                }
            }
        """
    )

    @Test
    fun multicastSocketSetTTLToSetTimeToLiveFromOtherMethod() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.net.MulticastSocket;

            public class Test {
                public static byte takeByte() {
                    return 127;
                }

                public static void method() {
                    MulticastSocket s = new MulticastSocket(0);
                    s.setTTL(takeByte());
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.net.MulticastSocket;

            public class Test {
                public static byte takeByte() {
                    return 127;
                }

                public static void method() {
                    MulticastSocket s = new MulticastSocket(0);
                    s.setTimeToLive(Byte.valueOf(takeByte()).intValue());
                }
            }
        """
    )

}
