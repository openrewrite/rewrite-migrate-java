package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class RemoveFinalizerFromZipTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveFinalizerFromZip());
    }

    @Test
    void removeFinalizerForInflater() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.Inflater;

               class FooBar extends Inflater{
                  public void test(){
                      FooBar obj = new FooBar();
                      obj.finalize();
                  }                    
               }
                """,
              """
               import java.util.zip.Inflater;

               class FooBar extends Inflater{
                  public void test(){
                      FooBar obj = new FooBar();
                  }
               }
                """
            ),
            12
          )
        );
    }

    @Test
    void noChangeWithoutFinalizerForInflater() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.Inflater;

               class FooBar extends Inflater{
                  public void test(){
                      FooBar obj = new FooBar();
                  }                    
               }
                """
            ),
            12
          )
        );
    }

    @Test
    void removeFinalizerForDeflater() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.Deflater;

               class FooBar extends Deflater{
                  public void test(){
                      FooBar obj = new FooBar();
                      obj.finalize();
                  }                    
               }
                """,
              """
               import java.util.zip.Deflater;

               class FooBar extends Deflater{
                  public void test(){
                      FooBar obj = new FooBar();
                  }
               }
                """
            ),
            12
          )
        );
    }

    @Test
    void noChangeWithoutFinalizerForDeflater() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.Deflater;

               class FooBar extends Deflater{
                  public void test(){
                      FooBar obj = new FooBar();
                  }                    
               }
                """
            ),
            12
          )
        );
    }

    @Test
    void removeFinalizerForZipFile() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.ZipFile;

               class FooBar extends ZipFile{
                  public void test(){
                      FooBar obj = new FooBar();
                      obj.finalize();
                  }                    
               }
                """,
              """
               import java.util.zip.ZipFile;

               class FooBar extends ZipFile{
                  public void test(){
                      FooBar obj = new FooBar();
                  }
               }
                """
            ),
            12
          )
        );
    }

    @Test
    void noChangeWithoutFinalizerForZipFile() {
        //language=java
        rewriteRun(
          version(
            java(
              """
               import java.util.zip.ZipFile;

               class FooBar extends ZipFile{
                  public void test(){
                      FooBar obj = new FooBar();
                  }                    
               }
                """
            ),
            12
          )
        );
    }
}
