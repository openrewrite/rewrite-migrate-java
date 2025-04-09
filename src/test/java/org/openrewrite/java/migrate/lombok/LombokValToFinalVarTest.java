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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class LombokValToFinalVarTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValToFinalVar())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    @DocumentExample
    void replaceAssignmentVal() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                class A {
                    void bar() {
                        val foo = "foo";
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        final var foo = "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void replaceAssignmentVar() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.var;
                class A {
                    void bar() {
                        var foo = "foo";
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        var foo = "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void preserveStarImport() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.*;

                @Getter
                @Setter
                @NoArgsConstructor
                @Data
                @Value
                class A {
                    void bar() {
                        var foo = "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @SuppressWarnings({"StatementWithEmptyBody", "RedundantOperationOnEmptyContainer"})
    @Test
    void valInForEachStatement() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                import java.util.List;
                import java.util.ArrayList;

                class A {
                    void bar() {
                        List<String> lst = new ArrayList<>();
                        for (val s : lst) {}
                    }
                }
                """,
              """
                import java.util.List;
                import java.util.ArrayList;

                class A {
                    void bar() {
                        List<String> lst = new ArrayList<>();
                        for (final var s : lst) {}
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void retainPrefixComment() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                class A {
                    void bar() {
                        // Prefix
                        val foo = "foo";
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        // Prefix
                        final var foo = "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void retainInfixComment() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                class A {
                    void bar() {
                        val foo =
                        // Infix
                        "foo";
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        final var foo =
                        // Infix
                        "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void retainSuffixComment() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                class A {
                    void bar() {
                        val foo = 43;
                        // Suffix
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        final var foo = 43;
                        // Suffix
                    }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void retainWhitespace() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.val;
                class A {
                    void bar() {
                        val foo =
                            "foo";
                    }
                }
                """,
              """
                class A {
                    void bar() {
                        final var foo =
                            "foo";
                    }
                }
                """
            ),
            17
          )
        );
    }
}
