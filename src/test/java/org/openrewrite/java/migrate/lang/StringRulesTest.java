/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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

import static org.openrewrite.java.Assertions.java;

class StringRulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StringRulesRecipes());
    }

    @DocumentExample
    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void substring() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  String s1 = "hello".substring(0, "hello".length());
                  String s2 = "hello".substring(0);
              }
              """,
            """
              class Test {
                  String s1 = "hello";
                  String s2 = "hello";
              }
              """
          )
        );
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    @Test
    void indexOf() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  int i1 = "hello".indexOf("hello", 0);
                  int i2 = "hello".indexOf("hello");
                  int i3 = "hello".indexOf('h', 0);
                  int i4 = "hello".indexOf('h');
              }
              """,
            """
              class Test {
                  int i1 = "hello".indexOf("hello");
                  int i2 = "hello".indexOf("hello");
                  int i3 = "hello".indexOf('h');
                  int i4 = "hello".indexOf('h');
              }
              """
          )
        );
    }

    @SuppressWarnings({"StringOperationCanBeSimplified", "ConstantValue", "ConstantConditions"})
    @Test
    void equalsCase() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  boolean b1 = "hello".toLowerCase().equals("hi");
                  boolean b2 = "hello".toLowerCase().equals("hi".toLowerCase());
                  boolean b3 = "hello".toUpperCase().equals("hi".toUpperCase());
                  boolean b4 = "hello".toUpperCase().equals(System.getProperty("user.dir").toUpperCase());
              }
              """,
            """
              class Test {
                  boolean b1 = "hello".toLowerCase().equals("hi");
                  boolean b2 = "hello".equalsIgnoreCase("hi");
                  boolean b3 = "hello".equalsIgnoreCase("hi");
                  boolean b4 = "hello".equalsIgnoreCase(System.getProperty("user.dir"));
              }
              """
          )
        );
    }
}
