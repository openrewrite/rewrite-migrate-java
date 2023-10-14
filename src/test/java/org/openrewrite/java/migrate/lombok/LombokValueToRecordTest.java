package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

public class LombokValueToRecordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValueToRecord())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    void convertOnlyValueAnnotatedClassWithoutDefaultValuesToRecord() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                }
                """,
              """
                public record A(
                   String test) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void classWithExplicitConstructorIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                   
                   public A() {
                       this.test = "test";
                   }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void nonJava17ClassIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void classWithMultipleLombokAnnotationsIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                import lombok.experimental.Accessors;
                
                @Value
                @Accessors(fluent = true)
                public class A {
                    String test;
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void existingRecordsAreUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                public record A(String test) {
                }
                """
            ),
            17
          )
        );
    }
}
