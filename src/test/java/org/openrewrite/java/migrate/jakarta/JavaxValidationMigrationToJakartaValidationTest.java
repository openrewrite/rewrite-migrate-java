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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaxValidationMigrationToJakartaValidationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxValidationMigrationToJakartaValidation")
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1526")
    @Test
    void sunIstackNotNullToJakartaValidation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            //language=java
            """
              package com.sun.istack;
              import java.lang.annotation.*;
              @Documented
              @Retention(RetentionPolicy.CLASS)
              @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
              public @interface NotNull {
              }
              """,
            //language=java
            """
              package jakarta.validation.constraints;
              import java.lang.annotation.*;
              @Documented
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
              public @interface NotNull {
                  String message() default "{jakarta.validation.constraints.NotNull.message}";
                  Class<?>[] groups() default {};
                  Class<?>[] payload() default {};
              }
              """
          )),
          //language=java
          java(
            """
              import com.sun.istack.NotNull;

              public class Example {
                  @NotNull
                  private String name;

                  public void setName(@NotNull String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import jakarta.validation.constraints.NotNull;

              public class Example {
                  @NotNull
                  private String name;

                  public void setName(@NotNull String name) {
                      this.name = name;
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void javaxValidationToJakartaValidation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            //language=java
            """
              package javax.validation.constraints;
              import java.lang.annotation.*;
              @Documented
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
              public @interface NotNull {
                  String message() default "{javax.validation.constraints.NotNull.message}";
                  Class<?>[] groups() default {};
                  Class<?>[] payload() default {};
              }
              """,
            //language=java
            """
              package jakarta.validation.constraints;
              import java.lang.annotation.*;
              @Documented
              @Retention(RetentionPolicy.RUNTIME)
              @Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.PARAMETER, ElementType.TYPE_USE})
              public @interface NotNull {
                  String message() default "{jakarta.validation.constraints.NotNull.message}";
                  Class<?>[] groups() default {};
                  Class<?>[] payload() default {};
              }
              """
          )),
          //language=java
          java(
            """
              import javax.validation.constraints.NotNull;

              public class Example {
                  @NotNull
                  private String name;
              }
              """,
            """
              import jakarta.validation.constraints.NotNull;

              public class Example {
                  @NotNull
                  private String name;
              }
              """
          )
        );
    }
}
