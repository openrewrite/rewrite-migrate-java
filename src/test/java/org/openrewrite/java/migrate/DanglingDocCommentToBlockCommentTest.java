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

import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

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
    void banners() {
        rewriteRun(
          java(
            """
              class A {
                  /******** section ********/
                  // private String gone;

                  /***/
                  // private String empty;

                  /********
                   * Documents nothing.
                   ********/
                  // private String alsoGone;

                  /** Documents the method. */
                  void a() {
                  }
              }
              """,
            """
              class A {
                  /* section ********/
                  // private String gone;

                  /**/
                  // private String empty;

                  /*
                   * Documents nothing.
                   ********/
                  // private String alsoGone;

                  /** Documents the method. */
                  void a() {
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveDocumentationOfMultipleVariablesAlone() {
        rewriteRun(
          java(
            """
              class A {
                  /** Documents both fields. */
                  public int a, b;
              }
              """
          )
        );
    }

    @Test
    void leaveDocumentationAfterStraySemicolonAlone() {
        rewriteRun(
          java(
            """
              interface A {
                  /** Documents first. */
                  default void first() { };

                  /** Documents second. */
                  default void second() { };

                  /** Documents third. */
                  void third();
              }
              """
          )
        );
    }

    @Test
    void leaveDocumentationAfterNestedTypeSemicolonAlone() {
        rewriteRun(
          java(
            """
              class A {
                  private enum E {P, Q};

                  /** Documents the field. */
                  private final String s = "";
              }
              """
          )
        );
    }

    @Test
    void leaveDocumentationOfFirstFieldAlone() {
        rewriteRun(
          java(
            """
              public class A extends RuntimeException {

                  /**
                   * The version identifier for this Serializable class. Increment only if the
                   * <i>serialized</i> form of the class changes.
                   */
                  private static final long serialVersionUID = 1L;

                  /**
                   * Default URL to download the maven-wrapper.jar from, if no 'downloadUrl' is provided.
                   */
                  private static final String DEFAULT_DOWNLOAD_URL =
                    "https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.4.2/maven-wrapper-0.4.2.jar";
              }
              """
          )
        );
    }

    @Test
    void leaveDocumentationBeforeAnnotatedClassAfterImportAlone() {
        rewriteRun(
          java(
            """
              import java.util.List;

              /** Documents the class. */
              @Deprecated
              class A {
                  List<String> l;
              }
              """
          )
        );
    }

    @Test
    void onlyTheLastDocumentationCommentIsAttached() {
        rewriteRun(
          java(
            """
              class A {
                  /** Documents nothing. */
                  /** Documents both fields. */
                  public int a, b;
              }
              """,
            """
              class A {
                  /* Documents nothing. */
                  /** Documents both fields. */
                  public int a, b;
              }
              """
          )
        );
    }

    @Test
    void localVariableDocumentationStillDocumentsNothing() {
        rewriteRun(
          java(
            """
              class A {
                  void a() {
                      /** Documents nothing. */
                      int x = 1, y = 2;
                  }
              }
              """,
            """
              class A {
                  void a() {
                      /* Documents nothing. */
                      int x = 1, y = 2;
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

    @Test
    void leaveKotlinAlone() {
        rewriteRun(
          kotlin(
            """
              import java.util.UUID

              /*******************************************************************
               * KDoc is parsed as a plain comment, so it would otherwise look dangling.
               */
              val id = UUID.randomUUID()
              """
          )
        );
    }

    @Test
    void leaveGroovyAlone() {
        rewriteRun(
          groovy(
            """
              import java.util.UUID

              /*******************************************************************
               * Groovydoc is parsed as a plain comment, so it would otherwise look dangling.
               */
              def id = UUID.randomUUID()
              """
          )
        );
    }
}
