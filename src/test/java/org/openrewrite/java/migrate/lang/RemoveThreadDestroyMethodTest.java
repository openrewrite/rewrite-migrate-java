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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class RemoveThreadDestroyMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveThreadDestroyMethod());//.allSources(s -> s.markers(javaVersion(8)));
    }

    @DocumentExample
    @Test
    void removeDestroyCall() {
        //language=java
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.builder().methodInvocations(false).build()),
          java(
            """
              class FooBar{
                  public void test() {
                      Thread thread = Thread.currentThread();
                      thread.setName("Main Thread");
                      thread.destroy();
                  }
              }
              """,
            """
              class FooBar{
                  public void test() {
                      Thread thread = Thread.currentThread();
                      thread.setName("Main Thread");
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithoutDestroy() {
        //language=java
        rewriteRun(
          java(
            """
              class FooBar{
                  public void test() {
                      Thread thread = Thread.currentThread();
                      thread.setName("Main Thread");
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithoutThreadDestroy() {
        //language=java
        rewriteRun(
          java(
            """
              class FooBar{
                  public void test() {
                      FooBar f = new FooBar();
                      f.destroy();
                  }
                  public void destroy(){
                  }
              }
              """
          )
        );
    }
}
