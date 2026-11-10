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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class RemoveSystemSecurityManagerChecksTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveSystemSecurityManagerChecks());
    }

    @DocumentExample
    @Test
    void removeCheckOnSystemSecurityManager() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkRead(file);
                      }
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckCalledDirectlyOnSystemSecurityManager() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file) {
                      System.getSecurityManager().checkRead(file);
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                  }
              }
              """
          )
        );
    }

    @Test
    void keepVariableUsedOtherwise() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkRead(file);
                      }
                      System.out.println(sm);
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      System.out.println(sm);
                  }
              }
              """
          )
        );
    }

    @Test
    void keepVariableUsedInInitializer() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkRead(file);
                      }
                      Object o = sm;
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      Object o = sm;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepAssignmentInConditionWhenVariableIsReadInOuterBlock() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file, boolean flag) {
                      SecurityManager sm;
                      if (flag) {
                          if ((sm = System.getSecurityManager()) != null) {
                              sm.checkRead(file);
                          }
                      } else {
                          sm = System.getSecurityManager();
                      }
                      System.out.println(sm);
                  }
              }
              """,
            """
              class Test {
                  void read(String file, boolean flag) {
                      SecurityManager sm;
                      if (flag) {
                          if ((sm = System.getSecurityManager()) != null) {
                          }
                      } else {
                          sm = System.getSecurityManager();
                      }
                      System.out.println(sm);
                  }
              }
              """
          )
        );
    }

    @Test
    void keepNullCheckWithElse() {
        rewriteRun(
          java(
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkRead(file);
                      } else {
                          System.out.println(file);
                      }
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                      } else {
                          System.out.println(file);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckOnVar() {
        rewriteRun(
          java(
            """
              class Test {
                  void accept(String host, int port) {
                      var sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkAccept(host, port);
                      }
                  }
              }
              """,
            """
              class Test {
                  void accept(String host, int port) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckOnVariableAssignedInCondition() {
        rewriteRun(
          java(
            """
              class Test {
                  void connect(String host, int port) {
                      SecurityManager sm;
                      if ((sm = System.getSecurityManager()) != null) {
                          sm.checkConnect(host, port);
                      }
                  }
              }
              """,
            """
              class Test {
                  void connect(String host, int port) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckInStaticInitializer() {
        rewriteRun(
          java(
            """
              class Test {
                  static {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkExit(0);
                      }
                  }
              }
              """,
            """
              class Test {
                  static {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckOnStaticFinalField() {
        rewriteRun(
          java(
            """
              class Test {
                  private static final SecurityManager SM = System.getSecurityManager();

                  void exit(int status) {
                      if (SM != null) {
                          SM.checkExit(status);
                      }
                  }
              }
              """,
            """
              class Test {
                  private static final SecurityManager SM = System.getSecurityManager();

                  void exit(int status) {
                      if (SM != null) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckOnFieldAccessedThroughThis() {
        rewriteRun(
          java(
            """
              class Test {
                  private final SecurityManager sm = System.getSecurityManager();

                  void exit(int status) {
                      this.sm.checkExit(status);
                  }
              }
              """,
            """
              class Test {
                  private final SecurityManager sm = System.getSecurityManager();

                  void exit(int status) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckOnFieldAssignedInConstructor() {
        rewriteRun(
          java(
            """
              class Test {
                  private SecurityManager sm;

                  Test() {
                      this.sm = System.getSecurityManager();
                  }

                  void exit(int status) {
                      sm.checkExit(status);
                  }
              }
              """,
            """
              class Test {
                  private SecurityManager sm;

                  Test() {
                      this.sm = System.getSecurityManager();
                  }

                  void exit(int status) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeCheckInAnonymousClass() {
        rewriteRun(
          java(
            """
              class Test {
                  private final SecurityManager sm = System.getSecurityManager();

                  Runnable exit(int status) {
                      return new Runnable() {
                          @Override
                          public void run() {
                              sm.checkExit(status);
                          }
                      };
                  }
              }
              """,
            """
              class Test {
                  private final SecurityManager sm = System.getSecurityManager();

                  Runnable exit(int status) {
                      return new Runnable() {
                          @Override
                          public void run() {
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void removeOnlySystemChecksAsPartOfRemoveSecurityManager() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.RemoveSecurityManager")
            .allSources(s -> s.markers(javaVersion(25))),
          java(
            """
              class Test {
                  void read(SecurityManager policy, String file) {
                      SecurityManager sm = System.getSecurityManager();
                      if (sm != null) {
                          sm.checkRead(file);
                      }
                      policy.checkRead(file);
                  }
              }
              """,
            """
              class Test {
                  void read(SecurityManager policy, String file) {
                      policy.checkRead(file);
                  }
              }
              """
          )
        );
    }

    @Test
    void removeDirectCheckBeforeReplacingSystemSecurityManagerWithNull() {
        rewriteRun(
          spec -> spec.recipeFromResources(
              "org.openrewrite.java.migrate.RemoveSecurityManager",
              "org.openrewrite.java.migrate.SystemGetSecurityManagerToNull")
            .allSources(s -> s.markers(javaVersion(25))),
          java(
            """
              class Test {
                  void read(String file) {
                      System.getSecurityManager().checkRead(file);
                  }
              }
              """,
            """
              class Test {
                  void read(String file) {
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void keepCheckOnParameter() {
            rewriteRun(
              java(
                """
                  class Test {
                      SecurityManager system() {
                          return System.getSecurityManager();
                      }

                      void requireRead(SecurityManager sm, String file) {
                          sm.checkRead(file);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnFieldAssignedFromConstructorParameter() {
            rewriteRun(
              java(
                """
                  import java.security.Permission;

                  class PermissionPolicy {
                      private final SecurityManager securityManager;

                      PermissionPolicy(SecurityManager securityManager) {
                          this.securityManager = securityManager;
                      }

                      static PermissionPolicy ofSystem() {
                          return new PermissionPolicy(System.getSecurityManager());
                      }

                      boolean isAllowed(Permission permission) {
                          try {
                              securityManager.checkPermission(permission);
                              return true;
                          } catch (SecurityException e) {
                              return false;
                          }
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnParameterReassignedFromSystemSecurityManager() {
            rewriteRun(
              java(
                """
                  class Test {
                      void read(SecurityManager sm, String file) {
                          if (sm == null) {
                              sm = System.getSecurityManager();
                          }
                          sm.checkRead(file);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnLocalReassignedFromParameter() {
            rewriteRun(
              java(
                """
                  class Test {
                      void read(String file, SecurityManager fallback) {
                          SecurityManager sm = System.getSecurityManager();
                          if (sm == null) {
                              sm = fallback;
                          }
                          sm.checkRead(file);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnPackagePrivateField() {
            rewriteRun(
              java(
                """
                  class Test {
                      SecurityManager sm = System.getSecurityManager();

                      void read(String file) {
                          sm.checkRead(file);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnFieldReassignedBySetter() {
            rewriteRun(
              java(
                """
                  class Test {
                      private SecurityManager sm = System.getSecurityManager();

                      void setSecurityManager(SecurityManager sm) {
                          this.sm = sm;
                      }

                      void exit(int status) {
                          sm.checkExit(status);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnAnnotatedField() {
            rewriteRun(
              spec -> spec.parser(JavaParser.fromJavaVersion().classpath("lombok")),
              java(
                """
                  import lombok.Setter;

                  class Test {
                      @Setter
                      private SecurityManager sm = System.getSecurityManager();

                      void exit(int status) {
                          sm.checkExit(status);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void keepCheckOnFieldOfLombokAnnotatedClass() {
            rewriteRun(
              spec -> spec.parser(JavaParser.fromJavaVersion().classpath("lombok")),
              java(
                """
                  import lombok.Setter;

                  @Setter
                  class Test {
                      private SecurityManager sm = System.getSecurityManager();

                      void exit(int status) {
                          sm.checkExit(status);
                      }
                  }
                  """
              )
            );
        }
    }
}
