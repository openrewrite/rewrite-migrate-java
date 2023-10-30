/*
 * Copyright (c) 2023 Atlassian US Inc.
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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class OptionalStreamRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OptionalStreamRecipe())
          .parser(JavaParser.fromJavaVersion());
    }

    @Test
    public void basic_case() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  var x = Stream.of(Optional.empty())
                       .filter(Optional::isPresent)
                       .map(Optional::get);
                }
              }
              """,
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  var x = Stream.of(Optional.empty())
                       .flatMap(Optional::stream);
                }
              }
              """
          ));
    }

    @Test
    public void twice() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  var x = Stream.of(Optional.empty())
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .map(Optional::of)
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .toList();
                              }
              }
              """,
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  var x = Stream.of(Optional.empty())
                      .flatMap(Optional::stream)
                      .map(Optional::of)
                      .flatMap(Optional::stream)
                      .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void without_assignment() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  Stream.of(Optional.empty())
                           .filter(Optional::isPresent)
                           .map(Optional::get);
                              }
              }
              """,
            """
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  Stream.of(Optional.empty())
                           .flatMap(Optional::stream);
                              }
              }
              """
          ));
    }

    @Test
    public void oneliner() {
        rewriteRun(
          //language=java
          java(
            """
                   import java.util.Optional;
                   import java.util.stream.Stream;
                   
                   class Scratch {
                     public void foo() {Stream.of(Optional.empty()).filter(Optional::isPresent).map(Optional::get);}
                   }
              """,
            """
                   import java.util.Optional;
                   import java.util.stream.Stream;
                   
                   class Scratch {
                     public void foo() {Stream.of(Optional.empty()).flatMap(Optional::stream);}
                   }
              """
          ));
    }

    @Test
    public void with_toList() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Integer> list = Stream.of(17)
                          .map(Optional::of)
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Integer> list = Stream.of(17)
                          .map(Optional::of)
                          .flatMap(Optional::stream)
                          .toList();
                              }
              }
              """));
    }

    @Test
    public void with_toList2() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          .flatMap(Optional::stream)
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void with_comment() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          // some comment
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          // some comment
                          .flatMap(Optional::stream)
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void with_comment_after() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          // some comment
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          .flatMap(Optional::stream)
                          // some comment
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void with_comment_in_the_middle() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          .filter(Optional::isPresent)
                          // some comment
                          .map(Optional::get)
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          // TODO this block was automatically refactor, check if the comment is still relevant: some comment
                          .flatMap(Optional::stream)
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void with_multiple_coments() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          /* comment before */ .filter(Optional::isPresent) /* comment between */ .map(Optional::get) /* comment after */
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          /* comment before */ /* TODO this block was automatically refactor, check if the comment is still relevant: comment between */ .flatMap(Optional::stream) /* comment after */
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void with_block_comment() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          /* some comment */
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .toList();
                              }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;
              import java.util.stream.Stream;
                                      
              class Scratch {
                public void foo() {
                  final List<Object> list = Stream.of(Optional.empty())
                          /* some comment */
                          .flatMap(Optional::stream)
                          .toList();
                              }
              }
              """
          ));
    }

    @Test
    public void dont_match_different_optional() {
        rewriteRun(
          //language=java
          java(
            """
               import java.util.stream.Stream;
               
               class Scratch {
                 public void foo() {
                   var x = Stream.of(Optional.empty())
                       .filter(Optional::isPresent)
                       .map(Optional::get);
                 }
                 private static class Optional {
                   public static Optional empty() {}
                   public boolean isPresent() {return false;}
                   public Object get() {return null;}
                 }
               }
              """
          ));
    }
}