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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.condition.JRE.JAVA_25;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@EnabledForJreRange(min = JAVA_25)
class ExtractExplicitConstructorInvocationArgumentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .allSources(src -> src.markers(javaVersion(25)))
          .recipe(new ExtractExplicitConstructorInvocationArguments());
    }

    @DocumentExample
    @Test
    void extractMethodInvocationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(Objects.requireNonNull(name));
                  }
              }
              """,
            """
              import java.util.Objects;

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      String name1 = Objects.requireNonNull(name);
                      super(name1);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractObjectCreationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              class Holder {
                  Holder(StringBuilder sb) {
                  }
              }

              class Child extends Holder {
                  Child(String value) {
                      super(new StringBuilder(value));
                  }
              }
              """,
            """
              class Holder {
                  Holder(StringBuilder sb) {
                  }
              }

              class Child extends Holder {
                  Child(String value) {
                      StringBuilder sb = new StringBuilder(value);
                      super(sb);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractPrimitiveReturningInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      int value1 = Integer.parseInt(value);
                      super(value1);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractSimpleIdentifier() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractLiteral() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      super("constant");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeImplicitSuper() {
        rewriteRun(
          //language=java
          java(
            """
              class Child {
                  Child(String name) {
                      System.out.println(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractMultipleArgumentsInOrder() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim(), value.strip());
                  }
              }
              """,
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      String a = value.trim();
                      String b = value.strip();
                      super(a, b);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTrivialArgumentsInlineWhenExtractingSiblings() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String first, int second, String third) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(name, Integer.parseInt(name), "literal");
                  }
              }
              """,
            """
              class Parent {
                  Parent(String first, int second, String third) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      int second = Integer.parseInt(name);
                      super(name, second, "literal");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractWhenNoArgumentDoesWork() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value, value);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractGenericParameterType() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Parent {
                  Parent(List<String> values) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      super(java.util.Collections.singletonList("a"));
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotClobberLocalClassConstructorInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Base {
                  Base(String s) {
                  }
              }

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim());
                      class Local extends Base {
                          Local() {
                              super("literal");
                          }
                      }
                  }
              }
              """,
            """
              class Base {
                  Base(String s) {
                  }
              }

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      String name = value.trim();
                      super(name);
                      class Local extends Base {
                          Local() {
                              super("literal");
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractFromVarargsConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String... values) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim(), value.strip());
                  }
              }
              """
          )
        );
    }

    @Test
    void insertDeclarationsAfterExistingStatementsBeforeSuper() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      System.out.println("before");
                      super(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      System.out.println("before");
                      int value1 = Integer.parseInt(value);
                      super(value1);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractStaticFieldArgumentToPreserveOrder() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int a, int b) {
                  }
              }

              class Child extends Parent {
                  static final int CONST = 1;

                  Child(String value) {
                      super(CONST, Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int a, int b) {
                  }
              }

              class Child extends Parent {
                  static final int CONST = 1;

                  Child(String value) {
                      int a = CONST;
                      int b = Integer.parseInt(value);
                      super(a, b);
                  }
              }
              """
          )
        );
    }

    @Test
    void avoidNameCollisionWithLaterDeclaredVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(Integer.parseInt(value));
                      int value1 = 5;
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      int value2 = Integer.parseInt(value);
                      super(value2);
                      int value1 = 5;
                  }
              }
              """
          )
        );
    }

    @Test
    void extractThisDelegationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              class Child {
                  Child(int value) {
                  }

                  Child(String value) {
                      this(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Child {
                  Child(int value) {
                  }

                  Child(String value) {
                      int value1 = Integer.parseInt(value);
                      this(value1);
                  }
              }
              """
          )
        );
    }
}
