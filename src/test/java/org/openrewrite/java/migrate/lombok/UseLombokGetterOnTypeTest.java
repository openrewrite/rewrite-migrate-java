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

class UseLombokGetterOnTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokGetterOnType())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1047")
    @Test
    void hoistsDefaultGetterAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Getter;

              class Person {
                  @Getter
                  private String name;
                  @Getter
                  private int age;
              }
              """,
            """
              import lombok.Getter;

              @Getter
              class Person {
                  private String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void retainsStaticAndSyntheticFieldAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Getter;

              class Cache {
                  @Getter
                  private String value;
                  @Getter
                  private static String shared;
                  @Getter
                  private String $cachedValue;
              }
              """,
            """
              import lombok.Getter;

              @Getter
              class Cache {
                  private String value;
                  @Getter
                  private static String shared;
                  @Getter
                  private String $cachedValue;
              }
              """
          )
        );
    }

    @Test
    void doesNotHoistWhenAnEligibleFieldLacksGetter() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.Getter;

              class Person {
                  @Getter
                  private String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void doesNotHoistConfiguredGetterAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class Person {
                  @Getter(AccessLevel.PACKAGE)
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
              import lombok.Getter;

              class Cache {
                  @Getter
                  private String $cachedValue, value;
              }
              """
          )
        );
    }

    @Test
    void hoistsGetterCreatedByLombokBestPractices() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.lombok.LombokBestPractices"),
          //language=java
          java(
            """
              class Person {
                  private String name;

                  public String getName() {
                      return name;
                  }
              }
              """,
            """
              import lombok.Getter;

              @Getter
              class Person {
                  private String name;
              }
              """
          )
        );
    }
}
