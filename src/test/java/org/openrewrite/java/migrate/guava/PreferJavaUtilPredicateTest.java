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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PreferJavaUtilPredicateTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.guava.PreferJavaUtilPredicate")
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void changeTypeAndMethodName() {
        rewriteRun(
          //language=java
          java(
            """   
              import com.google.common.base.Predicate;

              class A {
                  public static Predicate<String> makeStringPredicate() {
                      return new Predicate<String>() {
                          @Override
                          public boolean apply(String input) {
                              return input.isEmpty();
                          }
                      };
                  }
              }
              """,
            """ 
              import java.util.function.Predicate;

              class A {
                  public static Predicate<String> makeStringPredicate() {
                      return new Predicate<String>() {
                          @Override
                          public boolean test(String input) {
                              return input.isEmpty();
                          }
                      };
                  }
              }
              """
          )
        );
    }
}
