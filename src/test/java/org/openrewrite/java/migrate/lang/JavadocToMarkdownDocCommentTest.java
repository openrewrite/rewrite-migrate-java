/*
 * Copyright 2025 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class JavadocToMarkdownDocCommentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JavadocToMarkdownDocComment());
        spec.allSources(source -> version(source, 23));
    }

    @DocumentExample
    @Test
    void multiLineJavadocWithTags() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Computes the sum of two numbers.
                   *
                   * @param a the first number
                   * @param b the second number
                   * @return the sum of a and b
                   */
                  public int add(int a, int b) {
                      return a + b;
                  }
              }
              """,
            """
              public class A {
                  /// Computes the sum of two numbers.
                  ///
                  /// @param a the first number
                  /// @param b the second number
                  /// @return the sum of a and b
                  public int add(int a, int b) {
                      return a + b;
                  }
              }
              """
          )
        );
    }

    @Test
    void singleLineJavadoc() {
        rewriteRun(
          java(
            """
              public class A {
                  /** Returns the name. */
                  public String getName() {
                      return "name";
                  }
              }
              """,
            """
              public class A {
                  /// Returns the name.
                  public String getName() {
                      return "name";
                  }
              }
              """
          )
        );
    }

    @Test
    void javadocWithCodeTag() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Use the {@code List} interface for ordered collections.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Use the `List` interface for ordered collections.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithLinkTag() {
        rewriteRun(
          java(
            """
              import java.util.List;
              public class A {
                  /**
                   * Returns a {@link List} of items.
                   */
                  public void m() {}
              }
              """,
            """
              import java.util.List;
              public class A {
                  /// Returns a [List] of items.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithParagraph() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * First paragraph.
                   *
                   * <p>Second paragraph.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// First paragraph.
                  ///
                  /// Second paragraph.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithEmphasis() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * This is <em>important</em> text.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// This is _important_ text.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithStrong() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * This is <strong>very important</strong> text.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// This is **very important** text.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocOnClass() {
        rewriteRun(
          java(
            """
              /**
               * A simple class.
               *
               * @since 1.0
               */
              public class A {
              }
              """,
            """
              /// A simple class.
              ///
              /// @since 1.0
              public class A {
              }
              """
          )
        );
    }

    @Test
    void javadocWithThrows() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Does something.
                   *
                   * @throws IllegalArgumentException if argument is invalid
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Does something.
                  ///
                  /// @throws IllegalArgumentException if argument is invalid
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithHtmlEntities() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Checks if a &lt; b &amp;&amp; c &gt; d.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Checks if a < b && c > d.
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithInheritDoc() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * {@inheritDoc}
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// {@inheritDoc}
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void noChangeForRegularComments() {
        rewriteRun(
          java(
            """
              public class A {
                  // line comment
                  /* block comment */
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocOnField() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * The name of the entity.
                   */
                  private String name;
              }
              """,
            """
              public class A {
                  /// The name of the entity.
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void javadocWithPreBlock() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Example usage:
                   * <pre>
                   * List&lt;String&gt; items = getItems();
                   * items.forEach(System.out::println);
                   * </pre>
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Example usage:
                  /// ```
                  /// List<String> items = getItems();
                  /// items.forEach(System.out::println);
                  /// ```
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithUnorderedList() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Features:
                   * <ul>
                   * <li>Fast</li>
                   * <li>Reliable</li>
                   * </ul>
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Features:
                  ///
                  /// - Fast
                  /// - Reliable
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithDeprecated() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * @deprecated Use {@link #newMethod()} instead.
                   */
                  public void m() {}
                  public void newMethod() {}
              }
              """,
            """
              public class A {
                  /// @deprecated Use [#newMethod()] instead.
                  public void m() {}
                  public void newMethod() {}
              }
              """
          )
        );
    }

    @Test
    void javadocWithSee() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * Does something.
                   *
                   * @see Object#toString()
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// Does something.
                  ///
                  /// @see Object#toString()
                  public void m() {}
              }
              """
          )
        );
    }

    @Test
    void javadocPreservesIndentationInNestedClass() {
        rewriteRun(
          java(
            """
              public class Outer {
                  public static class Inner {
                      /**
                       * Inner method.
                       */
                      public void m() {}
                  }
              }
              """,
            """
              public class Outer {
                  public static class Inner {
                      /// Inner method.
                      public void m() {}
                  }
              }
              """
          )
        );
    }

    @Test
    void javadocWithLiteralTag() {
        rewriteRun(
          java(
            """
              public class A {
                  /**
                   * The type {@literal <T>} is a type parameter.
                   */
                  public void m() {}
              }
              """,
            """
              public class A {
                  /// The type `<T>` is a type parameter.
                  public void m() {}
              }
              """
          )
        );
    }

    @Nested
    class Jep467FlagshipExamples {

        @Test
        void javadocWithLinkWithLabel() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * See {@link #equals(Object) equals} for details.
                       */
                      public void m() {}
                      public boolean equals(Object o) { return false; }
                  }
                  """,
                """
                  public class A {
                      /// See [equals][#equals(Object)] for details.
                      public void m() {}
                      public boolean equals(Object o) { return false; }
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithOrderedList() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * Steps:
                       * <ol>
                       * <li>First</li>
                       * <li>Second</li>
                       * <li>Third</li>
                       * </ol>
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// Steps:
                      ///
                      /// 1. First
                      /// 2. Second
                      /// 3. Third
                      public void m() {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithException() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * Does something.
                       *
                       * @exception IllegalArgumentException if argument is invalid
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// Does something.
                      ///
                      /// @exception IllegalArgumentException if argument is invalid
                      public void m() {}
                  }
                  """
              )
            );
        }

        @ExpectedToFail("Extra blank lines around content in fenced code block")
        @Test
        void javadocWithMultiLineCode() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * Example:
                       * {@code
                       * int x = 1;
                       * int y = 2;
                       * }
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// Example:
                      /// ```
                      /// int x = 1;
                      /// int y = 2;
                      /// ```
                      public void m() {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithImplSpec() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * Does something.
                       *
                       * @implSpec The default implementation does nothing.
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// Does something.
                      ///
                      /// @implSpec The default implementation does nothing.
                      public void m() {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithItalicAndBoldTags() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * This is <i>italic</i> and <b>bold</b> text.
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// This is _italic_ and **bold** text.
                      public void m() {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocParamWithInlineCode() {
            rewriteRun(
              java(
                """
                  import java.util.List;
                  public class A {
                      /**
                       * Processes items.
                       *
                       * @param items the list, or {@code null} if not available
                       */
                      public void m(List<String> items) {}
                  }
                  """,
                """
                  import java.util.List;
                  public class A {
                      /// Processes items.
                      ///
                      /// @param items the list, or `null` if not available
                      public void m(List<String> items) {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithDocRoot() {
            rewriteRun(
              java(
                """
                  public class A {
                      /**
                       * See <a href="{@docRoot}/help.html">help</a>.
                       */
                      public void m() {}
                  }
                  """,
                """
                  public class A {
                      /// See <a href="{@docRoot}/help.html">help</a>.
                      public void m() {}
                  }
                  """
              )
            );
        }

        @Test
        void javadocWithValue() {
            rewriteRun(
              java(
                """
                  public class A {
                      /** The default value is {@value}. */
                      public static final int DEFAULT = 42;
                  }
                  """,
                """
                  public class A {
                      /// The default value is {@value}.
                      public static final int DEFAULT = 42;
                  }
                  """
              )
            );
        }

        @ExpectedToFail("@see with qualified names uses # instead of . for package separators")
        @Test
        void javadocHashCodeExample() {
            rewriteRun(
              java(
                """
                  import java.util.HashMap;
                  public class A {
                      /**
                       * Returns a hash code value for the object. This method is
                       * supported for the benefit of hash tables such as those provided by
                       * {@link java.util.HashMap}.
                       * <p>
                       * The general contract of {@code hashCode} is:
                       * <ul>
                       * <li>Whenever it is invoked on the same object more than once during
                       *     an execution of a Java application, the {@code hashCode} method
                       *     must consistently return the same integer.
                       * <li>If two objects are equal according to the {@link
                       *     #equals(Object) equals} method, then calling the {@code
                       *     hashCode} method on each of the two objects must produce the
                       *     same integer result.
                       * <li>It is <em>not</em> required that if two objects are unequal
                       *     according to the {@link #equals(Object) equals} method, then
                       *     calling the {@code hashCode} method on each of the two objects
                       *     must produce distinct integer results.
                       * </ul>
                       *
                       * @implSpec
                       * As far as is reasonably practical, the {@code hashCode} method defined
                       * by class {@code Object} returns distinct integers for distinct objects.
                       *
                       * @return  a hash code value for this object.
                       * @see     java.lang.Object#equals(java.lang.Object)
                       * @see     java.lang.System#identityHashCode
                       */
                      public int hashCode() { return 0; }
                      public boolean equals(Object o) { return false; }
                  }
                  """,
                """
                  import java.util.HashMap;
                  public class A {
                      /// Returns a hash code value for the object. This method is
                      /// supported for the benefit of hash tables such as those provided by
                      /// [java.util.HashMap].
                      ///
                      /// The general contract of `hashCode` is:
                      ///
                      /// - Whenever it is invoked on the same object more than once during
                      ///     an execution of a Java application, the `hashCode` method
                      ///     must consistently return the same integer.
                      /// - If two objects are equal according to the
                      ///     [equals][#equals(Object)] method, then calling the `hashCode`
                      ///     method on each of the two objects must produce the
                      ///     same integer result.
                      /// - It is _not_ required that if two objects are unequal
                      ///     according to the [equals][#equals(Object)] method, then
                      ///     calling the `hashCode` method on each of the two objects
                      ///     must produce distinct integer results.
                      ///
                      /// @implSpec
                      /// As far as is reasonably practical, the `hashCode` method defined
                      /// by class `Object` returns distinct integers for distinct objects.
                      ///
                      /// @return  a hash code value for this object.
                      /// @see     java.lang.Object#equals(java.lang.Object)
                      /// @see     java.lang.System#identityHashCode
                      public int hashCode() { return 0; }
                      public boolean equals(Object o) { return false; }
                  }
                  """
              )
            );
        }
    }
}
