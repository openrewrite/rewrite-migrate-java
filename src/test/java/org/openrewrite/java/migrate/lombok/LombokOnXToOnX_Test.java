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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class LombokOnXToOnX_Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokOnXToOnX_())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"))
          .typeValidationOptions(TypeValidation.all().identifiers(false));
    }

    @DocumentExample
    @Test
    void migrateGetterOnMethod() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Getter;
              class Example {
                  @Getter(onMethod=@__({@Deprecated}))
                  private String field;
              }
              """,
            """
              import lombok.Getter;
              class Example {
                  @Getter(onMethod_={@Deprecated})
                  private String field;
              }
              """
          )
        );
    }

    @Test
    void migrateSetterOnParam() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Setter;
              class Example {
                  @Setter(onParam=@__({@SuppressWarnings("unchecked")}))
                  private long unid;
              }
              """,
            """
              import lombok.Setter;
              class Example {
                  @Setter(onParam_={@SuppressWarnings("unchecked")})
                  private long unid;
              }
              """
          )
        );
    }

    @Test
    void migrateMultipleAnnotations() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Getter;
              import lombok.Setter;
              class Example {
                  @Getter(onMethod=@__({@Deprecated, @SuppressWarnings("all")}))
                  @Setter(onParam=@__({@SuppressWarnings("unchecked")}))
                  private long unid;
              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              class Example {
                  @Getter(onMethod_={@Deprecated, @SuppressWarnings("all")})
                  @Setter(onParam_={@SuppressWarnings("unchecked")})
                  private long unid;
              }
              """
          )
        );
    }

    @Test
    void migrateConstructorAnnotations() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.RequiredArgsConstructor;
              @RequiredArgsConstructor(onConstructor=@__({@Deprecated}))
              class Example {
                  private final String field;
              }
              """,
            """
              import lombok.RequiredArgsConstructor;
              @RequiredArgsConstructor(onConstructor_={@Deprecated})
              class Example {
                  private final String field;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeIfAlreadyMigrated() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Getter;
              class Example {
                  @Getter(onMethod_={@Deprecated})
                  private String field;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeIfNoOnXParameter() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Getter;
              class Example {
                  @Getter(lazy=true)
                  private final String field = "value";
              }
              """
          )
        );
    }

    @Test
    void handleEmptyAnnotationList() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.Getter;
              class Example {
                  @Getter(onMethod=@__({}))
                  private String field;
              }
              """,
            """
              import lombok.Getter;
              class Example {
                  @Getter(onMethod_={})
                  private String field;
              }
              """
          )
        );
    }

    @ExpectedToFail("Parser bug with @__ syntax causes test failure")
    @Test
    void handleWithAnnotation() {
        //language=java
        rewriteRun(
          java(
            """
              import lombok.With;
              class Example {
                  @With(onParam=@__({@SuppressWarnings("unused")}))
                  private final String field;

                  Example(String field) {
                      this.field = field;
                  }
              }
              """,
            """
              import lombok.With;
              class Example {
                  @With(onParam_={@SuppressWarnings("unused")})
                  private final String field;

                  Example(String field) {
                      this.field = field;
                  }
              }
              """
          )
        );
    }
}
