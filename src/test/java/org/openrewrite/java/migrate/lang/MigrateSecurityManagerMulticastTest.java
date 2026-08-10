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
                  public void method() throws Exception {
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
                  public void method() throws Exception {
                      InetAddress maddr = InetAddress.getByName("127.0.0.1");
                      byte b = 100;
                      new SecurityManager().checkMulticast(maddr);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateWhenReceiverIsExactAndTimeToLiveIsConstantOrLocal() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr, byte parameterTtl) {
                      byte localTtl = 100;
                      new SecurityManager().checkMulticast(maddr, (byte) 100);
                      new SecurityManager().checkMulticast(maddr, parameterTtl);
                      new SecurityManager().checkMulticast(maddr, localTtl);
                      (new SecurityManager()).checkMulticast(address(maddr), localTtl);
                  }

                  InetAddress address(InetAddress maddr) {
                      return maddr;
                  }
              }
              """,
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr, byte parameterTtl) {
                      byte localTtl = 100;
                      new SecurityManager().checkMulticast(maddr);
                      new SecurityManager().checkMulticast(maddr);
                      new SecurityManager().checkMulticast(maddr);
                      (new SecurityManager()).checkMulticast(address(maddr));
                  }

                  InetAddress address(InetAddress maddr) {
                      return maddr;
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCallWhenReceiverMayBeASubclass() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(SecurityManager sm, InetAddress maddr, byte ttl) {
                      sm.checkMulticast(maddr, ttl);
                      sm.checkMulticast(maddr, (byte) 0);
                      manager(sm).checkMulticast(maddr, ttl);
                  }

                  SecurityManager manager(SecurityManager sm) {
                      return sm;
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCallWhenAnOverloadIsOverridden() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  static class SendingSecurityManager extends SecurityManager {
                      @Override
                      public void checkMulticast(InetAddress maddr) {
                          super.checkMulticast(maddr);
                      }
                  }

                  void method(InetAddress maddr) {
                      SendingSecurityManager sm = new SendingSecurityManager();
                      sm.checkMulticast(maddr, (byte) 0);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCallDelegatingToSuper() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class AuditingSecurityManager extends SecurityManager {
                  @Override
                  public void checkMulticast(InetAddress maddr, byte ttl) {
                      super.checkMulticast(maddr, ttl);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCallOnAnonymousSubclass() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr) {
                      new SecurityManager() {
                      }.checkMulticast(maddr, (byte) 0);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainMethodInvocation() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, nextTtl());
                      new SecurityManager().checkMulticast(maddr, (byte) nextTtl());
                  }

                  byte nextTtl() {
                      throw new IllegalStateException("evaluated");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainIncrement() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  byte ttl;

                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, ttl++);
                      new SecurityManager().checkMulticast(maddr, ++ttl);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArrayAccess() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  byte[] ttls = {1};
                  int index;

                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, ttls[index++]);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainUnboxing() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr, Byte boxedTtl) {
                      new SecurityManager().checkMulticast(maddr, boxedTtl);
                      new SecurityManager().checkMulticast(maddr, (byte) boxedTtl);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainFieldRead() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  byte ttl;
                  volatile byte volatileTtl;
                  static final byte DEFAULT_TTL = 1;

                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, ttl);
                      new SecurityManager().checkMulticast(maddr, this.ttl);
                      new SecurityManager().checkMulticast(maddr, volatileTtl);
                      new SecurityManager().checkMulticast(maddr, DEFAULT_TTL);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainAssignment() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  byte ttl;

                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, ttl = 1);
                      new SecurityManager().checkMulticast(maddr, ttl += 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainConditionalAndSwitch() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  void method(InetAddress maddr, boolean flag, int mode) {
                      new SecurityManager().checkMulticast(maddr, flag ? nextTtl() : (byte) 1);
                      new SecurityManager().checkMulticast(maddr, switch (mode) {
                          case 1 -> (byte) 1;
                          default -> nextTtl();
                      });
                  }

                  byte nextTtl() {
                      throw new IllegalStateException("evaluated");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainDivisionAndAllocation() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.InetAddress;

              class Test {
                  byte ttl;
                  int divisor;

                  void method(InetAddress maddr) {
                      new SecurityManager().checkMulticast(maddr, (byte) (ttl / divisor));
                      new SecurityManager().checkMulticast(maddr, new Test().ttl);
                  }
              }
              """
          )
        );
    }
}
