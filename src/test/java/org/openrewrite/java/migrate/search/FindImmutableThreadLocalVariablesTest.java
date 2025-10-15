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

class FindImmutableThreadLocalVariablesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindImmutableThreadLocalVariables());
    }

    @DocumentExample
    @Test
    void identifySimpleImmutableThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public String getValue() {
                      return TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public String getValue() {
                      return TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyThreadLocalWithInitialValue() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<Integer> COUNTER = ThreadLocal.withInitial(() -> 0);

                  public int getCount() {
                      return COUNTER.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<Integer> COUNTER = ThreadLocal.withInitial(() -> 0);

                  public int getCount() {
                      return COUNTER.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkThreadLocalWithSetCall() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public void setValue(String value) {
                      TL.set(value);
                  }

                  public String getValue() {
                      return TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkThreadLocalWithRemoveCall() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public void cleanup() {
                      TL.remove();
                  }

                  public String getValue() {
                      return TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkReassignedThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private static ThreadLocal<String> tl = new ThreadLocal<>();

                  public void reset() {
                      tl = new ThreadLocal<>();
                  }

                  public String getValue() {
                      return tl.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void handleMultipleThreadLocalsWithMixedPatterns() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> IMMUTABLE_TL = new ThreadLocal<>();
                  private static final ThreadLocal<Integer> MUTABLE_TL = new ThreadLocal<>();
                  private static final ThreadLocal<Boolean> ANOTHER_IMMUTABLE = ThreadLocal.withInitial(() -> false);

                  public void updateMutable(int value) {
                      MUTABLE_TL.set(value);
                  }

                  public String getImmutable() {
                      return IMMUTABLE_TL.get();
                  }

                  public Boolean getAnother() {
                      return ANOTHER_IMMUTABLE.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<String> IMMUTABLE_TL = new ThreadLocal<>();
                  private static final ThreadLocal<Integer> MUTABLE_TL = new ThreadLocal<>();
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<Boolean> ANOTHER_IMMUTABLE = ThreadLocal.withInitial(() -> false);

                  public void updateMutable(int value) {
                      MUTABLE_TL.set(value);
                  }

                  public String getImmutable() {
                      return IMMUTABLE_TL.get();
                  }

                  public Boolean getAnother() {
                      return ANOTHER_IMMUTABLE.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyInstanceThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void handleThreadLocalWithComplexInitialization() {
        rewriteRun(
          java(
            """
              import java.text.SimpleDateFormat;

              class Example {
                  private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
                      ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

                  public String formatDate(java.util.Date date) {
                      return DATE_FORMAT.get().format(date);
                  }
              }
              """,
            """
              import java.text.SimpleDateFormat;

              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
                      ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

                  public String formatDate(java.util.Date date) {
                      return DATE_FORMAT.get().format(date);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkLocalVariableThreadLocal() {
        // Local ThreadLocals are unusual but should not be marked as they have different lifecycle
        rewriteRun(
          java(
            """
              class Example {
                  public void method() {
                      ThreadLocal<String> localTL = new ThreadLocal<>();
                      localTL.set("value");
                      System.out.println(localTL.get());
                  }
              }
              """
          )
        );
    }

    @Test
    void warnAboutPackagePrivateThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  static final ThreadLocal<String> PACKAGE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PACKAGE_TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated in project (but accessible from outside due to non-private access))~~>*/static final ThreadLocal<String> PACKAGE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PACKAGE_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void warnAboutProtectedThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated in project (but accessible from outside due to non-private access))~~>*/protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void handleInheritableThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final InheritableThreadLocal<String> ITL = new InheritableThreadLocal<>();

                  public String getValue() {
                      return ITL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal candidate for ScopedValue migration - never mutated after initialization)~~>*/private static final InheritableThreadLocal<String> ITL = new InheritableThreadLocal<>();

                  public String getValue() {
                      return ITL.get();
                  }
              }
              """
          )
        );
    }
}