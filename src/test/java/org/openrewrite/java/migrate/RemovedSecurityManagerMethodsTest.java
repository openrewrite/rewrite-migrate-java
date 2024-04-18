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
import org.openrewrite.java.migrate.jakarta.SomeRecipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class RemovedSecurityManagerMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemovedSecurityManagerMethods());
    }
    @DocumentExample
    @Test
    void testMethodChangeTocheckPermission() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              
              import java.lang.SecurityManager;
              
              public class testMethods {
                                
                public void testFourMethods() {
                    SecurityManager sm = new SecurityManager();
                    sm.checkAwtEventQueueAccess();      \s
                }
              }
              """,
            """
              package com.test;
              
              import java.lang.SecurityManager;
              
              public class testMethods {
                                
                public void testFourMethods() {
                    SecurityManager sm = new SecurityManager();
                    sm.checkAwtEventQueueAccess();      \s
                }
              }
              """
          )
        );
    }

}
