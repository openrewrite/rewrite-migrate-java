/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UsePredicateNotTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UsePredicateNot())
          .parser(JavaParser.fromJavaVersion());
    }

    @DocumentExample
    @Test
    void castOfMethodReference() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> notEmpty = ((Predicate<String>) String::isEmpty).negate();
                }
                """,
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> notEmpty = Predicate.not(String::isEmpty);
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void castOfLambda() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> notEmpty = ((Predicate<String>) s -> s.isEmpty()).negate();
                }
                """,
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> notEmpty = Predicate.not(s -> s.isEmpty());
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void insideStreamFilter() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                import java.util.List;
                import java.util.function.Predicate;
                import java.util.stream.Collectors;

                class A {
                    List<String> nonEmpty(List<String> in) {
                        return in.stream()
                            .filter(((Predicate<String>) String::isEmpty).negate())
                            .collect(Collectors.toList());
                    }
                }
                """,
              """
                import java.util.List;
                import java.util.function.Predicate;
                import java.util.stream.Collectors;

                class A {
                    List<String> nonEmpty(List<String> in) {
                        return in.stream()
                            .filter(Predicate.not(String::isEmpty))
                            .collect(Collectors.toList());
                    }
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void unchangedWhenNoCast() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> p = String::isEmpty;
                    Predicate<String> notEmpty = p.negate();
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void unchangedOnJava8() {
        rewriteRun(
          //language=java
          version(
            java(
              """
                import java.util.function.Predicate;

                class A {
                    Predicate<String> notEmpty = ((Predicate<String>) String::isEmpty).negate();
                }
                """
            ),
            8
          )
        );
    }
}
