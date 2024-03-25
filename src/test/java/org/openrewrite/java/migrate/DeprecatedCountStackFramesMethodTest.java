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

class DeprecatedCountStackFramesMethodTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DeprecatedCountStackFramesMethod());
    }

    @Test
    @DocumentExample
    void deprecatedCountStackFrame() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.Thread;
               
                 public class Test {
               	        public static void main(String args[]) {
               		        Thread t1 = new Thread();
               		        Thread t2 = new Thread();	
               		        int x = t1.countStackFrames();
               		        int y = t2.countStackFrames();
               	  }              
              }           
              """,
            """
              import java.lang.Thread;
               
                 public class Test {
               	        public static void main(String args[]) {
               		        Thread t1 = new Thread();
               		        Thread t2 = new Thread();	
               		        int x = Integer.valueOf("0");
               		        int y = Integer.valueOf("0");
               	  }              
              }    
              """
          )
        );
    }

}
