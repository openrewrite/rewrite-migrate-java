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

package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class RemoveDeprecatedRuntimeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveDeprecatedRuntime());
    }

    @Test
    void traceInstructionsRemove() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class FooBar{
                    public void test() {
                        Runtime.traceInstructions();
                    }
                }
                 """,
              """
                class FooBar{
                    public void test() {
                    }
                }
                 """
            ),
            8
          )
        );
    }


    @Test
    void traceMethodCallsRemove() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class FooBar{
                    public void test() {
                        Runtime.traceMethodCalls();
                    }
                }
                 """,
              """
                class FooBar{
                    public void test() {
                    }
                }
                 """
            ),
            8
          )
        );
    }

    @Test
    void noChange() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class FooBar{
                    public void test() {
                        Runtime.getRuntime();
                    }
                }
                 """
            ),
            8
          )
        );
    }

    @Test
    void noChanges() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                class FooBar{
                    public static void main(String[] args){
                        Runtime r = Runtime.getRuntime();
                        test(r);
                    }              
                    public void test(Runtime r) {
                        r.gc();
                    }
                }
                 """
            ),
            8
          )
        );
    }
}
