/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NegligentlyConvertEqualsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NegligentlyConvertEquals())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true).classpath("lombok"));
    }

    @DocumentExample
    @Test
    void replaceEquals() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo;

                  @Override
                  public boolean equals(Object o) {
                      return false;
                  }
              }
              """,
            """
              import lombok.EqualsAndHashCode;

              @EqualsAndHashCode
              class A {

                  int foo;
              }
              """
          )
        );
    }

    @Test
    void noCostomMethodsNoAnnotation() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo;

              }
              """
          )
        );
    }

    @Test
    void replaceEqualsInPackage() {
        rewriteRun(// language=java
          java(
            """
              package com.example;

              class A {

                  int foo;

                  @Override
                  public boolean equals(Object o) {
                      return false;
                  }
              }
              """,
            """
              package com.example;

              import lombok.EqualsAndHashCode;

              @EqualsAndHashCode
              class A {

                  int foo;
              }
              """
          )
        );
    }

    @Test
    void replaceHashCode() {
        rewriteRun(// language=java
          java(
            """
              package com.example;

              class A {

                  int foo;

                  @Override
                  public int hashCode() {
                      return 6;
                  }
              }
              """,
            """
              package com.example;

              import lombok.EqualsAndHashCode;

              @EqualsAndHashCode
              class A {

                  int foo;
              }
              """
          )
        );
    }

    @Test
    void replaceEqualsAndHashCode() {
        rewriteRun(// language=java
          java(
            """
              package com.example;

              class A {

                  int foo;

                  @Override
                  public boolean equals(Object o) {
                      return false;
                  }

                  @Override
                  public int hashCode() {
                      return 6;
                  }

              }
              """,
            """
              package com.example;

              import lombok.EqualsAndHashCode;

              @EqualsAndHashCode
              class A {

                  int foo;

              }
              """
          )
        );
    }

}
