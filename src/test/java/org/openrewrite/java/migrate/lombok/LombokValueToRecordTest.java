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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.*;

@SuppressWarnings({"ClassCanBeRecord", "LombokGetterMayBeUsed", "NullableProblems", "TypeParameterExplicitlyExtendsObject"})
class LombokValueToRecordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValueToRecord(false))
          .allSources(s -> s.markers(javaVersion(17)))
          .parser(JavaParser.fromJavaVersion()
            .classpath(
              "lombok",
              "jackson-core"
            )
          );
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/141")
    @Test
    void convertOnlyValueAnnotatedClassWithoutDefaultValuesToRecord() {
        //language=java
        rewriteRun(
          java(
            """
              package example;

              import lombok.Value;

              @Value
              public class A {
                  String test;
              }
              """,
            """
              package example;

              public record A(
                      String test) {
              }
              """
          ),
          java(
            """
              package example;

              public class UserOfA {

                  private final A record;

                  public UserOfA() {
                      this.record = new A("some value");
                  }

                  public String getRecordValue() {
                      return record.getTest();
                  }
              }
              """,
            """
              package example;

              public class UserOfA {

                  private final A record;

                  public UserOfA() {
                      this.record = new A("some value");
                  }

                  public String getRecordValue() {
                      return record.test();
                  }
              }
              """
          )
        );
    }

    @Test
    void valueAnnotatedClassWithUseExactOptionKeepsLombokToString() {
        //language=java
        rewriteRun(
          s -> s.recipe(new LombokValueToRecord(true)),
          java(
            """
              import lombok.Value;

              @Value
              public class Test {
                  String field1;

                  String field2;
              }
              """,
            """
              public record Test(
                      String field1,

                      String field2) {
                  @Override
                  public String toString() {
                      return "Test(" +
                              "field1=" + field1 + ", " +
                              "field2=" + field2 +
                              ")";
                  }
              }
              """
          )
        );
    }

    @Test
    void onlyRemoveAnnotationFromRecords() {
        //language=java
        rewriteRun(
          java(
            """
              package example;

              import lombok.ToString;
              import lombok.Value;

              @Value
              public class A {
                  String test;
              }

              @Value
              @ToString
              public class B {
                  String test;
              }
              """,
            """
              package example;

              import lombok.ToString;
              import lombok.Value;

              public record A(
                      String test) {
              }

              @Value
              @ToString
              public class B {
                  String test;
              }
              """
          )
        );
    }

    @Test
    void innerRecordsNotStatic() {
        //language=java
        rewriteRun(
          java(
            """
              package example;

              import lombok.Value;

              public class A {
                  @Value
                  static class B {
                      String test;
                  }
              }
              """,
            """
              package example;

              public class A {
                  record B(
                          String test) {
                  }
              }
              """
          )
        );
    }

    @Test
    void interfaceIsImplementedThatDoesNotDefineFieldGetter() {
        //language=java
        rewriteRun(
          java(
            """
              package example;

              import lombok.Value;
              import java.io.Serializable;

              @Value
              public class A implements Serializable {
                  String test;
              }
              """,
            """
              package example;

              import java.io.Serializable;

              public record A(
                      String test) implements Serializable {
              }
              """
          )
        );
    }

    @Test
    void plainLombokBuilder() {
        //language=java
        rewriteRun(
          java(
            """
              package example;

              import lombok.Value;
              import lombok.Builder;
              import java.io.Serializable;

              @Value
              @Builder
              public class A implements Serializable {
                  String test;
              }
              """,
            """
              package example;

              import lombok.Builder;
              import java.io.Serializable;

              @Builder
              public record A(
                      String test) implements Serializable {
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/812")
    @Test
    void booleanFieldWithIsGetter() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Value;

              @Value
              public class Foo {
                  boolean bar;
              }
              """,
            """
              public record Foo(
                      boolean bar) {
              }"""
          ),
          java(
            """
              public class Baz {
                  public void baz(Foo foo) {
                      foo.isBar();
                  }
              }
              """,
            """
              public class Baz {
                  public void baz(Foo foo) {
                      foo.bar();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/812")
    @Test
    void multipleBooleanFields() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Value;

              @Value
              public class Config {
                  boolean enabled;
                  Boolean active;
                  String name;
              }
              """,
            """
              public record Config(
                      boolean enabled,
                      Boolean active,
                      String name) {
              }
              """
          ),
          java(
            """
              public class ConfigUser {
                  public void useConfig(Config config) {
                      if (config.isEnabled() && config.getActive()) {
                          System.out.println(config.getName());
                      }
                  }
              }
              """,
            """
              public class ConfigUser {
                  public void useConfig(Config config) {
                      if (config.enabled() && config.active()) {
                          System.out.println(config.name());
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/449")
    @Test
    void methodReferences() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Value;
              import java.util.function.Supplier;

              @Value
              class A {
                  String test;
              }

              class Using {
                 Supplier<String> usingMethodReference() {
                    A a = new A("foo");
                    return a::getTest;
                }
              }
              """,
            """
              import java.util.function.Supplier;

              record A(
                      String test) {
              }

              class Using {
                 Supplier<String> usingMethodReference() {
                    A a = new A("foo");
                    return a::test;
                }
              }
              """
          )
        );
    }

    @Nested
    class Unchanged {
        @Test
        void classWithExplicitConstructor() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;

                  @Value
                  public class A {
                     String test;

                     public A() {
                         this.test = "test";
                     }
                  }
                  """
              )
            );
        }

        @Test
        void classWithFieldAnnotations() {
            //language=java
            rewriteRun(
              s -> s.typeValidationOptions(TypeValidation.all().identifiers(false)),
              java(
                """
                  import com.fasterxml.jackson.annotation.JsonProperty;
                  import lombok.Value;

                  @Value
                  public class A {

                     @JsonProperty
                     String test;
                  }
                  """
              )
            );
        }

        @Test
        void classWithExplicitMethods() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;

                  @Value
                  public class A {
                     String test;

                     public String getTest() {
                         return test;
                     }
                  }
                  """
              )
            );
        }

        @Test
        void genericClass() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;

                  @Value
                  public class A<T extends Object> {
                     T test;
                  }
                  """
              )
            );
        }

        @Test
        void nonJava17Class() {
            //language=java
            rewriteRun(
              version(
                java(
                  """
                    import lombok.Value;

                    @Value
                    public class A {
                        String test;
                    }
                    """
                ),
                11
              )
            );
        }

        @Test
        void classWithMultipleLombokAnnotations() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;
                  import lombok.experimental.Accessors;

                  @Value
                  @Accessors(fluent = true)
                  public class A {
                      String test;
                  }
                  """
              )
            );
        }

        @Test
        void existingRecordsAreUnchanged() {
            //language=java
            rewriteRun(
              java(
                """
                  public record A(String test) {
                  }
                  """
              )
            );
        }

        @Test
        void classWithStaticField() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;

                  @Value
                  public class A {
                      static String disqualifyingField;
                      String test;
                  }
                  """
              )
            );
        }

        @Test
        void nonStaticInnerClass() {
            //language=java
            rewriteRun(
              java(
                """
                  package example;

                  import lombok.Value;

                  public class A {
                      @Value
                      class B {
                          String test;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void staticConstructor() {
            //language=java
            rewriteRun(
              java(
                """
                  package example;

                  import lombok.Value;

                  @Value(staticConstructor = "of")
                  public class A {
                      String test;
                  }
                  """
              )
            );
        }

        @Test
        void classImplementingConflictingInterface() {
            //language=java
            rewriteRun(
              java(
                """
                  package example;

                  import lombok.Value;

                  interface I {
                      String getTest();
                  }

                  @Value
                  public class A implements I {
                      String test;
                  }
                  """
              )
            );

        }

        @Test
        void classImplementingConflictingInterfaceWithInheritance() {
            //language=java
            rewriteRun(
              java(
                """
                  package example;

                  import lombok.Value;

                  interface I {
                      String getTest();
                  }

                  interface J extends I {
                  }

                  @Value
                  public class A implements J {
                      String test;
                  }
                  """
              )
            );

        }
    }
}
