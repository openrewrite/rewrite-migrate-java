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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class AddMissingMethodImplementationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            new AddMissingMethodImplementation("I1", "*..* m1()",
              "public void m1() { System.out.println(\"m1\"); }"))
          .allSources(src -> src.markers(javaVersion(21)));
    }

    @DocumentExample
    @Test
    void happyPath() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              class C2 implements I1 {}
              """,
            """
              interface I1 {}
              class C2 implements I1 {
                  public void m1() {
                      System.out.println("m1");
                  }}
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/459")
    @Test
    void skipInterfaces() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              interface I2 extends I1 {}
              """
          )
        );
    }

    @Test
    void skipAbstractClasses() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              abstract class AC2 implements I1 {}
              """
          )
        );
    }

    @Test
    void methodExists() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              class C2 implements I1 {
                  public void m1() {
                      System.out.println("m1");
                  }
              }
              """
          )
        );
    }

    @Test
    void methodExistsDiffImpl() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              class C2 implements I1 {
                  public void m1() {
                      System.out.println("m1 diff");
                  }
              }
              """
          )
        );
    }

    @Test
    void methodExistsDiffSignature() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              class C2 implements I1 {
                  protected void m1() {
                      System.out.println("m1");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1862")
    @Test
    void skipWhenSuperclassAlreadyHasMethod() {
        //language=java
        rewriteRun(
          java(
            """
              interface I1 {}
              class SuperClass implements I1 {
                  public void m1() {
                      System.out.println("m1 from super");
                  }
              }
              class SubClass extends SuperClass {}
              """
          )
        );
    }

}
