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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class InliningsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Inlinings())
          .parser(JavaParser.fromJavaVersion().classpath("guava", "error_prone_annotations"));
    }

    @Test
    @DocumentExample
    void inlineMe() {
        //language=java
        rewriteRun(
          java(
            """
              package m;

              import com.google.errorprone.annotations.InlineMe;
              import java.time.Duration;

              public final class MyClass {
                  private final Duration deadline;

                  public Duration getDeadline() {
                      return deadline;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.getDeadline().toMillis()")
                  public long getDeadlineMillis() {
                      return getDeadline().toMillis();
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import m.MyClass;
              class Foo {
                  void foo(MyClass myClass) {
                      myClass.getDeadlineMillis();
                  }
              }
              """,
            """
              import m.MyClass;
              class Foo {
                  void foo(MyClass myClass) {
                      myClass.getDeadline().toMillis();
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceMethodWithImports() {
        //language=java
        rewriteRun(
          java(
            """
              package m;

              import com.google.errorprone.annotations.InlineMe;
              import java.time.Duration;

              public final class MyClass {
                  private Duration deadline;

                  public void setDeadline(Duration deadline) {
                      this.deadline = deadline;
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "this.setDeadline(Duration.ofMillis(millis))",
                      imports = {"java.time.Duration"})
                  public void setDeadline(long millis) {
                      setDeadline(Duration.ofMillis(millis));
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import m.MyClass;

              class Foo {
                  void foo(MyClass myClass) {
                      myClass.setDeadline(1000L);
                  }
              }
              """,
            """
              import m.MyClass;

              import java.time.Duration;

              class Foo {
                  void foo(MyClass myClass) {
                      myClass.setDeadline(Duration.ofMillis(1000L));
                  }
              }
              """
          )
        );
    }

    @Test
    void staticMethodReplacement() {
        //language=java
        rewriteRun(
          java(
            """
              package com.google.frobber;

              import com.google.errorprone.annotations.InlineMe;

              public final class Frobber {

                  public static Frobber fromName(String name) {
                      return new Frobber();
                  }

                  @Deprecated
                  @InlineMe(
                      replacement = "Frobber.fromName(name)",
                      imports = {"com.google.frobber.Frobber"})
                  public static Frobber create(String name) {
                      return fromName(name);
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.google.frobber.Frobber;

              class Foo {
                  void foo() {
                      Frobber f = Frobber.create("test");
                  }
              }
              """,
            """
              import com.google.frobber.Frobber;

              class Foo {
                  void foo() {
                      Frobber f = Frobber.fromName("test");
                  }
              }
              """
          )
        );
    }

    @Test
    void constructorToFactoryMethod() {
        //language=java
        rewriteRun(
          java(
            """
              package com.google.frobber;

              import com.google.errorprone.annotations.InlineMe;
              import com.google.errorprone.annotations.InlineMeValidationDisabled;

              public final class MyClass {

                  @InlineMeValidationDisabled
                  @Deprecated
                  @InlineMe(
                      replacement = "MyClass.create()",
                      imports = {"com.google.frobber.MyClass"})
                  public MyClass() {
                  }

                  public static MyClass create() {
                      return new MyClass();
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import com.google.frobber.MyClass;

              class Foo {
                  void foo() {
                      MyClass obj = new MyClass();
                  }
              }
              """,
            """
              import com.google.frobber.MyClass;

              class Foo {
                  void foo() {
                      MyClass obj = MyClass.create();
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleParameters() {
        //language=java
        rewriteRun(
          java(
            """
              package m;

              import com.google.errorprone.annotations.InlineMe;

              public final class Calculator {

                  public int addAndMultiply(int a, int b, int c) {
                      return (a + b) * c;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.addAndMultiply(x, y, z)")
                  public int compute(int x, int y, int z) {
                      return addAndMultiply(x, y, z);
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import m.Calculator;

              class Foo {
                  void foo(Calculator calc) {
                      int result = calc.compute(1, 2, 3);
                  }
              }
              """,
            """
              import m.Calculator;

              class Foo {
                  void foo(Calculator calc) {
                      int result = calc.addAndMultiply(1, 2, 3);
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedMethodCalls() {
        //language=java
        rewriteRun(
          java(
            """
              package m;

              import com.google.errorprone.annotations.InlineMe;

              public final class Builder {

                  public Builder withName(String name) {
                      return this;
                  }

                  public Builder withAge(int age) {
                      return this;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.withName(name).withAge(age)")
                  public Builder configure(String name, int age) {
                      return withName(name).withAge(age);
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import m.Builder;

              class Foo {
                  void foo(Builder builder) {
                      builder.configure("John", 30);
                  }
              }
              """,
            """
              import m.Builder;

              class Foo {
                  void foo(Builder builder) {
                      builder.withName("John").withAge(30);
                  }
              }
              """
          )
        );
    }
}
