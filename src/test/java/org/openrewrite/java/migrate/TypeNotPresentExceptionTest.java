package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

public class TypeNotPresentExceptionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HandleTypeNotPresentExceptionInsteadOfArrayStoreException())
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
            """
              import java.lang.annotation.*;
              import java.util.*;
          
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught ArrayStoreException");
                      }
                  }
              }
              """,
            """
              import java.lang.annotation.*;
              import java.util.*;
              
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (TypeNotPresentException e) {
                          System.out.println("Caught TypeNotPresentException");
                      }
                  }
              }
              """
          )
        );
    }


    @Test
    void replaceGetLocalizedOutputStream() {
        rewriteRun(
          //language=java
          java(
            """
          public class Test {
              public void testMethod() {
                  try {
                      Object o = "test";
                      o.getClass().getAnnotation(Override.class);
                  } catch (NullPointerException e) {
                      System.out.println("Caught NullPointerException");
                  }
              }
          }
          """
          )
        );
    }
}
