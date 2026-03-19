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

import org.junit.jupiter.api.Test;
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
}
