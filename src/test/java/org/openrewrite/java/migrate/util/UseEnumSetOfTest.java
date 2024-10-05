/*
 * Copyright 2022 the original author or authors.
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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings("UnusedAssignment")
class UseEnumSetOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseEnumSetOf());
    }

    @Test
    void changeDeclaration() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method() {
                        Set<Color> warm = Set.of(Color.RED, Color.GREEN);
                    }
                }
                """,
              """
                import java.util.EnumSet;
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method() {
                        Set<Color> warm = EnumSet.of(Color.RED, Color.GREEN);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void changeAssignment() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method() {
                        Set<Color> warm;
                        warm = Set.of(Color.RED);
                    }
                }
                """,
              """
                import java.util.EnumSet;
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method() {
                        Set<Color> warm;
                        warm = EnumSet.of(Color.RED);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/516")
    void dontChangeVarargs() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method(final Color... colors) {
                        Set<Color> s = Set.of(colors);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/516")
    void dontChangeArray() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                
                class Test {
                    public enum Color {
                        RED, GREEN, BLUE
                    }
                    public void method() {
                        Color[] colors = {};
                        Set<Color> s = Set.of(colors);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/490")
    void dontHaveArgs() {
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import java.util.concurrent.TimeUnit;
                class Test {
                
                    public void method() {
                        Set<TimeUnit> warm = Set.of();
                    }
                }
                """,
              """
                import java.util.EnumSet;
                import java.util.Set;
                import java.util.concurrent.TimeUnit;
                
                class Test {
                
                    public void method() {
                        Set<TimeUnit> warm = EnumSet.noneOf(TimeUnit.class);
                    }
                }
                """
            ),
            9
          )
        );
    }


}
