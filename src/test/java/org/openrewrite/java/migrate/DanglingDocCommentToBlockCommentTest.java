/*
 * Copyright 2025 the original author or authors.
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

class DanglingDocCommentToBlockCommentTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new DanglingDocCommentToBlockComment());
    }

    @DocumentExample
    @Test
    void documentingCommentedOutCode() {
        rewriteRun(
          java(
            """
              class A {
                  /** The field name. */
                  private String fieldName;

                  /** The request. */
                  // private transient Object request = null;

                  /** Instantiates a new A. */
                  A() {
                  }
              }
              """,
            """
              class A {
                  /** The field name. */
                  private String fieldName;

                  /* The request. */
                  // private transient Object request = null;

                  /** Instantiates a new A. */
                  A() {
                  }
              }
              """
          )
        );
    }

    @Test
    void beforeAnImport() {
        rewriteRun(
          java(
            """
              package a;

              /** Before an import. */
              import java.util.List;

              class A {
                  List<String> l;
              }
              """,
            """
              package a;

              /* Before an import. */
              import java.util.List;

              class A {
                  List<String> l;
              }
              """
          )
        );
    }

    @Test
    void afterAnAnnotation() {
        rewriteRun(
          java(
            """
              class A {
                  @Deprecated
                  /** Documents nothing. */
                  void a() {
                  }
              }
              """,
            """
              class A {
                  @Deprecated
                  /* Documents nothing. */
                  void a() {
                  }
              }
              """
          )
        );
    }

    @Test
    void multiline() {
        rewriteRun(
          java(
            """
              class A {
                  /**
                   * Documents nothing.
                   */
                  // private String gone;

                  /** Documents the method. */
                  void a() {
                  }
              }
              """,
            """
              class A {
                  /*
                   * Documents nothing.
                   */
                  // private String gone;

                  /** Documents the method. */
                  void a() {
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveAttachedDocumentationAlone() {
        rewriteRun(
          java(
            """
              /** Documents the class. */
              class A {
                  /** Documents the field. */
                  private String a;

                  /** Documents the method, through an annotation. */
                  @Deprecated
                  void a() {
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTheFileHeaderAlone() {
        rewriteRun(
          java(
            """
              /** Copyright the original author or authors. */
              package a;

              class A {
              }
              """
          )
        );
    }

    @Test
    void leaveOrdinaryBlockCommentsAlone() {
        rewriteRun(
          java(
            """
              class A {
                  /* Not documentation. */
                  // private String gone;

                  void a() {
                  }
              }
              """
          )
        );
    }
}
