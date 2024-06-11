package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class JREThrowableFinalMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JREThrowableFinalMethods() );
    }

    @Test
    void renameOverRideMethods() {
        rewriteRun(
          //language=java
          java(
            """     
             package com.test;

             public class MyBadThrowableExceptionAddGet extends Throwable{
                public void add1Suppressed(Throwable exception) {  }
                public Throwable[] get1Suppressed() { return null;}
                public static void main(String args[]) {
                   MyBadThrowableExceptionAddGet t1;
                   t1.add1Suppressed(null);
                   t1.get1Suppressed();
                }
             }
             """,
            """
             package com.test;

             public class MyBadThrowableExceptionAddGet extends Throwable{
                public void myAddSuppressed(Throwable exception) {  }
                public Throwable[] myGetSuppressed() { return null;}
                public static void main(String args[]) {
                   MyBadThrowableExceptionAddGet t1;
                   t1.myAddSuppressed(null);
                   t1.myGetSuppressed();
                }
             }     
             """
          )
        );
    }
}
