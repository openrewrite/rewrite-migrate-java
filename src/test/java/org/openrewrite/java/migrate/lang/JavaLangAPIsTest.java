/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("deprecation")
class JavaLangAPIsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.lang")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.lang.JavaLangAPIs"));
    }

    @Test
    void characterIsJavaLetterToIsJavaIdentifierStart() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isJavaLetter('b');
                 }
              }
              """,
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isJavaIdentifierStart('b');
                 }
              }
              """
          )
        );
    }

    @Test
    void characterIsJavaLetterOrDigitToIsJavaIdentifierPart() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isJavaLetterOrDigit('b');
                 }
              }
              """,
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isJavaIdentifierPart('b');
                 }
              }
              """
          )
        );
    }

    @Test
    void characterIsSpaceToIsWhitespace() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isSpace('b');
                 }
              }
              """,
            """
              package com.abc;

              class A {
                 public void test() {
                     boolean result = Character.isWhitespace('b');
                 }
              }
              """
          )
        );
    }

    @Test
    void runtimeVersionMajorToFeature() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.major();
                  }
              }
              """,
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.feature();
                  }
              }
              """
          )
        );
    }

    @Test
    void runtimeVersionMinorToInterim() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.minor();
                  }
              }
              """,
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.interim();
                  }
              }
              """
          )
        );
    }

    @Test
    void runtimeVersionSecurityToUpdate() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.security();
                  }
              }
              """,
            """
              package com.abc;

              import java.lang.Runtime.Version;
              class A {
                  public void test() {
                      Runtime.Version runtimeVersion = Runtime.version();
                      int version = runtimeVersion.update();
                  }
              }
              """
          )
        );
    }
}
