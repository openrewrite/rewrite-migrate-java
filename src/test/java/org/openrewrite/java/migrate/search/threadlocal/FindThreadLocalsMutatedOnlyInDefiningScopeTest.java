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
package org.openrewrite.java.migrate.search.threadlocal;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.migrate.table.ThreadLocalTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindThreadLocalsMutatedOnlyInDefiningScopeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindThreadLocalsMutatedOnlyInDefiningScope());
    }

    @DocumentExample
    @Test
    void identifyThreadLocalMutatedOnlyInConstructor() {
        rewriteRun(
          java(
            """
              class Example {
                  private final ThreadLocal<String> TL = new ThreadLocal<>();

                  public Example() {
                      TL.set("initial value");
                  }

                  public String getValue() {
                      return TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated during initialization (constructor/static initializer))~~>*/private final ThreadLocal<String> TL = new ThreadLocal<>();

                  public Example() {
                      TL.set("initial value");
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
    void identifyThreadLocalMutatedOnlyInStaticInitializer() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("static initial value");
                  }

                  public String getValue() {
                      return TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated during initialization (constructor/static initializer))~~>*/private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("static initial value");
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
    void identifyThreadLocalMutatedInDefiningClass() {
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

                  public void cleanup() {
                      TL.remove();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated within its defining class)~~>*/private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public void setValue(String value) {
                      TL.set(value);
                  }

                  public String getValue() {
                      return TL.get();
                  }

                  public void cleanup() {
                      TL.remove();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkThreadLocalNeverMutated() {
        // This should not be marked by this recipe - it's for FindNeverMutatedThreadLocals
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public String getValue() {
                      return TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkNonPrivateThreadLocal() {
        // Non-private ThreadLocals shouldn't be marked by this recipe
        rewriteRun(
          java(
            """
              class Example {
                  static final ThreadLocal<String> TL = new ThreadLocal<>();

                  public void setValue(String value) {
                      TL.set(value);
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyInstanceThreadLocalMutatedInConstructor() {
        rewriteRun(
          java(
            """
              class Example {
                  private final ThreadLocal<Integer> counter = new ThreadLocal<>();

                  public Example(int initial) {
                      counter.set(initial);
                  }

                  public Integer getCount() {
                      return counter.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated during initialization (constructor/static initializer))~~>*/private final ThreadLocal<Integer> counter = new ThreadLocal<>();

                  public Example(int initial) {
                      counter.set(initial);
                  }

                  public Integer getCount() {
                      return counter.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyThreadLocalWithMixedInitAndClassMutations() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("initial");
                  }

                  public void update(String value) {
                      TL.set(value);
                  }

                  public String getValue() {
                      return TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated within its defining class)~~>*/private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("initial");
                  }

                  public void update(String value) {
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
    void verifyDataTableOutput() {
        rewriteRun(
          spec -> spec.dataTable(ThreadLocalTable.Row.class, rows -> {
              assertThat(rows).hasSize(1);
              assertThat(rows.get(0).getClassName()).isEqualTo("Example");
              assertThat(rows.get(0).getFieldName()).isEqualTo("TL");
              assertThat(rows.get(0).getAccessModifier()).isEqualTo("private");
              assertThat(rows.get(0).getMutationType()).isEqualTo("Mutated only in initialization");
          }),
          java(
            """
              class Example {
                  private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("initial");
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is only mutated during initialization (constructor/static initializer))~~>*/private static final ThreadLocal<String> TL = new ThreadLocal<>();

                  static {
                      TL.set("initial");
                  }
              }
              """
          )
        );
    }
}
