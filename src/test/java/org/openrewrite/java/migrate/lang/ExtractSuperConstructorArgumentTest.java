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
class ExtractSuperConstructorArgumentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .allSources(src -> src.markers(javaVersion(25)))
          .recipe(new ExtractSuperConstructorArgument());
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
    void leaveThisDelegationAlone() {
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
              """
          )
        );
    }
}
