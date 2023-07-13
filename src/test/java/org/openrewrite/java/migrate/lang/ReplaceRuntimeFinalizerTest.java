/*
 * Copyright 2021 the original author or authors.
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

import static org.openrewrite.java.Assertions.*;

class ReplaceRuntimeFinalizerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceRuntimeFinalizer());//.allSources(s -> s.markers(javaVersion(8)));
    }

    @Test
    void replaceRuntimeRunFinalizerOnExit() {
        //language=java
        rewriteRun(
            java(
              """
                 class FooBar {
                      public void test(){
                          Runtime.runFinalizersOnExit(true);
                      }
                  }
                """,
              """
                 class FooBar {
                      public void test(){
                          Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().exit(0)));
                      }
                  }
                """
            ));
    }

    @Test
    void noChange() {
        //language=java
        rewriteRun(
            java(
              """
                 class FooBar {
                      public void test(){
                          Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().exit(0)));
                      }
                  }
                """
            ));
    }

    @Test
    void replaceSystemRunFinalizerOnExit() {
        //language=java
        rewriteRun(
            java(
              """
                 class FooBar {
                      public void test(){
                          System.runFinalizersOnExit(true);
                      }
                  }
                """,
              """
                 class FooBar {
                      public void test(){
                          Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().exit(0)));
                      }
                  }
                """
            ));
    }

}
