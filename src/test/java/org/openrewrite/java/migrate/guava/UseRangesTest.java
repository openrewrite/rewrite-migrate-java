/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseRangesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("guava"))
          .recipe(new UseRangesRecipes());
    }

    @Test
    @DocumentExample
    void simple() {
        rewriteRun(
                //language=java
                java(//todo add other cases
                        """
                          import java.math.BigDecimal;

                          class Test {

                            void foo() {
                              BigDecimal from = new BigDecimal("0");
                              BigDecimal candidate = new BigDecimal("0");
                              BigDecimal to = new BigDecimal("2");
                              boolean trueCondition = from.compareTo(candidate) <= 0
                                                    && candidate.compareTo(to) <= 0;
                            }
                          }
                          """,
                        """
                          import com.google.common.collect.Range;

                          import java.math.BigDecimal;

                          class Test {

                            void foo() {
                              BigDecimal from = new BigDecimal("0");
                              BigDecimal candidate = new BigDecimal("0");
                              BigDecimal to = new BigDecimal("2");
                              boolean trueCondition = Range.closed(from, to).contains(candidate);
                            }
                          }
                          """
                )
        );
    }

    @Nested
    class Generic {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = from.compareTo(candidate) <= 0
                                              && candidate.compareTo(to) <= 0;
                        boolean condition2 = from.compareTo(candidate) <= 0
                                              && to.compareTo(candidate) >= 0;
                        boolean condition3 = candidate.compareTo(from) >= 0
                                              && candidate.compareTo(to) <= 0;
                        boolean condition4 = candidate.compareTo(to) <= 0
                                              && from.compareTo(candidate) <= 0;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = from.compareTo(candidate) < 0
                                              && candidate.compareTo(to) < 0;
                        boolean condition2 = from.compareTo(candidate) < 0
                                              && to.compareTo(candidate) > 0;
                        boolean condition3 = candidate.compareTo(from) > 0
                                              && candidate.compareTo(to) < 0;
                        boolean condition4 = candidate.compareTo(to) < 0
                                              && from.compareTo(candidate) < 0;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = from.compareTo(candidate) <= 0
                                              && candidate.compareTo(to) < 0;
                        boolean condition2 = from.compareTo(candidate) <= 0
                                              && to.compareTo(candidate) > 0;
                        boolean condition3 = candidate.compareTo(from) >= 0
                                              && candidate.compareTo(to) < 0;
                        boolean condition4 = candidate.compareTo(to) < 0
                                              && from.compareTo(candidate) <= 0;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                //language=java
                java(
                  """
                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = from.compareTo(candidate) < 0
                                              && candidate.compareTo(to) <= 0;
                        boolean condition2 = from.compareTo(candidate) < 0
                                              && to.compareTo(candidate) >= 0;
                        boolean condition3 = candidate.compareTo(from) > 0
                                              && candidate.compareTo(to) <= 0;
                        boolean condition4 = candidate.compareTo(to) <= 0
                                              && from.compareTo(candidate) < 0;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    import java.math.BigDecimal;

                    class Test {

                      void foo() {
                        BigDecimal from = new BigDecimal("0");
                        BigDecimal candidate = new BigDecimal("0");
                        BigDecimal to = new BigDecimal("2");
                        boolean condition1 = Range.openClosed(from, to).contains(candidate);
                        boolean condition2 = Range.openClosed(from, to).contains(candidate);
                        boolean condition3 = Range.openClosed(from, to).contains(candidate);
                        boolean condition4 = Range.openClosed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }
    }

    @Nested
    class Int {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate <= to;
                        boolean condition2 = from <= candidate
                                              && to >= candidate;
                        boolean condition3 = candidate >= from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = from < candidate
                                              && candidate < to;
                        boolean condition2 = from < candidate
                                              && to > candidate;
                        boolean condition3 = candidate > from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate < to;
                        boolean condition2 = from <= candidate
                                              && to > candidate;
                        boolean condition3 = candidate >= from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = from < candidate
                                              && candidate <= to;
                        boolean condition2 = from < candidate
                                              && to >= candidate;
                        boolean condition3 = candidate > from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        int from = 0;
                        int candidate = 0;
                        int to = 2;
                        boolean condition1 = Range.openClosed(from, to).contains(candidate);
                        boolean condition2 = Range.openClosed(from, to).contains(candidate);
                        boolean condition3 = Range.openClosed(from, to).contains(candidate);
                        boolean condition4 = Range.openClosed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }
    }

    @Nested
    class Short {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate <= to;
                        boolean condition2 = from <= candidate
                                              && to >= candidate;
                        boolean condition3 = candidate >= from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = from < candidate
                                              && candidate < to;
                        boolean condition2 = from < candidate
                                              && to > candidate;
                        boolean condition3 = candidate > from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate < to;
                        boolean condition2 = from <= candidate
                                              && to > candidate;
                        boolean condition3 = candidate >= from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        short from = 0;
                        short candidate = 0;
                        short to = 2;
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                    //language=java
                    java(
                            """
                              class Test {

                                void foo() {
                                  short from = 0;
                                  short candidate = 0;
                                  short to = 2;
                                  boolean condition1 = from < candidate
                                                        && candidate <= to;
                                  boolean condition2 = from < candidate
                                                        && to >= candidate;
                                  boolean condition3 = candidate > from
                                                        && candidate <= to;
                                  boolean condition4 = candidate <= to
                                                        && from < candidate;
                                }
                              }
                              """,
                            """
                              import com.google.common.collect.Range;

                              class Test {

                                void foo() {
                                  short from = 0;
                                  short candidate = 0;
                                  short to = 2;
                                  boolean condition1 = Range.openClosed(from, to).contains(candidate);
                                  boolean condition2 = Range.openClosed(from, to).contains(candidate);
                                  boolean condition3 = Range.openClosed(from, to).contains(candidate);
                                  boolean condition4 = Range.openClosed(from, to).contains(candidate);
                                }
                              }
                              """
                    )
            );
        }
    }

    @Nested
    class Long {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate <= to;
                        boolean condition2 = from <= candidate
                                              && to >= candidate;
                        boolean condition3 = candidate >= from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = from < candidate
                                              && candidate < to;
                        boolean condition2 = from < candidate
                                              && to > candidate;
                        boolean condition3 = candidate > from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate < to;
                        boolean condition2 = from <= candidate
                                              && to > candidate;
                        boolean condition3 = candidate >= from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = from < candidate
                                              && candidate <= to;
                        boolean condition2 = from < candidate
                                              && to >= candidate;
                        boolean condition3 = candidate > from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        long from = 0;
                        long candidate = 0;
                        long to = 2;
                        boolean condition1 = Range.openClosed(from, to).contains(candidate);
                        boolean condition2 = Range.openClosed(from, to).contains(candidate);
                        boolean condition3 = Range.openClosed(from, to).contains(candidate);
                        boolean condition4 = Range.openClosed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }
    }

    @Nested
    class Float {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate <= to;
                        boolean condition2 = from <= candidate
                                              && to >= candidate;
                        boolean condition3 = candidate >= from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = from < candidate
                                              && candidate < to;
                        boolean condition2 = from < candidate
                                              && to > candidate;
                        boolean condition3 = candidate > from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate < to;
                        boolean condition2 = from <= candidate
                                              && to > candidate;
                        boolean condition3 = candidate >= from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = from < candidate
                                              && candidate <= to;
                        boolean condition2 = from < candidate
                                              && to >= candidate;
                        boolean condition3 = candidate > from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        float from = 0;
                        float candidate = 0;
                        float to = 2;
                        boolean condition1 = Range.openClosed(from, to).contains(candidate);
                        boolean condition2 = Range.openClosed(from, to).contains(candidate);
                        boolean condition3 = Range.openClosed(from, to).contains(candidate);
                        boolean condition4 = Range.openClosed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }
    }

    @Nested
    class Double {

        @Test
        void closed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate <= to;
                        boolean condition2 = from <= candidate
                                              && to >= candidate;
                        boolean condition3 = candidate >= from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = Range.closed(from, to).contains(candidate);
                        boolean condition2 = Range.closed(from, to).contains(candidate);
                        boolean condition3 = Range.closed(from, to).contains(candidate);
                        boolean condition4 = Range.closed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void open() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = from < candidate
                                              && candidate < to;
                        boolean condition2 = from < candidate
                                              && to > candidate;
                        boolean condition3 = candidate > from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = Range.open(from, to).contains(candidate);
                        boolean condition2 = Range.open(from, to).contains(candidate);
                        boolean condition3 = Range.open(from, to).contains(candidate);
                        boolean condition4 = Range.open(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void closedOpen() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = from <= candidate
                                              && candidate < to;
                        boolean condition2 = from <= candidate
                                              && to > candidate;
                        boolean condition3 = candidate >= from
                                              && candidate < to;
                        boolean condition4 = candidate < to
                                              && from <= candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition2 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition3 = Range.closedOpen(from, to).contains(candidate);
                        boolean condition4 = Range.closedOpen(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }

        @Test
        void openClosed() {
            rewriteRun(
                //language=java
                java(
                  """
                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = from < candidate
                                              && candidate <= to;
                        boolean condition2 = from < candidate
                                              && to >= candidate;
                        boolean condition3 = candidate > from
                                              && candidate <= to;
                        boolean condition4 = candidate <= to
                                              && from < candidate;
                      }
                    }
                    """,
                  """
                    import com.google.common.collect.Range;

                    class Test {

                      void foo() {
                        double from = 0;
                        double candidate = 0;
                        double to = 2;
                        boolean condition1 = Range.openClosed(from, to).contains(candidate);
                        boolean condition2 = Range.openClosed(from, to).contains(candidate);
                        boolean condition3 = Range.openClosed(from, to).contains(candidate);
                        boolean condition4 = Range.openClosed(from, to).contains(candidate);
                      }
                    }
                    """
                )
            );
        }
    }

    @Test
    @DocumentExample
    void regression1() {
        rewriteRun(
          //language=java
          java(
            """
              import java.math.BigDecimal;

              class Test {

                void foo() {
                  boolean b = BigDecimal.ZERO.compareTo(BigDecimal.ONE) <= 0
                           && BigDecimal.ONE.compareTo(BigDecimal.TEN) <= 0;
                }
              }
              """,
            """
              import com.google.common.collect.Range;

              import java.math.BigDecimal;

              class Test {

                void foo() {
                  boolean b = Range.closed(BigDecimal.ZERO, BigDecimal.TEN).contains(BigDecimal.ONE);
                }
              }
              """
          )
        );
    }

    @Test
    void real() {
        rewriteRun(
          //language=java
          java(
            """
              import java.math.BigDecimal;

              class Test {

                class T {
                  BigDecimal getMinValue() { return null;}
                  BigDecimal getMaxValue() { return null;}
                }

                private boolean isInRange(BigDecimal value, T t) {
                  return t.getMinValue().compareTo(value) <= 0
                      && t.getMaxValue().compareTo(value) >= 0;
                }
              }
              """,
            """
              import com.google.common.collect.Range;

              import java.math.BigDecimal;

              class Test {

                class T {
                  BigDecimal getMinValue() { return null;}
                  BigDecimal getMaxValue() { return null;}
                }

                private boolean isInRange(BigDecimal value, T t) {
                  return Range.closed(t.getMinValue(), t.getMaxValue()).contains(value);
                }
              }
              """
          )
        );
    }

    @Test
    void unchanged() {
        rewriteRun(
          spec -> spec.recipe(new UseRangesRecipes()),
          //language=java
          java(
            """
              class Test {
                  boolean unchanged1 = booleanExpression() ? booleanExpression() : !booleanExpression();
                  boolean unchanged2 = booleanExpression() ? true : !booleanExpression();
                  boolean unchanged3 = booleanExpression() ? booleanExpression() : false;

                  boolean booleanExpression() {
                    return true;
                  }
              }
              """
          )
        );
    }
}
