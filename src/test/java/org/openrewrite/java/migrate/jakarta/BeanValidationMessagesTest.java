package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
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
                    String message() default "{javax.validation.constraints.NotNull.message}";
                }
                """
            )
          );
    }

    @Test
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