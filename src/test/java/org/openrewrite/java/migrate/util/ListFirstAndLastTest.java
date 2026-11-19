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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.kotlin.Assertions.kotlin;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
class ListFirstAndLastTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ListFirstAndLast())
          .allSources(src -> src.markers(javaVersion(21)));
    }

    @Test
    void preservesFirstElementOperationsCaughtByIndexHandler() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class T {
                  void prune(List<Long> times, long cutoff) {
                      try {
                          while (times.get(0) < cutoff) {
                              times.remove(0);
                          }
                      } catch (IndexOutOfBoundsException empty) {
                          return;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesLastElementOperationsCaughtByMultiCatch() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class T {
                  String prune(List<String> times) {
                      try {
                          try {
                              String last = times.get(times.size() - 1);
                              times.remove(times.size() - 1);
                              return last;
                          } finally {
                              System.out.println("checked");
                          }
                      } catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                          return null;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesOperationsWhenReplacementWouldEnterAnotherCatch() {
        rewriteRun(
          java(
            """
              import java.util.List;
              import java.util.NoSuchElementException;

              class T {
                  String first(List<String> values) {
                      try {
                          return values.get(0);
                      } catch (NoSuchElementException ignored) {
                          return null;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void rewritesAddAndOperationsOutsideProtectedBody() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class T {
                  String first(List<String> values, List<String> fallback) {
                      try {
                          values.add(0, "value");
                          return values.get(0);
                      } catch (IndexOutOfBoundsException ignored) {
                          return fallback.get(0);
                      } finally {
                          fallback.remove(0);
                      }
                  }
              }
              """,
            """
              import java.util.List;

              class T {
                  String first(List<String> values, List<String> fallback) {
                      try {
                          values.addFirst("value");
                          return values.get(0);
                      } catch (IndexOutOfBoundsException ignored) {
                          return fallback.getFirst();
                      } finally {
                          fallback.removeFirst();
                      }
                  }
              }
              """
          )
        );
    }

    @Nested
    class First {
        @Test
        void getFirst() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.get(0);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.getFirst();
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/423")
        @Test
        void getFirstFromMethodInvocation() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }

                      String bar() {
                          return collection().get(0);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }

                      String bar() {
                          return collection().getFirst();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addFirst() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      void bar(List<String> collection) {
                          collection.add(0, "first");
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      void bar(List<String> collection) {
                          collection.addFirst("first");
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/423")
        @Test
        void addFirstFromMethodInvocation() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }
                      void bar() {
                          collection().add(0, "first");
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }
                      void bar() {
                          collection().addFirst("first");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void removeFirst() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.remove(0);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.removeFirst();
                      }
                  }
                  """
              )
            );
        }

        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/423")
        @Test
        void removeFirstFromMethodInvocation() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }

                      String bar() {
                          return collection().remove(0);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      List<String> collection() {
                          return new ArrayList<>();
                      }

                      String bar() {
                          return collection().removeFirst();
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class Last {
        @Test
        void getLast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.get(collection.size() - 1);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.getLast();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void removeLast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.remove(collection.size() - 1);
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.removeLast();
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class NoChange {
        @Test
        void getSecond() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.get(1);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void getSecondToLast() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.List;

                  class Foo {
                      String bar(List<String> collection) {
                          return collection.get(collection.size() - 2);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void getLastOfDifferentCollection() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<String> a, List<String> b) {
                          return a.get(b.size() - 1);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addInteger() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      String bar(List<Integer> collection) {
                          return collection.add(0);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addAtSize() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      void bar(List<String> collection) {
                          collection.add(collection.size(), "last");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void addAtSizeMinusOne() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      void bar(List<String> collection) {
                          collection.add(collection.size() - 1, "last");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void kotlinIndexAccess() {
            rewriteRun(
              //language=kotlin
              kotlin(
                """
                  class Foo {
                      fun bar(collection: java.util.ArrayList<String>): String {
                          return collection[0]
                      }
                  }
                  """
              )
            );
        }

        @Test
        void kotlinGet() {
            rewriteRun(
              //language=kotlin
              kotlin(
                """
                  class Foo {
                      fun bar(collection: java.util.ArrayList<String>): String {
                          return collection.get(0)
                      }
                  }
                  """
              )
            );
        }

        @Test
        void groovyGet() {
            rewriteRun(
              //language=groovy
              groovy(
                """
                  class Foo {
                      String bar(List<String> collection) {
                          return collection.get(0)
                      }
                  }
                  """
              )
            );
        }
    }
}
