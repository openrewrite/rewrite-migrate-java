/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class BeanValidationMessagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new BeanValidationMessages())
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
              """
                package javax.validation.constraints;
                                
                import java.lang.annotation.*;
                import static java.lang.annotation.ElementType.*;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;
                                
                @Target(value={METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER})
                @Retention(value=RUNTIME)
                public @interface NotNull {
                    String message() default "{javax.validation.constraints.NotNull.message}";
                }
                """,
              """
                package jakarta.validation.constraints;
                                
                import java.lang.annotation.*;
                import static java.lang.annotation.ElementType.*;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;
                                
                @Target(value={METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER,TYPE_USE})
                @Retention(value=RUNTIME)
                public @interface NotNull {
                    String message() default "{jakarta.validation.constraints.NotNull.message}";
                }
                """
            )
          );
    }

    @Test
    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/pull/374")
    void replaceMessage() {
        rewriteRun(
          //language=java
          java(
            """
                import javax.validation.constraints.*;
                
                class Test {
                   @NotNull(message = "Resource Code {javax.validation.constraints.NotNull.message}")
                   private String resourceCode;
                }
              """,
            """
                import javax.validation.constraints.*;
                
                class Test {
                   @NotNull(message = "Resource Code {jakarta.validation.constraints.NotNull.message}")
                   private String resourceCode;
                }
              """
          )
        );
    }
}