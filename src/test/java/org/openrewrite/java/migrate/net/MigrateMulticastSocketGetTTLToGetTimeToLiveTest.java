/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateMulticastSocketGetTTLToGetTimeToLiveTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMulticastSocketGetTTLToGetTimeToLive());
    }

    @DocumentExample
    @Test
    void castBackToByteWhenResultIsUsed() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.MulticastSocket;

              class Test {
                  byte read(MulticastSocket s) throws Exception {
                      byte ttl = s.getTTL();
                      return ttl;
                  }
              }
              """,
            """
              import java.net.MulticastSocket;

              class Test {
                  byte read(MulticastSocket s) throws Exception {
                      byte ttl = (byte) s.getTimeToLive();
                      return ttl;
                  }
              }
              """
          )
        );
    }

    @Test
    void castBackToByteWhenPassedAsArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.MulticastSocket;

              class Test {
                  void copy(MulticastSocket from, MulticastSocket to) throws Exception {
                      to.setTTL(from.getTTL());
                  }
              }
              """,
            """
              import java.net.MulticastSocket;

              class Test {
                  void copy(MulticastSocket from, MulticastSocket to) throws Exception {
                      to.setTTL((byte) from.getTimeToLive());
                  }
              }
              """
          )
        );
    }

    @Test
    void keepSignExtensionWhenWidenedToInt() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.MulticastSocket;

              class Test {
                  int read(MulticastSocket s) throws Exception {
                      return s.getTTL();
                  }
              }
              """,
            """
              import java.net.MulticastSocket;

              class Test {
                  int read(MulticastSocket s) throws Exception {
                      return (byte) s.getTimeToLive();
                  }
              }
              """
          )
        );
    }

    @Test
    void renameOnlyWhenResultIsDiscarded() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.MulticastSocket;

              class Test {
                  void read(MulticastSocket s, boolean b) throws Exception {
                      s.getTTL();
                      if (b) s.getTTL();
                  }
              }
              """,
            """
              import java.net.MulticastSocket;

              class Test {
                  void read(MulticastSocket s, boolean b) throws Exception {
                      s.getTimeToLive();
                      if (b) s.getTimeToLive();
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void lambdaBody() {
            //language=java
            rewriteRun(
              java(
                """
                  import java.net.MulticastSocket;
                  import java.util.concurrent.Callable;

                  class Test {
                      Callable<Byte> reader(MulticastSocket s) {
                          return () -> s.getTTL();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void methodReference() {
            //language=java
            rewriteRun(
              java(
                """
                  import java.io.IOException;
                  import java.net.MulticastSocket;

                  class Test {
                      interface TtlReader {
                          byte read(MulticastSocket s) throws IOException;
                      }

                      TtlReader reader() {
                          return MulticastSocket::getTTL;
                      }
                  }
                  """
              )
            );
        }
    }
}
