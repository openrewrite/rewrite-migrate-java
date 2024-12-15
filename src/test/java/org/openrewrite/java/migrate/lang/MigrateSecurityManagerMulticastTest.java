/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("removal")
class MigrateSecurityManagerMulticastTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateSecurityManagerMulticast());
    }

    @DocumentExample
    @Test
    void migrateCheckMulticast() {
        //language=java
        rewriteRun(
          java(
            """
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
            """
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
        );
    }
}
