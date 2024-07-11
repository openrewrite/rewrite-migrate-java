/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReferenceCloneMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReferenceCloneMethod());
    }

    @DocumentExample
    @Test
    void referenceCloneRemoval() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.ref.WeakReference;
              import java.lang.ref.SoftReference;
              import java.lang.ref.PhantomReference;

              class Foo {
                  void foo() throws Exception{
                      WeakReference<Object> ref = new WeakReference<Object>(null);
                      WeakReference<Object> ref1 = (WeakReference<Object>) ref.clone();
                      SoftReference<Object> ref3 = new SoftReference<Object>(null);
                      SoftReference<Object> ref4 = (SoftReference<Object>) ref3.clone();
                      PhantomReference<Object> ref5 = new PhantomReference<Object>(null,null);
                      PhantomReference<Object> ref6 = (PhantomReference<Object>) ref5.clone();
                  }
               }
              """,
            """
              import java.lang.ref.WeakReference;
              import java.lang.ref.SoftReference;
              import java.lang.ref.PhantomReference;

              class Foo {
                  void foo() throws Exception{
                      WeakReference<Object> ref = new WeakReference<Object>(null);
                      WeakReference<Object> ref1 = new WeakReference<Object>(ref, new ReferenceQueue<>());
                      SoftReference<Object> ref3 = new SoftReference<Object>(null);
                      SoftReference<Object> ref4 = new SoftReference<Object>(ref3, new ReferenceQueue<>());
                      PhantomReference<Object> ref5 = new PhantomReference<Object>(null,null);
                      PhantomReference<Object> ref6 = new PhantomReference<Object>(ref5, new ReferenceQueue<>());
                  }
               }
              """
          )
        );
    }

    @Test
    void noCloneRemoval() {
        rewriteRun(
          //language=java
          java(
            """
              class ClonableClass implements Cloneable {
                public ClonableClass(int id) {
                }

                @Override
                public Object clone() throws CloneNotSupportedException {
                  return super.clone();
                }
              }
              """
          )
        );
    }
}
