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

import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import static org.openrewrite.java.Assertions.java;

class DetectAWTGetPeerMethodTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DetectAWTGetPeerMethod("com.test.Component1 getPeer()","com.test.Component1 isDisplayable()", "com.test.TestDummy","com.test.Component1 isLightweight()"));
    }
    //language=java
    String componentClass = """
      package com.test;
      public class Component1 {

             public String getPeer(){
               return "x";
             } 
             public boolean getPeer1(){
               return true;
             }   
             public boolean isDisplayable(){
               return true;
             }
            public boolean isLightweight(){
              return true;
            }
      }
     """;
    String instantOfDummyClass = """
      package com.test;
      public class TestDummy {
      }
     """;
    @DocumentExample
    @Test
    void instanceAndGetPeerMethod(){
        rewriteRun(
          //language=java
          java(componentClass),
          java(instantOfDummyClass),
          java(
            """
              package com.test;

              public class Test extends TestDummy{
                      
                    public static void main(String args[]) {
                      Test t1 = new Test();
                      Component1 c = new Component1();
                      if(c.getPeer() instanceof com.test.TestDummy){};
                      if(c.getPeer() instanceof TestDummy){};
                      Component1 y = new Component1();             
                      if (y.getPeer() != null){}         
                    }
              }
              """,
            """
              package com.test;

              public class Test extends TestDummy{
                      
                    public static void main(String args[]) {
                      Test t1 = new Test();
                      Component1 c = new Component1();
                      if(c.isLightweight()){};
                      if(c.isLightweight()){};
                      Component1 y = new Component1();             
                      if (y.isDisplayable()){}     
                    }
              }
              """
          )
        );
    }
}
