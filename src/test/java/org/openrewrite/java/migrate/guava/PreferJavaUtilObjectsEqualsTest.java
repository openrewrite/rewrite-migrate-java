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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/434")
class PreferJavaUtilObjectsEqualsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.PreferJavaUtilObjectsEquals")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"))
          .cycles(2)
          .expectedCyclesThatMakeChanges(1);
    }

    @DocumentExample
    @Test
    void qualifyCallsInsidePublicEqualsOverride() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Objects.equal;

              class Value {
                  Object left;
                  Object right;

                  @Override
                  public boolean equals(Object other) {
                      if (!(other instanceof Value)) {
                          return false;
                      }
                      Value that = (Value) other;
                      return equal(left, that.left) && equal(right, that.right);
                  }
              }
              """,
            """
              class Value {
                  Object left;
                  Object right;

                  @Override
                  public boolean equals(Object other) {
                      if (!(other instanceof Value)) {
                          return false;
                      }
                      Value that = (Value) other;
                      return java.util.Objects.equals(left, that.left) && java.util.Objects.equals(right, that.right);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotRebindToLocalStaticEquals() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Objects.equal;

              class Comparison {
                  final StringBuilder trace = new StringBuilder();

                  static boolean equals(Object left, Object right) {
                      return !java.util.Objects.equals(left, right);
                  }

                  Object argument(String label) {
                      trace.append(label);
                      return "same";
                  }

                  boolean compare() {
                      return equal(argument("L"), argument("R"));
                  }
              }
              """,
            """
              class Comparison {
                  final StringBuilder trace = new StringBuilder();

                  static boolean equals(Object left, Object right) {
                      return !java.util.Objects.equals(left, right);
                  }

                  Object argument(String label) {
                      trace.append(label);
                      return "same";
                  }

                  boolean compare() {
                      return java.util.Objects.equals(argument("L"), argument("R"));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateQualifiedCallWithoutCollision() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Objects;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return Objects.equal(left, right);
                  }
              }
              """,
            """
              import java.util.Objects;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainGuavaHashCodeAndMigrateMethodReference() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Objects;

              import java.util.function.BiPredicate;

              import static com.google.common.base.Objects.equal;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }

                  int hash(Object left, Object right) {
                      return Objects.hashCode(left, right);
                  }

                  BiPredicate<Object, Object> comparator() {
                      return Objects::equal;
                  }
              }
              """,
            """
              import com.google.common.base.Objects;

              import java.util.function.BiPredicate;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return java.util.Objects.equals(left, right);
                  }

                  int hash(Object left, Object right) {
                      return Objects.hashCode(left, right);
                  }

                  BiPredicate<Object, Object> comparator() {
                      return java.util.Objects::equals;
                  }
              }
              """
          )
        );
    }

    @Test
    void retainImportedApplicationObjects() {
        rewriteRun(
          java(
            """
              package application;

              public class Objects {
                  public static boolean equals(Object left, Object right) {
                      return !java.util.Objects.equals(left, right);
                  }
              }
              """
          ),
          java(
            """
              package example;

              import application.Objects;

              import static com.google.common.base.Objects.equal;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }

                  boolean applicationComparison(Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """,
            """
              package example;

              import application.Objects;

              class Comparison {
                  boolean compare(Object left, Object right) {
                      return java.util.Objects.equals(left, right);
                  }

                  boolean applicationComparison(Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifyWhenEnclosingTypeIsNamedObjects() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Objects.equal;

              class Objects {
                  static boolean equals(Object left, Object right) {
                      return false;
                  }

                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """,
            """
              class Objects {
                  static boolean equals(Object left, Object right) {
                      return false;
                  }

                  boolean compare(Object left, Object right) {
                      return java.util.Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveUnrelatedLocalEqualAndEqualsUntouched() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Objects;

              class Comparison {
                  static boolean equal(Object left, Object right) {
                      return false;
                  }

                  static boolean equals(Object left, Object right) {
                      return true;
                  }

                  boolean applicationComparison(Object left, Object right) {
                      return equal(left, right) || equals(left, right);
                  }

                  boolean guavaComparison(Object left, Object right) {
                      return Objects.equal(left, right);
                  }
              }
              """,
            """
              import java.util.Objects;

              class Comparison {
                  static boolean equal(Object left, Object right) {
                      return false;
                  }

                  static boolean equals(Object left, Object right) {
                      return true;
                  }

                  boolean applicationComparison(Object left, Object right) {
                      return equal(left, right) || equals(left, right);
                  }

                  boolean guavaComparison(Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifyWhenAParameterIsNamedObjectsAndKeepJavaUtilOnlySource() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Objects.equal;

              class Comparison {
                  static class LocalObjects {
                      static boolean equals(Object left, Object right) {
                          return false;
                      }
                  }

                  boolean compare(Object left, Object right, LocalObjects Objects) {
                      return equal(left, right);
                  }
              }
              """,
            """
              class Comparison {
                  static class LocalObjects {
                      static boolean equals(Object left, Object right) {
                          return false;
                      }
                  }

                  boolean compare(Object left, Object right, LocalObjects Objects) {
                      return java.util.Objects.equals(left, right);
                  }
              }
              """
          ),
          java(
            """
              import java.util.Objects;

              class JavaUtilOnlyComparison {
                  boolean compare(Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifyWhenANestedTypeIsNamedObjects() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Objects.equal;

              class Comparison {
                  static class Objects {
                      static boolean equals(Object left, Object right) {
                          return false;
                      }
                  }

                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """,
            """
              class Comparison {
                  static class Objects {
                      static boolean equals(Object left, Object right) {
                          return false;
                      }
                  }

                  boolean compare(Object left, Object right) {
                      return java.util.Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveGuavaWhenJavaPackageNameIsShadowed() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import static com.google.common.base.Objects.equal;

              class PrefixCase {
                  static Object java;

                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCuWhenJavaParameterShadowsExistingJdkUse() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import java.util.Objects;

              import static com.google.common.base.Objects.equal;

              class ParameterPrefix {
                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }

                  boolean existingJavaComparison(Object java, Object left, Object right) {
                      return Objects.equals(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCuWhenJavaPrefixIsAType() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(0),
          java(
            """
              import static com.google.common.base.Objects.equal;

              class NestedTypePrefix {
                  static class java {
                  }

                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """
          ),
          java(
            """
              import static com.google.common.base.Objects.equal;

              class LocalTypePrefix {
                  boolean compare(Object left, Object right) {
                      class java {
                      }
                      return equal(left, right);
                  }
              }
              """
          ),
          java(
            """
              import static com.google.common.base.Objects.equal;

              class GenericTypePrefix<java> {
                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """
          ),
          java(
            """
              package application;

              public class java {
              }
              """
          ),
          java(
            """
              package example;

              import application.java;

              import static com.google.common.base.Objects.equal;

              class ImportedTypePrefix {
                  boolean compare(Object left, Object right) {
                      return equal(left, right);
                  }
              }
              """
          )
        );
    }
}
