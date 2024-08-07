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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceAWTGetPeerMethodTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceAWTGetPeerMethod(
            "com.test.Component1 getPeer()",
            "com.test.Component1 isDisplayable()",
            "com.test.TestDummy",
            "com.test.Component1 isLightweight()"))
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
              """
                package com.test;
                public class Component1 {
                    public String getPeer() {
                        return "x";
                    }
                    public boolean getPeer1() {
                        return true;
                    }
                    public boolean isDisplayable() {
                        return true;
                    }
                    public boolean isLightweight() {
                        return true;
                    }
                }
                """,
              """
                 package com.test;
                 public class TestDummy {
                 }
                """
            ));
    }

    @Test
    @DocumentExample
    void instanceAndGetPeerMethodControlParentheses() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              class Test extends TestDummy {
                  void foo() {
                      Test t1 = new Test();
                      Component1 c = new Component1();
                      if (c.getPeer() instanceof com.test.TestDummy) {
                      }
                      if (c.getPeer() instanceof TestDummy) {
                      }
                      Component1 y = new Component1();
                      if (y.getPeer() != null) {
                      }
                  }
              }
              """,
            """
              package com.test;
              class Test extends TestDummy {
                  void foo() {
                      Test t1 = new Test();
                      Component1 c = new Component1();
                      if (c.isLightweight()) {
                      }
                      if (c.isLightweight()) {
                      }
                      Component1 y = new Component1();
                      if (y.isDisplayable()) {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceAndGetPeerMethodNoParenthesis() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;

              class Test {
                  void foo() {
                      Component1 y = new Component1();
                      boolean instance = y.getPeer() instanceof TestDummy;
                      if (instance){
                      }
                      boolean instance1 = y.getPeer() != null;
                      if (instance1){
                      }
                  }
              }
              """,
            """
              package com.test;

              class Test {
                  void foo() {
                      Component1 y = new Component1();
                      boolean instance = y.isLightweight();
                      if (instance){
                      }
                      boolean instance1 = y.isDisplayable();
                      if (instance1){
                      }
                  }
              }
              """
          )
        );
    }
}
