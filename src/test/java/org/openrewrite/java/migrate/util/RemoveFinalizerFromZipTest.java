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

                class FooBar extends Inflater {
                    public void test() {
                        FooBar obj = new FooBar();
                        obj.finalize();
                    }
                }
                 """,
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
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
    void removeCallsToSelfFinalize() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                        finalize();
                    }
                }
                 """,
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                    }
                }
                 """
            ),
            12
          )
        );
    }

    @Test
    void removeCallsToThisFinalize() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                        this.finalize();
                    }
                }
                 """,
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                    }
                }
                 """
            ),
            12
          )
        );
    }

    @Test
    void removeWhileKeepingSideEffects() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                        new FooBar().finalize();
                    }
                }
                 """,
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                        new FooBar();
                    }
                }
                 """
            ),
            12
          )
        );
    }

    @Test
    void noChangeWithFinalizeOnObject() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.zip.Inflater;

                class FooBar extends Inflater {
                    public void test() {
                        new Object().finalize();
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

                class FooBar extends Inflater {
                    public void test() {
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

                class FooBar extends Deflater {
                    public void test() {
                        FooBar obj = new FooBar();
                        obj.finalize();
                    }
                }
                 """,
              """
                import java.util.zip.Deflater;

                class FooBar extends Deflater {
                    public void test() {
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

                class FooBar extends Deflater {
                    public void test() {
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

                class FooBar extends ZipFile {
                    FooBar(){
                        super("");
                    }
                    public void test() {
                        FooBar obj = new FooBar();
                        obj.finalize();
                    }
                }
                 """,
              """
                import java.util.zip.ZipFile;

                class FooBar extends ZipFile {
                    FooBar(){
                        super("");
                    }
                    public void test() {
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

                class FooBar extends ZipFile {
                    FooBar(){
                        super("");
                    }
                   public void test() {
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
