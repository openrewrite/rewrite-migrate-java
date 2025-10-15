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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindImmutableThreadLocalVariablesCrossFileTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindImmutableThreadLocalVariables());
    }

    @DocumentExample
    @Test
    void detectMutationFromAnotherClassInSamePackage() {
        rewriteRun(
          // First class with package-private ThreadLocal
          java(
            """
              package com.example;

              class ThreadLocalHolder {
                  static final ThreadLocal<String> SHARED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return SHARED_TL.get();
                  }
              }
              """
          ),
          // Second class that mutates the ThreadLocal
          java(
            """
              package com.example;

              class ThreadLocalMutator {
                  public void mutate() {
                      ThreadLocalHolder.SHARED_TL.set("mutated");
                  }

                  public void cleanup() {
                      ThreadLocalHolder.SHARED_TL.remove();
                  }
              }
              """
          )
        );
        // The ThreadLocal should NOT be marked as immutable because it's mutated in ThreadLocalMutator
    }

    @Test
    void detectNoMutationAcrossMultipleClasses() {
        rewriteRun(
          java(
            """
              package com.example;

              public class ReadOnlyHolder {
                  public static final ThreadLocal<Integer> COUNTER = ThreadLocal.withInitial(() -> 0);

                  public int getCount() {
                      return COUNTER.get();
                  }
              }
              """,
            """
              package com.example;

              public class ReadOnlyHolder {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated in project (but accessible from outside due to non-private access))~~>*/public static final ThreadLocal<Integer> COUNTER = ThreadLocal.withInitial(() -> 0);

                  public int getCount() {
                      return COUNTER.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class Reader1 {
                  public void readValue() {
                      Integer value = ReadOnlyHolder.COUNTER.get();
                      System.out.println(value);
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class Reader2 {
                  public int calculate() {
                      return ReadOnlyHolder.COUNTER.get() + 10;
                  }
              }
              """
          )
        );
        // The ThreadLocal should be marked with a warning since it's public but never mutated
    }

    @Test
    void detectMutationThroughInheritance() {
        rewriteRun(
          java(
            """
              package com.example;

              public class BaseClass {
                  protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example.sub;

              import com.example.BaseClass;

              public class SubClass extends BaseClass {
                  public void modifyThreadLocal() {
                      PROTECTED_TL.set("modified by subclass");
                  }
              }
              """
          )
        );
        // The ThreadLocal should NOT be marked as immutable because it's mutated in SubClass
    }

    @Test
    void privateThreadLocalNotAccessibleFromOtherClass() {
        rewriteRun(
          java(
            """
              package com.example;

              public class PrivateHolder {
                  private static final ThreadLocal<String> PRIVATE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PRIVATE_TL.get();
                  }

                  public static class NestedClass {
                      public void tryToAccess() {
                          // Can access private field from nested class
                          String value = PRIVATE_TL.get();
                      }
                  }
              }
              """,
            """
              package com.example;

              public class PrivateHolder {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<String> PRIVATE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PRIVATE_TL.get();
                  }

                  public static class NestedClass {
                      public void tryToAccess() {
                          // Can access private field from nested class
                          String value = PRIVATE_TL.get();
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class ExternalClass {
                  public void cannotAccess() {
                      // Cannot access private ThreadLocal from PrivateHolder
                      // This class cannot mutate PRIVATE_TL
                  }
              }
              """
          )
        );
        // Private ThreadLocal should be marked as immutable since it can't be mutated externally
    }

    @Test
    void detectMutationInNestedClass() {
        rewriteRun(
          java(
            """
              package com.example;

              public class OuterClass {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public String getValue() {
                      return TL.get();
                  }

                  public static class InnerClass {
                      public void mutate() {
                          TL.set("mutated by inner class");
                      }
                  }
              }
              """
          )
        );
        // ThreadLocal should NOT be marked as immutable because it's mutated in InnerClass
    }

    @Test
    void detectMutationThroughStaticImport() {
        rewriteRun(
          java(
            """
              package com.example;

              public class ThreadLocalProvider {
                  public static final ThreadLocal<String> STATIC_TL = new ThreadLocal<>();
              }
              """
          ),
          java(
            """
              package com.example.user;

              import static com.example.ThreadLocalProvider.STATIC_TL;

              public class StaticImportUser {
                  public void mutate() {
                      STATIC_TL.set("mutated through static import");
                  }
              }
              """
          )
        );
        // ThreadLocal should NOT be marked as immutable due to mutation through static import
    }

    @Test
    void multipleThreadLocalsWithMixedAccessPatterns() {
        rewriteRun(
          java(
            """
              package com.example;

              public class MultipleThreadLocals {
                  private static final ThreadLocal<String> PRIVATE_IMMUTABLE = new ThreadLocal<>();
                  static final ThreadLocal<String> PACKAGE_MUTATED = new ThreadLocal<>();
                  public static final ThreadLocal<String> PUBLIC_READ_ONLY = new ThreadLocal<>();
                  protected static final ThreadLocal<String> PROTECTED_MUTATED = new ThreadLocal<>();

                  public void readAll() {
                      String p1 = PRIVATE_IMMUTABLE.get();
                      String p2 = PACKAGE_MUTATED.get();
                      String p3 = PUBLIC_READ_ONLY.get();
                      String p4 = PROTECTED_MUTATED.get();
                  }
              }
              """,
            """
              package com.example;

              public class MultipleThreadLocals {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<String> PRIVATE_IMMUTABLE = new ThreadLocal<>();
                  static final ThreadLocal<String> PACKAGE_MUTATED = new ThreadLocal<>();
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated in project (but accessible from outside due to non-private access))~~>*/public static final ThreadLocal<String> PUBLIC_READ_ONLY = new ThreadLocal<>();
                  protected static final ThreadLocal<String> PROTECTED_MUTATED = new ThreadLocal<>();

                  public void readAll() {
                      String p1 = PRIVATE_IMMUTABLE.get();
                      String p2 = PACKAGE_MUTATED.get();
                      String p3 = PUBLIC_READ_ONLY.get();
                      String p4 = PROTECTED_MUTATED.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class Mutator {
                  public void mutate() {
                      MultipleThreadLocals.PACKAGE_MUTATED.set("mutated");
                      MultipleThreadLocals.PROTECTED_MUTATED.remove();
                  }

                  public void readOnly() {
                      String value = MultipleThreadLocals.PUBLIC_READ_ONLY.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void detectMutationThroughMethodReference() {
        rewriteRun(
          java(
            """
              package com.example;

              import java.util.function.Consumer;

              public class MethodReferenceExample {
                  public static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public static void setValue(String value) {
                      TL.set(value);
                  }

                  public void useMethodReference() {
                      Consumer<String> setter = MethodReferenceExample::setValue;
                      setter.accept("value");
                  }
              }
              """
          )
        );
        // ThreadLocal is mutated through setValue method, should NOT be marked as immutable
    }

    @Test
    void detectIndirectMutationThroughPublicSetter() {
        rewriteRun(
          java(
            """
              package com.example;

              public class IndirectMutation {
                  private static final ThreadLocal<String> PRIVATE_TL = new ThreadLocal<>();

                  public static void setThreadLocalValue(String value) {
                      PRIVATE_TL.set(value);
                  }

                  public static String getThreadLocalValue() {
                      return PRIVATE_TL.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class ExternalSetter {
                  public void mutateIndirectly() {
                      IndirectMutation.setThreadLocalValue("mutated");
                  }
              }
              """
          )
        );
        // ThreadLocal should NOT be marked as immutable because setThreadLocalValue mutates it
    }

    @Test
    void instanceThreadLocalWithCrossFileAccess() {
        rewriteRun(
          java(
            """
              package com.example;

              public class InstanceThreadLocalHolder {
                  public final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class InstanceMutator {
                  public void mutate(InstanceThreadLocalHolder holder) {
                      holder.instanceTL.set("mutated");
                  }
              }
              """
          )
        );
        // Instance ThreadLocal should NOT be marked as immutable due to external mutation
    }
}
