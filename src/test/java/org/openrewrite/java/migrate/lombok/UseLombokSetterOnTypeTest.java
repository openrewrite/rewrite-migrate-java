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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseLombokSetterOnTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokSetterOnType())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1047")
    @Test
    void hoistsDefaultSetterAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Setter;

              class Person {
                  @Setter
                  private String name;
                  @Setter
                  private int age;
              }
              """,
            """
              import lombok.Setter;

              @Setter
              class Person {
                  private String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void retainsFieldsSkippedByTypeLevelSetter() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Setter;

              class Cache {
                  @Setter
                  private String value;
                  @Setter
                  private static String shared;
                  @Setter
                  private final String id = "id";
                  @Setter
                  private String $cachedValue;
              }
              """,
            """
              import lombok.Setter;

              @Setter
              class Cache {
                  private String value;
                  @Setter
                  private static String shared;
                  @Setter
                  private final String id = "id";
                  @Setter
                  private String $cachedValue;
              }
              """
          )
        );
    }

    @Test
    void doesNotHoistWhenAMutableFieldLacksSetter() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Setter;

              class Person {
                  @Setter
                  private String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void doesNotHoistConfiguredSetterAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class Person {
                  @Setter(AccessLevel.PACKAGE)
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void doesNotHoistMixedVariableDeclarations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Setter;

              class Cache {
                  @Setter
                  private String $cachedValue, value;
              }
              """
          )
        );
    }
}
