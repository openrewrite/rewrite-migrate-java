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
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class RemovedToolProviderConstructorTest  implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
            spec.expectedCyclesThatMakeChanges(2).recipe(new RemovedToolProviderConstructor());
   }
    @Test
    void moveToStaticTest() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
               
              import javax.tools.ToolProvider;
               
              public class RemovedToolProviderConstructorApp {
               
                   public void test() throws Exception {
                       ToolProvider tp = null;
                       tp.getSystemJavaCompiler();     
                       tp.getSystemDocumentationTool();
                       tp.getSystemToolClassLoader();  
                       System.out.println(ToolProvider.getSystemJavaCompiler());      
                   }
              }          
              """,
            """
              package com.test;
               
              import javax.tools.ToolProvider;
               
              public class RemovedToolProviderConstructorApp {
               
                   public void test() throws Exception {
                       ToolProvider tp = null;
                       ToolProvider.getSystemJavaCompiler();     
                       ToolProvider.getSystemDocumentationTool();
                       ToolProvider.getSystemToolClassLoader();  
                       System.out.println(ToolProvider.getSystemJavaCompiler());              
                   }
              }          
              """
          )
        );
    }


}
