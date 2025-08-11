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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings("RedundantStringFormatCall")
class StringFormattedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StringFormatted(null));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2163")
    @Test
    void textBlock() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                package com.example.app;
                class A {
                    String str = String.format(\"""
                    foo
                    %s
                    \""", "a");
                }
                """,
              """
                package com.example.app;
                class A {
                    String str = \"""
                    foo
                    %s
                    \""".formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void concatenatedText() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                package com.example.app;
                class A {
                    String str = String.format("foo"
                            + "%s", "a");
                }
                """,
              """
                package com.example.app;
                class A {
                    String str = ("foo"
                            + "%s").formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/616")
    @Test
    void concatenatedFormatStringNotConvertedIfIndicated() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new StringFormatted(false)),
          version(
            java(
              """
                package com.example.app;
                class A {
                    String str = String.format("foo"
                            + "%s", "a");
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void callingFunction() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                package com.example.app;

                class A {
                    String str = String.format(getTemplateString(), "a");

                    private String getTemplateString() {
                        return "foo %s";
                    }
                }
                """,
              """
                package com.example.app;

                class A {
                    String str = getTemplateString().formatted("a");

                    private String getTemplateString() {
                        return "foo %s";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void oneArgument() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format("foo");
                }
                """,
              """
                class A {
                    String str = "foo".formatted();
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void twoArguments() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format("foo %s", "a");
                }
                """,
              """
                class A {
                    String str = "foo %s".formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void threeArguments() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format("foo %s %d", "a", 1);
                }
                """,
              """
                class A {
                    String str = "foo %s %d".formatted("a", 1);
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void fourArguments() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String str = String.format("foo %s %d %f", "a", 1, 2.0);
                }
                """,
              """
                class A {
                    String str = "foo %s %d %f".formatted("a", 1, 2.0);
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void splitFirstArgument() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format("foo " + "%s", "a");
                }
                """,
              """
                class A {
                    String str = ("foo " + "%s").formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void splitSecondArgument() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String str = String.format("foo %s", "a" + "b");
                }
                """,
              """
                class A {
                    String str = "foo %s".formatted("a" + "b");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void doNotWrapMethodInvocation() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String str = String.format(someMethod(), "a");
                    String someMethod() {
                        return "foo %s";
                    }
                }
                """,
              """
                class A {
                    String str = someMethod().formatted("a");
                    String someMethod() {
                        return "foo %s";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void doNotWrapLocalVariable() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String someMethod() {
                        String fmt = "foo %s";
                        String str = String.format(fmt, "a");
                    }
                }
                """,
              """
                class A {
                    String someMethod() {
                        String fmt = "foo %s";
                        String str = fmt.formatted("a");
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void doNotWrapField() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    static final String fmt = "foo %s";
                    String str = String.format(fmt, "a");
                }
                """,
              """
                class A {
                    static final String fmt = "foo %s";
                    String str = fmt.formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void doNotWrapFieldAccess() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    final String fmt = "foo %s";
                    String str = String.format(this.fmt, "a");
                }
                """,
              """
                class A {
                    final String fmt = "foo %s";
                    String str = this.fmt.formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/77")
    @Test
    void removeStaticImport() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import static java.lang.String.format;
                class A {
                    String str = format("foo %s", "a");
                }
                """,
              """
                class A {
                    String str = "foo %s".formatted("a");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    @Test
    void doNotMatchLocale() {
        //language=java
        rewriteRun(
          version(
            java("""
              import java.util.Locale;
              class A {
                  String str = String.format(Locale.US, "foo %s", "a");
              }
              """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    @Test
    void doNotChangeLackOfWhitespace() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format("foo %s %s","a","b");
                }
                """,
              """
                class A {
                    String str = "foo %s %s".formatted("a", "b");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    @Test
    void doNotChangeWhitespaceWithNewlines() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String str = String.format("foo %s %s",
                        "a",
                        "b");
                }
                """,
              """
                class A {
                    String str = "foo %s %s".formatted(
                            "a",
                            "b");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/122")
    @Test
    void doNotChangeWhitespaceWithNewlinesAndComments() {
        //language=java
        rewriteRun(
          version(
            java("""
                class A {
                    String str = String.format("foo %s %s",
                        "a",
                        // B
                        "b");
                }
                """,
              """
                class A {
                    String str = "foo %s %s".formatted(
                            "a",
                            // B
                            "b");
                }
                """
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/453")
    @Test
    void doNotRetainTrailingWhitespaceWhenFirstArgumentOnNewLine() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class A {
                    String str = String.format(
                    "foo %s %s",
                        "a",
                        "b");
                }
                """,
              """
                class A {
                    String str = "foo %s %s".formatted(
                            "a",
                            "b");
                }
                """
            ),
            17
          )
        );
    }
}
