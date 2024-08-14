package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ReplaceLocalizedStreamMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceLocalizedStreamMethods("com.test.Runtime1 getLocalizedInputStream1(java.io.InputStream)", "com.test.Runtime1 getLocalizedOutputStream1(java.io.OutputStream)"));
    }

    //language=java
    String LocalizedStreamMethodsClass = """
      
       package com.test;
       import java.io.InputStream;
       import java.io.OutputStream;

       public class Runtime1 {

        public InputStream getLocalizedInputStream1(InputStream in) {
           return in;
        }
       
       public OutputStream getLocalizedOutputStream1(OutputStream out) {
           return out;
      }
       
 }
      """;

        @Test
        @DocumentExample
        void replaceGetLocalizedInputStream() {
            rewriteRun(
              //language=java
              java(LocalizedStreamMethodsClass),
              java(
                """
                  package com.test;
                  import java.io.InputStream;
                  
                  class Test {
                      void exampleMethod(InputStream in) {
                          Runtime1 rt = null;
                          InputStream newStream = rt.getLocalizedInputStream1(in);
                      }
                  }
                  """,
                """
                  package com.test;
                  import java.io.InputStream;
                  
                  class Test {
                      void exampleMethod(InputStream in) {
                          Runtime1 rt = null;
                          InputStream newStream = in;
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
      java(LocalizedStreamMethodsClass),
      java(
        """
          package com.test;
          import java.io.OutputStream;
          
          class Test {
              void exampleMethod(OutputStream out) {
                  Runtime1 rt = null;
                  OutputStream newStream = rt.getLocalizedOutputStream1(out);
              }
          }
          """,
        """
          package com.test;
          import java.io.OutputStream;
          
          class Test {
              void exampleMethod(OutputStream out) {
                  Runtime1 rt = null;
                  OutputStream newStream = out;
              }
          }
          """
      )
    );
}
}
