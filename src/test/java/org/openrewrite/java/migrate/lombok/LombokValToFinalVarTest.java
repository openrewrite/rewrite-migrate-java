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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class LombokValToFinalVarTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValToFinalVar())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @DocumentExample
    @Test
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

    @Test
    void preserveStarImportWithoutVarUsage() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion())
            .typeValidationOptions(TypeValidation.none()),
          version(
            java(
              """
                import lombok.*;

                @AllArgsConstructor
                @NoArgsConstructor
                @ToString
                @Data
                @EqualsAndHashCode
                public class AuthHeaders {
                    String authHeader;
                    String token;
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void preserveStarImportWithVarUsage() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion())
            .typeValidationOptions(TypeValidation.none()),
          version(
            java(
              """
                import lombok.*;

                @Data
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
    void removeExplicitVarImport() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion())
            .typeValidationOptions(TypeValidation.none()),
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
    void removeStarImportWhenOnlyValUsed() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion())
            .typeValidationOptions(TypeValidation.none()),
          version(
            java(
              """
                import lombok.*;
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
    void starImportRemainsWhenOnlyVarUsed() {
        // When `import lombok.*;` exists only for `var`, the star import remains unused after
        // the recipe runs. This is acceptable: removing a star import with incomplete type info
        // (e.g. in multi-module projects) risks breaking compilation by dropping imports that
        // other lombok types still need. An unused import is preferable to broken code.
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion())
            .typeValidationOptions(TypeValidation.none()),
          version(
            java(
              """
                import lombok.*;
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

    @Nested
    @SuppressWarnings({"StatementWithEmptyBody", "RedundantOperationOnEmptyContainer"})
    class ValInLoop {
        @Test
        void list() {
            //language=java
            rewriteRun(
              version(
                java(
                  """
                    import lombok.val;

                    import java.util.List;

                    class A {
                        void bar(List<String> lst) {
                            for (val s : lst) {}
                        }
                    }
                    """,
                  """
                    import java.util.List;

                    class A {
                        void bar(List<String> lst) {
                            for (var s : lst) {}
                        }
                    }
                    """
                ),
                17
              )
            );
        }

        @Test
        void array() {
            //language=java
            rewriteRun(
              version(
                java(
                  """
                    import lombok.val;
                    import java.util.List;
                    import java.util.ArrayList;

                    class A {
                        void bar(String[] lst) {
                            for (val s : lst) {}
                        }
                    }
                    """,
                  """
                    import java.util.List;
                    import java.util.ArrayList;

                    class A {
                        void bar(String[] lst) {
                            for (var s : lst) {}
                        }
                    }
                    """
                ),
                17
              )
            );
        }

        @Test
        void valuesFromMethod() {
            //language=java
            rewriteRun(
              java(
                """
                  interface Mapper {
                      String[] getNamesList();
                  }
                  """
              ),
              version(
                java(
                  """
                    import lombok.val;

                    class A {
                        void bar(Mapper mapper) {
                            for (val s : mapper.getNamesList()) {}
                        }
                    }
                    """,
                  """
                    class A {
                        void bar(Mapper mapper) {
                            for (var s : mapper.getNamesList()) {}
                        }
                    }
                    """
                ),
                17
              )
            );
        }
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
