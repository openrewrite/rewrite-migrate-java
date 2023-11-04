/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;


@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
class SequencedCollectionFirstAndLastTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SequencedCollectionFirstAndLast())
          .afterTypeValidationOptions(TypeValidation.builder().methodInvocations(false).build())
          .allSources(src -> src.markers(javaVersion(21)));
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
        void addLast() {
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
                  """,
                """
                  import java.util.*;
                                
                  class Foo {
                      void bar(List<String> collection) {
                          collection.addLast("last");
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
    }
}
