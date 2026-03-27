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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class AddSealedClassModifierTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddSealedClassModifier())
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Test
    void sealClassWithPrivateConstructorAndNestedFinalSubclasses() {
        rewriteRun(
          //language=java
          java(
            """
              public class Shape {
                  private Shape() {}
                  public static final class Circle extends Shape {}
                  public static final class Square extends Shape {}
              }
              """,
            """
              public sealed class Shape permits Circle, Square {
                  private Shape() {}
                  public static final class Circle extends Shape {}
                  public static final class Square extends Shape {}
              }
              """
          )
        );
    }

    @Test
    void sealAbstractClassWithNestedSubclasses() {
        rewriteRun(
          //language=java
          java(
            """
              public abstract class Expr {
                  private Expr() {}
                  public static final class Literal extends Expr {
                      final int value;
                      Literal(int value) { this.value = value; }
                  }
                  public static final class Add extends Expr {
                      final Expr left, right;
                      Add(Expr left, Expr right) { this.left = left; this.right = right; }
                  }
              }
              """,
            """
              public abstract sealed class Expr permits Literal, Add {
                  private Expr() {}
                  public static final class Literal extends Expr {
                      final int value;
                      Literal(int value) { this.value = value; }
                  }
                  public static final class Add extends Expr {
                      final Expr left, right;
                      Add(Expr left, Expr right) { this.left = left; this.right = right; }
                  }
              }
              """
          )
        );
    }

    @Test
    void sealInterfaceWithNestedRecordImplementors() {
        rewriteRun(
          //language=java
          java(
            """
              public interface Event {
                  record Click(int x, int y) implements Event {}
                  record KeyPress(char key) implements Event {}
              }
              """,
            """
              public sealed interface Event permits Click, KeyPress {
                  record Click(int x, int y) implements Event {}
                  record KeyPress(char key) implements Event {}
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void doNotSealClassWithPublicConstructor() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Base {
                      public Base() {}
                      public static final class Sub extends Base {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealClassWithProtectedConstructor() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Base {
                      protected Base() {}
                      public static final class Sub extends Base {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealClassWithDefaultConstructor() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Base {
                      public static final class Sub extends Base {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealClassWithNoSubclasses() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Lonely {
                      private Lonely() {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealAlreadySealedClass() {
            rewriteRun(
              //language=java
              java(
                """
                  public sealed class Shape permits Shape.Circle {
                      private Shape() {}
                      public static final class Circle extends Shape {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealWhenSubclassExistsOutsideClass() {
            rewriteRun(
              //language=java
              java(
                """
                  package p;
                  public class Parent {
                      private Parent() {}
                      static final class Nested extends Parent {}
                  }
                  """
              ),
              //language=java
              java(
                """
                  package p;
                  public final class External extends Parent {
                      private External() {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealEnum() {
            rewriteRun(
              //language=java
              java(
                """
                  public enum Color {
                      RED, GREEN, BLUE
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealWhenNestedSubclassIsNotFinal() {
            rewriteRun(
              //language=java
              java(
                """
                  public class Base {
                      private Base() {}
                      public static class Sub extends Base {}
                  }
                  """
              )
            );
        }

        @Test
        void doNotSealWithJava16() {
            rewriteRun(
              s -> s.allSources(src -> src.markers(javaVersion(16))),
              //language=java
              java(
                """
                  public class Shape {
                      private Shape() {}
                      public static final class Circle extends Shape {}
                  }
                  """
              )
            );
        }
    }
}
