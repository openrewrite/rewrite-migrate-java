/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.*;

class UseTextBlocksTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseTextBlocks())
          .allSources(s -> s.markers(javaVersion(17)));
    }

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
                          WHERE something = 1; \\
                          \""";
              }
              """
          )
        );
    }

    @Test
    void preserveTrailingWhiteSpaces2() {
        rewriteRun(
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
        return spec -> spec.parser(JavaParser.fromJavaVersion().styles(singletonList(
            new NamedStyles(
              randomId(), "TabsOnlyFile", "TabsOnlyFile", "tabSize is x", emptySet(),
              singletonList(with.apply(buildTabsAndIndents(tabSize)))
            )
          )));
    }

    private static TabsAndIndentsStyle buildTabsAndIndents(int tabSize) {
        return new TabsAndIndentsStyle(true, tabSize, tabSize, tabSize * 2, false,
          new TabsAndIndentsStyle.MethodDeclarationParameters(true));
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

    @Disabled("""
            This test is to demonstrate that the text block conversion is correct.
            In this test, s1, s2, and s3 are equivalent, and we translate s1 to s2.
            """)
    @Test
    void textBlockDemo() {
        String s1 = "\n========================================================="
                    + "\n                                                         "
                    + "\n          Welcome to Spring Integration!                 "
                    + "\n                                                         "
                    + "\n    For more information please visit:                   "
                    + "\n    https://www.springsource.org/spring-integration      "
                    + "\n                                                         "
                    + "\n=========================================================";
        String s2 = """
                    
                    =========================================================
                                                                            \s
                              Welcome to Spring Integration!                \s
                                                                            \s
                        For more information please visit:                  \s
                        https://www.springsource.org/spring-integration     \s
                                                                            \s
                    =========================================================\
                    """;
        String s3 = """
                    \n=========================================================\
                    \n                                                         \
                    \n          Welcome to Spring Integration!                 \
                    \n                                                         \
                    \n    For more information please visit:                   \
                    \n    https://www.springsource.org/spring-integration      \
                    \n                                                         \
                    \n=========================================================\
                    """;
        assertThat(s1).isEqualTo(s2).isEqualTo(s3);
    }

    @Test
    void newlineAtBeginningOfLines() {
        rewriteRun(
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
    void consecutiveNewLines() {
        rewriteRun(
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

    /**
     * Single escaping a quote in a string literal provides: " -> \"
     * <p>
     * On converting this to a text block, we can let go of the escaping for the double quote: \" -> "
     */
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

    /**
     * Double escaping a quote in a string literal provides: " -> \" -> \\\"
     * <p>
     * On converting this to a text block, the escaped backslash should remain, but we can let go of the
     * escaping for the double quote: \\\" -> \\"
     */
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

    @Disabled
    @Test
    void grouping() {
        rewriteRun(
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
              """,
            """
              public class Test {
                  public void method() {
                      int variable = 1;
                      String stringWithVariableInIt =
                          \"""
                          This \\
                          is  \\
                          text \\
                          BEFORE the variable \\
                          \"""
                          This \\
                          is \\
                          text \\
                          AFTER the variable. \\
                          \""";
                  }
              }
              """
          )
        );
    }
}
