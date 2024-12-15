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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class JREThrowableFinalMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
    }

    @DocumentExample
    @Test
    void renameMethodDeclarationsAndUsagesElsewhere() {
        rewriteRun(
          // Override method patterns as test examples would not compile otherwise
          spec -> spec.recipe(new JREThrowableFinalMethods(
            "*..* add1Suppressed(Throwable)",
            "*..* get1Suppressed()")),
          //language=java
          java(
            """
              package com.test;
              public class ThrowableWithIllegalOverrrides extends Throwable {
                  public void add1Suppressed(Throwable exception) {
                  }

                  public Throwable[] get1Suppressed() {
                      return null;
                  }
              }
              """,
            """
              package com.test;
              public class ThrowableWithIllegalOverrrides extends Throwable {
                  public void myAddSuppressed(Throwable exception) {
                  }

                  public Throwable[] myGetSuppressed() {
                      return null;
                  }
              }
              """,
            spec -> spec.markers(javaVersion(6))
          ),
          //language=java
          java(
            """
              import com.test.ThrowableWithIllegalOverrrides;
              class ClassUsingException {
                  void methodUsingException(ThrowableWithIllegalOverrrides t1) {
                      t1.add1Suppressed(null);
                      t1.get1Suppressed();
                  }
              }
              """,
            """
              import com.test.ThrowableWithIllegalOverrrides;
              class ClassUsingException {
                  void methodUsingException(ThrowableWithIllegalOverrrides t1) {
                      t1.myAddSuppressed(null);
                      t1.myGetSuppressed();
                  }
              }
              """,
            spec -> spec.markers(javaVersion(6))
          )
        );
    }

    @Test
    void shouldRenameOnJava6() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new JREThrowableFinalMethods()),
          java(
            """
              class ClassUsingThrowable {
                  void methodUsingRenamedMethodsAlready(Throwable t1) {
                      t1.addSuppressed(new Throwable());
                      t1.getSuppressed();
                  }
              }
              """,
            """
              class ClassUsingThrowable {
                  void methodUsingRenamedMethodsAlready(Throwable t1) {
                      t1.myAddSuppressed(new Throwable());
                      t1.myGetSuppressed();
                  }
              }
              """,
            spec -> spec.markers(javaVersion(6))
          )
        );
    }

    @Test
    void shouldNotRenameOnJava7() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new JREThrowableFinalMethods()),
          java(
            """
              class ClassUsingThrowable {
                  void methodUsingRenamedMethodsAlready(Throwable t1) {
                      t1.addSuppressed(new Throwable());
                      t1.getSuppressed();
                  }
              }
              """,
            spec -> spec.markers(javaVersion(7))
          )
        );
    }
}
