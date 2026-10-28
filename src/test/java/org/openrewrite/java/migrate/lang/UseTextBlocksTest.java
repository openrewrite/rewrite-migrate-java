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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.kotlin.Assertions.kotlin;

@SuppressWarnings({"ConcatenationWithEmptyString", "TextBlockMigration", "EscapedSpace"})
class UseTextBlocksTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseTextBlocks())
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Test
    void regular() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n" +
                          "my_table\\n" +
                          "WHERE something = 1;";
              }
              """,
            """
              class Test {
                  String query = \"""
                          SELECT * FROM
                          my_table
                          WHERE something = 1;\\
                          \""";
              }
              """
          )
        );
    }

    @Test
    void newlineAtBeginningOfLines() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void welcome() {
                      log("\\n========================================================="
                          + "\\n                                                         "
                          + "\\n          Welcome to Spring Integration!                 "
                          + "\\n                                                         "
                          + "\\n    For more information please visit:                   "
                          + "\\n    https://www.springsource.org/spring-integration      "
                          + "\\n                                                         "
                          + "\\n=========================================================");
                  }
                  void log(String s) {}
              }
              """,
            """
              class A {
                  void welcome() {
                      log(\"""
                         \s
                          =========================================================
                                                                                  \\s
                                    Welcome to Spring Integration!                \\s
                                                                                  \\s
                              For more information please visit:                  \\s
                              https://www.springsource.org/spring-integration     \\s
                                                                                  \\s
                          =========================================================\\
                          \""");
                  }
                  void log(String s) {}
              }
              """
          )
        );
    }

    @Test
    void preserveTrailingWhiteSpaces() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                      String query = "SELECT * FROM    \\n" +
                          "my_table    \\n" +
                          "WHERE something = 1; ";
              }
              """,
            """
              class Test {
                      String query = \"""
                          SELECT * FROM   \\s
                          my_table   \\s
                          WHERE something = 1;\\s\\
                          \""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1132")
    @Test
    void preserveTrailingSpaceOnLastLine() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                String message =
                    "Connection failed:\\n"
                        + " connect timed out\\n"
                        + "Transfer id: ";
              }
              """,
            """
              class Test {
                String message =
                    \"""
                    Connection failed:
                     connect timed out
                    Transfer id:\\s\\
                    \""";
              }
              """
          )
        );
    }

    @Test
    void preserveTrailingWhiteSpaces2() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String color = "red   \\n" +
                                 "green \\n" +
                                 "blue  \\n";
              }
              """,
            """
              class Test {
                  String color = \"""
                                 red  \\s
                                 green\\s
                                 blue \\s
                                 ""\";
              }
              """
          )
        );
    }

    @Test
    void worksOnNewerJavaVersions() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String color = "red\\n" +
                                 "green\\n" +
                                 "blue\\n";
              }
              """,
            """
              class Test {
                  String color = \"""
                                 red
                                 green
                                 blue
                                 ""\";
              }
              """,
              spec -> spec.markers(javaVersion(21))
          )
        );
    }

    @Test
    void indentsAlignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n" +
                      "my_table\\n" +
                          "WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
                  String query = \"""
                      SELECT * FROM
                      my_table
                      WHERE something = 1;
                      \""";
              }
              """
          )
        );
    }

    @Test
    void multipleLinesWithAdditionBinaryAtLineFront() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n"
                          + "my_table\\n"
                          + "WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
                  String query = \"""
                          SELECT * FROM
                          my_table
                          WHERE something = 1;
                          \""";
              }
              """
          )
        );
    }

    @Test
    void noChangeForStringBlocks() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = \"""
                         SELECT * FROM
                         my_table
                         \""" +
                         "WHERE something = 1;";
              }
              """
          )
        );
    }

    @Test
    void doNotDropLineComments() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n" + // table query
                          "my_table\\n" +
                          "WHERE something = 1;";
              }
              """
          )
        );
    }

    @Test
    void doNotDropBlockComments() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n" + /* table name */ "my_table\\n" +
                          "WHERE something = 1;";
              }
              """
          )
        );
    }

    @Test
    void preferNoChangeIfCarriageReturnInContent() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String text = "Deliberate\\r\\n" +
                          "carriage return";
              }
              """
          )
        );
    }

    @Test
    void preferChangeIfNoNewLineInContent() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM " +
                          "my_table" +
                          " WHERE something = 1;";
              }
              """,
            """
              class Test {
                  String query = \"""
                          SELECT * FROM \\
                          my_table\\
                           WHERE something = 1;\\
                          \""";
              }
              """
          )
        );
    }

    @Test
    void preferNoChangeIfNoNewLineInContent() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(false)),
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM " +
                          "my_table " +
                          "WHERE something = 1;";
              }
              """
          )
        );
    }

    @Test
    void noChangeIfNoNewLineInConcatenation() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM \\n" + "my_table \\n" + "WHERE something = 1;";
              }
              """
          )
        );
    }

    @Test
    void emptyStringOnFirstLine() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "" +
                          "SELECT * FROM\\n" +
                          "my_table\\n" +
                          "WHERE something = 1;";
              }
              """,
            """
              class Test {
                  String query = \"""
                          SELECT * FROM
                          my_table
                          WHERE something = 1;\\
                          \""";
              }
              """
          )
        );
    }

    @Test
    void startsOnNextLine() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query =
                          "SELECT * FROM\\n" +
                          "my_table\\n" +
                          "WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
                  String query =
                          \"""
                          SELECT * FROM
                          my_table
                          WHERE something = 1;
                          \""";
              }
              """
          )
        );
    }

    @Test
    void indentations() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String query = "SELECT * FROM\\n" +
                          "my_table\\n" +
                      "WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
                  String query = \"""
                      SELECT * FROM
                      my_table
                      WHERE something = 1;
                      \""";
              }
              """
          )
        );
    }

    @Test
    void indentationsWithTabsOnly() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true), 4),
          //language=java
          java(
            """
              class Test {
              	String query = "SELECT * FROM\\n" +
              			"my_table\\n" +
              			"WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
              	String query = \"""
              			SELECT * FROM
              			my_table
              			WHERE something = 1;
              			\""";
              }
              """
          )
        );
    }

    @Test
    void indentationsWithTabsOnlyAndReplaceToSpaces() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(false), 4),
          //language=java
          java(
            """
              class Test {
              	String query = "SELECT * FROM\\n" +
              			"my_table\\n" +
              			"WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
              	String query = \"""
                          SELECT * FROM
                          my_table
                          WHERE something = 1;
                          \""";
              }
              """
          )
        );
    }

    private static Consumer<RecipeSpec> tabsAndIndents(UnaryOperator<TabsAndIndentsStyle> with, int tabSize) {
        return spec -> spec.parser(JavaParser.fromJavaVersion().styles(List.of(
          new NamedStyles(
            randomId(), "TabsOnlyFile", "TabsOnlyFile", "tabSize is x", Set.of(),
            List.of(with.apply(buildTabsAndIndents(tabSize)))
          )
        )));
    }

    private static TabsAndIndentsStyle buildTabsAndIndents(int tabSize) {
        return new TabsAndIndentsStyle(true,
          tabSize,
          tabSize,
          tabSize * 2,
          false);
    }

    @Test
    void indentationsWithTabsAndWhitespacesCombined() {
        rewriteRun(
          tabsAndIndents(style -> style.withUseTabCharacter(true), 8),
          //language=java
          java(
            """
              class Test {
              	String query = "SELECT * FROM\\n" +
              			    "my_table\\n" +
              			    "WHERE something = 1;\\n";
              }
              """,
            """
              class Test {
              	String query = \"""
              			    SELECT * FROM
              			    my_table
              			    WHERE something = 1;
              			    \""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1158")
    @Test
    void keepTabEscapeToAvoidIncidentalWhitespaceStripping() {
        rewriteRun(
          //language=java
          java(
            """
              class Main {
                  String s =
                      ""
                          + "\\tb";
              }
              """,
            """
              class Main {
                  String s =
                      \"""
                      \\tb\""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1158")
    @Test
    void keepFormFeedEscapeToAvoidIncidentalWhitespaceStripping() {
        rewriteRun(
          //language=java
          java(
            """
              class Main {
                  String s =
                      ""
                          + "\\fb";
              }
              """,
            """
              class Main {
                  String s =
                      \"""
                      \\fb\""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/195")
    @Test
    void newlinesAlignment() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void method() {
                      print(String.format("CREATE KEYSPACE IF NOT EXISTS %s\\n"
                                          + "WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };", keyspace()));
                  }
                  String keyspace() {
                      return "key";
                  }
                  void print(String str) {
                      System.out.println(str);
                  }

                  public static void main(String[] args) {
                      new A().method();
                  }
              }
              """,
            """
              class A {
                  void method() {
                      print(String.format(\"""
                                          CREATE KEYSPACE IF NOT EXISTS %s
                                          WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };\\
                                          \""", keyspace()));
                  }
                  String keyspace() {
                      return "key";
                  }
                  void print(String str) {
                      System.out.println(str);
                  }

                  public static void main(String[] args) {
                      new A().method();
                  }
              }
              """
          )
        );
    }

    @Test
    void consecutiveNewLines() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String s1 = "line1\\n\\n" +
                              "line3\\n\\n\\n" +
                              "line6\\n";
              }
              """,
            """
              class Test {
                  String s1 = \"""
                              line1
                             \s
                              line3
                             \s
                             \s
                              line6
                              \""";
              }
              """
          )
        );
    }

    /// Single escaping a quote in a string literal provides: " -> \"
    ///
    /// On converting this to a text block, we can let go of the escaping for the double quote: \" -> "
    @Test
    void singleEscapedQuote() {
        rewriteRun(
          //language=java
          java(
            // Before:
            // String json = "{" +
            //               "\"key\": \"value\"" +
            //               "}";
            """
              class Test {
                  String json = "{" +
                                "\\"key\\": \\"value\\"" +
                                "}";
              }
              """,
            // After:
            // String json = """
            //               {\
            //               "key": "value"\
            //               }\
            //               """;
            """
              class Test {
                  String json = ""\"
                                {\\
                                "key": "value"\\
                                }\\
                                ""\";
              }
              """
          )
        );
    }

    /// Double escaping a quote in a string literal provides: " -> \" -> \\\"
    ///
    /// On converting this to a text block, the escaped backslash should remain, but we can let go of the
    /// escaping for the double quote: \\\" -> \\"
    @Test
    void doubleEscapedQuote() {
        rewriteRun(
          //language=java
          java(
            // Before:
            // String stringifiedJson = "{" +
            //                          "\\\"key\\\": \\\"value\\\"" +
            //                          "}";
            """
              class Test {
                  String stringifiedJson = "{" +
                                           "\\\\\\"key\\\\\\": \\\\\\"value\\\\\\"" +
                                           "}";
              }
              """,
            // After:
            // String stringifiedJson = """
            //                          {\
            //                          \\"key\\": \\"value\\"\
            //                          }\
            //                          """;
            """
              class Test {
                  String stringifiedJson = ""\"
                                           {\\
                                           \\\\"key\\\\": \\\\"value\\\\"\\
                                           }\\
                                           ""\";
              }
              """
          )
        );
    }

    /// Triple quotes in a string literal are escaped as: """ -> \"\"\"
    ///
    /// On converting this to a text block, only one of the quotes needs to be escaped: \"\"\" -> ""\"
    @Test
    void tripleQuotes() {
        rewriteRun(
          //language=java
          java(
            // Before:
            // String myFaceInASCII = "\"\"\"\"\"\"\"\"\n" +
            //                        "| o  o |\n" +
            //                        "|  ==  |\n" +
            //                        "\\------/\n";
            """
              class Test {
                  String myFaceInASCII = "\\"\\"\\"\\"\\"\\"\\"\\"\\n" +
                                         "| o  o |\\n" +
                                         "|  ==  |\\n" +
                                         "\\\\------/\\n";
              }
              """,
            // After:
            // String myFaceInASCII = """
            //                        ""\"""\"""
            //                        | o  o |
            //                        |  ==  |
            //                        \\------/
            //                        """;
            """
              class Test {
                  String myFaceInASCII = ""\"
                                         ""\\""\"\\""\"
                                         | o  o |
                                         |  ==  |
                                         \\\\------/
                                         ""\";
              }
              """
          )
        );
    }

    @Test
    void eightQuotes() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String eightQuotes = "\\"\\"\\"\\"\\"\\"\\"\\"" +
                                 "after 8 quotes";
              }
              """,
            """
              class Test {
                  String eightQuotes = ""\"
                                 ""\\""\"\\""\"\\
                                 after 8 quotes""\";
              }
              """
          )
        );
    }

    /// Quote in a string literal is escaped as: " -> \"
    ///
    /// On converting this to a text block, it needs to be escaped if it is the last character:
    /// ```
    ///             """
    /// "test\"" -> test\""""
    /// ```
    /// However, this is not required in case we use the newline escape (\) to put the ending delimiter on the next line:
    /// ```
    ///             """
    /// "test\"" -> test"\
    ///             """
    /// ```
    @Test
    void endingQuote() {
        rewriteRun(
          //language=java
          java(
            // Before:
            // String myPlay = "Alice: \"Hi Bob!\"\n" +
            //                 "Bob: \"Don't use plaintext Alice!\"";
            """
              class Test {
                  String myPlay = "Alice: \\"Hi Bob!\\"\\n" +
                                  "Bob: \\"Don't use plaintext Alice!\\"";
              }
              """,
            // After:
            // String myPlay = """
            //                 Alice: "Hi Bob!"
            //                 Bob: "Don't use plaintext Alice!"\
            //                 """;
            """
              class Test {
                  String myPlay = ""\"
                                  Alice: "Hi Bob!"
                                  Bob: "Don't use plaintext Alice!"\\
                                  ""\";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/260")
    @Test
        //@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/261")
    void doNotPartiallyReplaceWithTextBlocks() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void method() {
                      int variable = 1;
                      String stringWithVariableInIt =
                          "This " +
                          "is  " +
                          "text " +
                          "BEFORE the variable " +
                          variable +
                          "This " +
                          "is  " +
                          "text " +
                          "AFTER the variable. ";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/261")
    @Test
    void doNotPartiallyReplaceWithTextBlocksWithIntLiteral() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void method() {
                      String stringWithVariableInIt =
                          "This " +
                          "is  " +
                          "text " +
                          "BEFORE the variable " +
                          1 +
                          "This " +
                          "is  " +
                          "text " +
                          "AFTER the variable. ";
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/501")
    @Test
    void shouldNotUpdateKotlinCode() {
        rewriteRun(
          kotlin(
            """
              const val MULTI_LINE_MESSAGE =
                 \s"This is a multi-line message and should not be updated. " +
                         \s"This is the second sentence of such message."
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1763")
    @Test
    void noLineContinuationWhenContentHasNewlines() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, true)),
          //language=java
          java(
            """
              class Test {
                  String query = "select count(1) cls_count\\n" +
                                 "from my_table\\n" +
                                 "where a.id = b.id\\n" +
                                 "and a.ts > b.ts\\n" +
                                 "and a.id = :tag_id \\n" +
                                 "and a.stat_cd = 'ACTV'" +
                                 " and a.del_fl = 'N'" +
                                 " and a.rgn = :region";
              }
              """,
            """
              class Test {
                  String query = \"""
                                 select count(1) cls_count
                                 from my_table
                                 where a.id = b.id
                                 and a.ts > b.ts
                                 and a.id = :tag_id\\s
                                 and a.stat_cd = 'ACTV' and a.del_fl = 'N' and a.rgn = :region\""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1763")
    @Test
    void noLineContinuationInMixedConcatenation() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, true)),
          //language=java
          java(
            """
              class Test {
                  String query = "select b.id, b.val\\n" +
                                 "from my_table b, other_table c\\n" +
                                 "where c.no = :no\\n" +
                                 "and b.vrsn = c.vrsn" +
                                 " and b.rgn = :region" +
                                 " and b.del_fl = 'N'" +
                                 " and c.rgn = :region";
              }
              """,
            """
              class Test {
                  String query = \"""
                                 select b.id, b.val
                                 from my_table b, other_table c
                                 where c.no = :no
                                 and b.vrsn = c.vrsn and b.rgn = :region and b.del_fl = 'N' and c.rgn = :region\""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1763")
    @Test
    void noLineContinuationInMiddleOfMixedConcatenation() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, true)),
          //language=java
          java(
            """
              class Test {
                  String query = "select b.id\\n" +
                                 "from my_table" +
                                 " inner join other_table\\n" +
                                 "where b.id = :id\\n";
              }
              """,
            """
              class Test {
                  String query = \"""
                                 select b.id
                                 from my_table inner join other_table
                                 where b.id = :id
                                 \""";
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1110")
    @Test
    void supplementaryCharacterAsSurrogatePair() {
        // The Java parser currently returns a corrupted value/valueSource for string literals containing
        // supplementary characters encoded as surrogate pairs. Until that is fixed upstream, the recipe
        // must bail out rather than emit a broken text block that drops or re-escapes content.
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks()),
          java(
            """
             class Test {
               String json =
                   "{\\n"
                       + "  \\"euro\\": 1,\\n"
                       + "  \\"ligature\\": 2,\\n"
                       + "  \\"\\ud834\\udd20\\": 3\\n"
                       + "}";
             }""",
            src -> src.markers(javaVersion(17))));
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/555")
    @Test
    void textBlockTrailingEscape() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks()),
          java(
            """
             package com.helloworld;

             public class Main {
                 String foo =
                     "hello\\n"
                         + "world";
            }""",
            """
             package com.helloworld;

             public class Main {
                 String foo =
                     \"""
                     hello
                     world\""";
            }""",
            src -> src.markers(javaVersion(17))));
    }

    @Test
    void formatVariablesDisabledDoesNotModify() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, false)),
          java(
            """
            class Test {
                void test(final String name, final int count) {
                    String query = "SELECT * FROM\\n" +
                            name + "\\n" +
                            "WHERE count = " + count;
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesEnabledReplacesWithFormattedTextBlock() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String table, final int id) {
                    String query = "SELECT * FROM\\n" +
                            table + "\\n" +
                            "WHERE id = " + id;
                }
            }
            """,
            """
            class Test {
                void test(final String table, final int id) {
                    String query = \"""
                            SELECT * FROM
                            %s
                            WHERE id = %d\""".formatted(table, id);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesWithPercentSignsEscapesPercentSignsToDoublePercent() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String item, final int discount) {
                    String msg = "Special Offer: 100% genuine\\n" +
                            item + "\\n" +
                            "Discount: " + discount + "% off";
                }
            }
            """,
            """
            class Test {
                void test(final String item, final int discount) {
                    String msg = \"""
                            Special Offer: 100%% genuine
                            %s
                            Discount: %d%% off\""".formatted(item, discount);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesWithVariousTypesUsesCorrectFormatSpecifiers() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(
                        final long l,
                        final boolean b,
                        final char c,
                        final double d,
                        final Object obj,
                        final Integer boxedInt,
                        final Boolean boxedBool,
                        final Character boxedChar
                ) {
                    String str = "Values:\\n" +
                            l + "\\n" +
                            b + "\\n" +
                            c + "\\n" +
                            d + "\\n" +
                            obj + "\\n" +
                            boxedInt + "\\n" +
                            boxedBool + "\\n" +
                            boxedChar;
                }
            }
            """,
            """
            class Test {
                void test(
                        final long l,
                        final boolean b,
                        final char c,
                        final double d,
                        final Object obj,
                        final Integer boxedInt,
                        final Boolean boxedBool,
                        final Character boxedChar
                ) {
                    String str = \"""
                            Values:
                            %d
                            %b
                            %c
                            %s
                            %s
                            %d
                            %b
                            %c\""".formatted(l, b, c, d, obj, boxedInt, boxedBool, boxedChar);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesWithArithmeticExpressionTreatsExpressionAsSingleArgument() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final int a, final int b) {
                    String msg = "Result:\\n" +
                            (a + b) + "\\n" +
                            "Done";
                }
            }
            """,
            """
            class Test {
                void test(final int a, final int b) {
                    String msg = \"""
                            Result:
                            %d
                            Done\""".formatted((a + b));
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void avoidLineContinuationsWithVariablesRemovesBackslashContinuations() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, true, true)),
          java(
            """
            class Test {
                void test(final String table, final int id) {
                    String query = "SELECT * FROM\\n" +
                            table +
                            " WHERE id = " + id;
                }
            }
            """,
            """
            class Test {
                void test(final String table, final int id) {
                    String query = \"""
                            SELECT * FROM
                            %s WHERE id = %d\""".formatted(table, id);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesWithVariableAtStartConvertsCorrectly() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String prefix) {
                    String s = prefix + "\\n" +
                            "second line";
                }
            }
            """,
            """
            class Test {
                void test(final String prefix) {
                    String s = \"""
                            %s
                            second line\""".formatted(prefix);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void multilineWithoutNewlinesInContentNotConvertedWhenConvertStringsWithoutNewlinesFalse() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(false, false, true)),
          java(
            """
            class Test {
                void test(final String name) {
                    String s = "Hello, " +
                            name +
                            "!";
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void multilineWithoutNewlinesInContentConvertedWhenConvertStringsWithoutNewlinesTrue() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String name) {
                    String s = "Hello, " +
                            name +
                            "!";
                }
            }
            """,
            """
            class Test {
                void test(final String name) {
                    String s = \"""
                            Hello, \\
                            %s\\
                            !\""".formatted(name);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    // Edge Cases

    @Test
    void formatVariablesEdgeCaseAdjacentVariablesWithoutSeparator() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String first, final String last) {
                    String name = "User:\\n" +
                            first + last;
                }
            }
            """,
            """
            class Test {
                void test(final String first, final String last) {
                    String name = \"""
                            User:
                            %s%s\""".formatted(first, last);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesEdgeCaseWithEscapedQuotesInStringLiterals() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String value) {
                    String json = "{\\n" +
                            "  \\"key\\": \\"" + value + "\\"\\n" +
                            "}";
                }
            }
            """,
            """
            class Test {
                void test(final String value) {
                    String json = \"""
                            {
                              "key": "%s"
                            }\\
                            \""".formatted(value);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesEdgeCaseWithTrailingSpacesPreserved() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Test {
                void test(final String greeting, final String name) {
                    String msg = greeting + "   \\n" +
                            "to: " + name;
                }
            }
            """,
            """
            class Test {
                void test(final String greeting, final String name) {
                    String msg = \"""
                            %s  \\s
                            to: %s\""".formatted(greeting, name);
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    void formatVariablesEdgeCaseMethodInvocationsAndFieldAccessAsArguments() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(true, false, true)),
          java(
            """
            class Person {
                String name;
                int getAge() { return 30; }
            }
            class Test {
                void test(final Person person) {
                    String info = "Person:\\n" +
                            person.name + "\\n" +
                            "Age: " + person.getAge();
                }
            }
            """,
            """
            class Person {
                String name;
                int getAge() { return 30; }
            }
            class Test {
                void test(final Person person) {
                    String info = \"""
                            Person:
                            %s
                            Age: %d\""".formatted(person.name, person.getAge());
                }
            }
            """,
            src -> src.markers(javaVersion(17))
          )
        );
    }
}



