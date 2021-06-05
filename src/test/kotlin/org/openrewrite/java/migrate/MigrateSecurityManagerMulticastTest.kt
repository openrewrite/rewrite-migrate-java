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
package org.openrewrite.java.migrate

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.migrate.lang.MigrateSecurityManagerMulticast

class MigrateSecurityManagerMulticastTest: JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateSecurityManagerMulticast()

    @Test
    fun replaceCheckMulticast() = assertChanged(
        before = """
            package org.openrewrite;

            import java.net.InetAddress;
            import java.lang.SecurityManager;

            class Test {
                public void method() {
                    InetAddress maddr = InetAddress.getByName("127.0.0.1");
                    byte b = 100;
                    new SecurityManager().checkMulticast(maddr, b);
                }
            }
        """,
        after = """
            package org.openrewrite;

            import java.net.InetAddress;
            import java.lang.SecurityManager;

            class Test {
                public void method() {
                    InetAddress maddr = InetAddress.getByName("127.0.0.1");
                    byte b = 100;
                    new SecurityManager().checkMulticast(maddr);
                }
            }
        """
    )
}
