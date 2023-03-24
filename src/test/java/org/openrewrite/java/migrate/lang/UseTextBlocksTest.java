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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UseTextBlocksTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseTextBlocks());
    }

    @Test
    void regular() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void preserveTrailingWhiteSpaces() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void preserveTrailingWhiteSpaces2() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void indentsAlignment() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void multipleLinesWithAdditionBinaryAtLineFront() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void noChangeForStringBlocks() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void preferChangeIfNoNewLineInContent() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void preferNoChangeIfNoNewLineInContent() {
        rewriteRun(
          spec -> spec.recipe(new UseTextBlocks(false)),
          //language=java
          version(
            java(
              """
                class Test {
                    String query = "SELECT * FROM " +
                            "my_table " +
                            "WHERE something = 1;";
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void noChangeIfNoNewLineInConcatenation() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                class Test {
                    String query = "SELECT * FROM \\n" + "my_table \\n" + "WHERE something = 1;";
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void emptyStringOnFirstLine() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void startsOnNextLine() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Test
    void indents() {
        rewriteRun(
          //language=java
          version(
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
            ),
            17
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/195")
    @Test
    void newlinesAlignment() {
        rewriteRun(
          //language=java
          version(
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
            ), 17
          )
        );
    }

    /**
     * In this test, s1, s2 and s3 are equivalent, and we do translate s1 to s2.
     *         String s1 = "\n========================================================="
     *                 + "\n                                                         "
     *                 + "\n          Welcome to Spring Integration!                 "
     *                 + "\n                                                         "
     *                 + "\n    For more information please visit:                   "
     *                 + "\n    https://www.springsource.org/spring-integration      "
     *                 + "\n                                                         "
     *                 + "\n=========================================================";
     *         String s2 = """
     *                     =========================================================
     *                                                                             \s
     *                               Welcome to Spring Integration!                \s
     *                                                                             \s
     *                         For more information please visit:                  \s
     *                         https://www.springsource.org/spring-integration     \s
     *                                                                             \s
     *                     =========================================================\
     *                     """;
     *         String s3 = """
     *                     \n=========================================================\
     *                     \n                                                         \
     *                     \n          Welcome to Spring Integration!                 \
     *                     \n                                                         \
     *                     \n    For more information please visit:                   \
     *                     \n    https://www.springsource.org/spring-integration      \
     *                     \n                                                         \
     *                     \n=========================================================\
     *                     """;
     */
    @Test
    void newlineAtBeginningOfLines() {
        rewriteRun(
          version(
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
            ), 17
          )
        );
    }
}
