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
package org.openrewrite.java.migrate.net;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("resource")
class MigrateMulticastSocketSetTTLToSetTimeToLiveTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMulticastSocketSetTTLToSetTimeToLive());
    }

    @DocumentExample
    @Test
    void multicastSocketSetTTLToSetTimeToLive() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import java.net.MulticastSocket;

              public class Test {
                  public static void method() {
                      MulticastSocket s = new MulticastSocket(0);
                      s.setTTL((byte) 1);
                  }
              }
              """,
            """
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
        );
    }

    @Test
    void multicastSocketSetTTLToSetTimeToLiveFromOtherMethod() {
        //language=java
        rewriteRun(
          java(
            """
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
            """
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
        );
    }
}
