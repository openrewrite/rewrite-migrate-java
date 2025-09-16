/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
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

    @DocumentExample
    @Test
    void relocateSuperAfterIf() {
        rewriteRun(
          java(
            """
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Employee extends Person {
                Employee(int age) {
                    super(age);
                    if (age < 18 || age > 67)
                        throw new IllegalArgumentException("Age must be between 18 and 67");
                }
            }
            """,
            """
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Employee extends Person {
                Employee(int age) {
                    if (age < 18 || age > 67)
                        throw new IllegalArgumentException("Age must be between 18 and 67");
                    super(age);
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
            """
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Employee extends Person {
                String officeID;
                String department;
                Employee(int age, String officeID, String department) {
                    super(age);
                    if (age < 18 || age > 67)
                        throw new IllegalArgumentException("Age must be between 18 and 67");
                    if (department == null)
                        throw new IllegalArgumentException("Department cannot be null");
                    this.officeID = officeID;
                    this.department = department;
                }
            }
            """,
            """
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Employee extends Person {
                String officeID;
                String department;
                Employee(int age, String officeID, String department) {
                    if (age < 18 || age > 67)
                        throw new IllegalArgumentException("Age must be between 18 and 67");
                    if (department == null)
                        throw new IllegalArgumentException("Department cannot be null");
                    this.officeID = officeID;
                    this.department = department;
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
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Outer {
                int i;
                void hello() { System.out.println("Hello"); }
                class Inner extends Person {
                    int j;
                    Inner(int age, int j) {
                        super(age);
                        this.j = j;
                    }
                }
            }
            """,
            """
            class Person {
                int age;
                Person(int age) {
                    if (age < 0)
                        throw new IllegalArgumentException("Age cannot be negative");
                    this.age = age;
                }
            }
            class Outer {
                int i;
                void hello() { System.out.println("Hello"); }
                class Inner extends Person {
                    int j;
                    Inner(int age, int j) {
                        this.j = j;
                        super(age);
                    }
                }
            }
            """
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
