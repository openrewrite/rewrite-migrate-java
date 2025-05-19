package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class RelocateSuperCallTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RelocateSuperCall())
          .allSources(src -> src.markers(javaVersion(25)));

    }
    @Test
    void relocateSuperAfterIf() {
        rewriteRun(
          java(
            """
            class A {
                public A(String bar) {
                    super();
                    if(bar.equals("test"))
                        throw new RuntimeException();
                }
            }
            """,
            """
            class A {
                public A(String bar) {
                    if(bar.equals("test"))
                        throw new RuntimeException();
                    super();
                }
            }
            """
          )
        );
    }

    @Test
    void relocateSuperAfterIfStatement() {
        rewriteRun(
          java(
            // language=java
            """
            class Person {
                final int age;
                public Person(int age) {
                    if (age < 0) {
                        throw new IllegalArgumentException("Invalid age");
                    }
                    this.age = age;
                }
            }

            class Employee extends Person {
                public Employee(int age) {
                    super(age);
                    if (age < 18 || age > 67) {
                        throw new IllegalArgumentException("Invalid employee age");
                    }
                }
            }
            """,
            // Expected output
            """
            class Person {
                final int age;
                public Person(int age) {
                    if (age < 0) {
                        throw new IllegalArgumentException("Invalid age");
                    }
                    this.age = age;
                }
            }

            class Employee extends Person {
                public Employee(int age) {
                    if (age < 18 || age > 67) {
                        throw new IllegalArgumentException("Invalid employee age");
                    }
                    super(age);
                }
            }
            """
          )
        );
    }

    @Test
    void relocateSuperWithSafeFieldAssignmentOnly() {
        rewriteRun(
          java(
            """
            class Outer {
                class Inner {
                    int x;
                    int y = 100;

                    Inner(int input) {
                        super();
                        x = input;
                        y = 200;
                    }
                }
            }
            """,
            // Note: `y = 200` is illegal under early construction context since `y` has initializer
            // So the expected result is same as input if `super()` is already last.
            """
            class Outer {
                class Inner {
                    int x;
                    int y = 100;

                    Inner(int input) {
                        x = input;
                        y = 200;
                        super();
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void nprelocateSuperInInnerClassIfAlreadyAsLast_withSafeAssignments() {
        rewriteRun(
          java(
            // language=java (before transformation)
            """
            class Outer {
                class Inner {
                    int x;
                    int y;

                    Inner(int input) {
                        var tmp = input * 2;
                        x = tmp;
                        y = 42;
                        super();
                    }
                }
            }
            """,
            // // No Change expected as super() valid with JDK25 version
            spec -> spec.markers(javaVersion(25))
          )
        );
    }

    @Test
    void noRelocateSuperAfterIf_givenBelowJDK25Version() {
        rewriteRun(
          java(
            // Input
            """
            class A {
                public A(String bar) {
                    super();
                    if(bar.equals("test"))
                        throw new RuntimeException();
                }
            }
            """,
            // Simulate Java 8 environment
            spec -> spec.markers(javaVersion(8))
          )
        );
    }

    @Test
    void relocateSuperInInnerClass_withSafeAssignments() {
        rewriteRun(
          java(
            // language=java (before transformation)
            """
            class Outer {
                class Inner {
                    int x;
                    int y;

                    Inner(int input) {
                        super();
                        var tmp = input * 2;
                        x = tmp;
                        y = 42;
                    }
                }
            }
            """,
            spec -> spec.markers(javaVersion(8))
          )
        );
    }
}

