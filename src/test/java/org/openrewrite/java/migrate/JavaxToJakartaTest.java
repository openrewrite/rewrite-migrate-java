/*
 * Copyright 2021 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaxToJakartaTest implements RewriteTest {
    @Language("java")
    private static final String javax =
      """
            package javax.xml.bind.annotation;
            public class A {
                public static void stat() {}
                public void foo() {}
            }
        """;

    @Language("java")
    private static final String jakarta =
      """
              package jakarta.xml.bind.annotation;
              public class A {
                  public static void stat() {}
                  public void foo() {}
              }
        """;

    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.JavaxMigrationToJakarta")
        );
    }

    @Test
    void doNotAddImportWhenNoChangesWereMade() {
        rewriteRun(
          java("public class B {}")
        );
    }

    @Test
    void changeImport() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  import javax.xml.bind.annotation.A;
                  public class B {
                  }
              """,
            """
                  import jakarta.xml.bind.annotation.A;
                  public class B {
                  }
              """
          )
        );
    }

    @Test
    void fullyQualifiedName() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            "public class B extends javax.xml.bind.annotation.A {}",
            "public class B extends jakarta.xml.bind.annotation.A {}"
          )
        );
    }

    @Test
    void annotation() {
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion().dependsOn(
                """
                      package javax.xml.bind.annotation;
                      public @interface A {}
                  """,
                """
                      package jakarta.xml.bind.annotation;
                      public @interface A {}
                  """
              )
            )
          ,
          java(
            "@javax.xml.bind.annotation.A public class B {}",
            "@jakarta.xml.bind.annotation.A public class B {}"
          )
        );
    }

    // array types and new arrays
    @Test
    void array() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  public class B {
                     javax.xml.bind.annotation.A[] a = new javax.xml.bind.annotation.A[0];
                  }
              """,
            """
                  public class B {
                     jakarta.xml.bind.annotation.A[] a = new jakarta.xml.bind.annotation.A[0];
                  }
              """
          )
        );
    }

    @Test
    void classDecl() {
        rewriteRun(
          spec -> {
              spec.recipe(Environment.builder()
                .scanRuntimeClasspath("org.openrewrite.java.migrate")
                .build()
                .activateRecipes("org.openrewrite.java.migrate.JavaxMigrationToJakarta")
                .doNext(new ChangeType("I1", "I2", false))
              );
              spec.parser(JavaParser.fromJavaVersion()
                .dependsOn(
                  javax,
                  jakarta,
                  "public interface I1 {}",
                  "public interface I2 {}"
                )
              );
          },
          java(
            "public class B extends javax.xml.bind.annotation.A implements I1 {}",
            "public class B extends jakarta.xml.bind.annotation.A implements I2 {}"
          ));
    }

    @Test
    void method() {
        rewriteRun(
          spec ->
            spec
              .recipe(Environment.builder()
                .scanRuntimeClasspath("org.openrewrite.java.migrate")
                .build()
                .activateRecipes("org.openrewrite.java.migrate.JavaxMigrationToJakarta")
                .doNext(new ChangeType("I1", "I2", false))
              )
              .parser(
                JavaParser.fromJavaVersion().dependsOn(
                  javax,
                  jakarta,
                  "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
                  "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"
                )
              )
          ,
          java(
            """
                     public class B {
                        public javax.xml.bind.annotation.A foo() throws javax.xml.bind.annotation.NewException { return null; }
                     }
              """,
            """
                  public class B {
                     public jakarta.xml.bind.annotation.A foo() throws jakarta.xml.bind.annotation.NewException { return null; }
                  }
              """
          )
        );
    }

    @Test
    void methodInvocationTypeParametersAndWildcard() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                     import java.util.List;
                     public class B {
                        public <T extends javax.xml.bind.annotation.A> T generic(T n, List<? super javax.xml.bind.annotation.A> in) {
                             return n;
                        }
                        public void test() {
                            javax.xml.bind.annotation.A.stat();
                            this.<javax.xml.bind.annotation.A>generic(null, null);
                        }
                     }
              """,
            """
                     import java.util.List;
                     public class B {
                        public <T extends jakarta.xml.bind.annotation.A> T generic(T n, List<? super jakarta.xml.bind.annotation.A> in) {
                             return n;
                        }
                        public void test() {
                            jakarta.xml.bind.annotation.A.stat();
                            this.<jakarta.xml.bind.annotation.A>generic(null, null);
                        }
                     }
              """
          )
        );
    }

    @Test
    void multiCatch() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
              "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"
            )
          ),
          java(
            """
                     public class B {
                        public void test() {
                            try {}
                            catch(javax.xml.bind.annotation.NewException | RuntimeException e) {}
                        }
                     }
              """,
            """
                     public class B {
                        public void test() {
                            try {}
                            catch(jakarta.xml.bind.annotation.NewException | RuntimeException e) {}
                        }
                     }
              """
          )
        );
    }

    @Test
    void multiVariable() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  public class B {
                     javax.xml.bind.annotation.A f1, f2;
                  }
              """,
            """
                  public class B {
                     jakarta.xml.bind.annotation.A f1, f2;
                  }
              """
          )
        );
    }

    @Test
    void newClass() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  public class B {
                     javax.xml.bind.annotation.A a = new javax.xml.bind.annotation.A();
                  }
              """,
            """
                  public class B {
                     jakarta.xml.bind.annotation.A a = new jakarta.xml.bind.annotation.A();
                  }
              """
          )
        );
    }

    @Test
    void parameterizedType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  import java.util.Map;
                  public class B {
                     Map<javax.xml.bind.annotation.A, javax.xml.bind.annotation.A> m;
                  }
              """,
            """
                  import java.util.Map;
                  public class B {
                     Map<jakarta.xml.bind.annotation.A, jakarta.xml.bind.annotation.A> m;
                  }
              """
          )
        );
    }

    @Test
    void typeCast() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  public class B {
                     javax.xml.bind.annotation.A a = (javax.xml.bind.annotation.A) null;
                  }
              """,
            """
                  public class B {
                     jakarta.xml.bind.annotation.A a = (jakarta.xml.bind.annotation.A) null;
                  }
              """
          )
        );
    }

    @Test
    void classReference() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                  public class A {
                      Class<?> clazz = javax.xml.bind.annotation.A.class;
                  }
              """,
            """
                  public class A {
                      Class<?> clazz = jakarta.xml.bind.annotation.A.class;
                  }
              """
          )
        );
    }

    @Test
    void methodSelect() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                     public class B {
                        javax.xml.bind.annotation.A a = null;
                        public void test() { a.foo(); }
                     }
              """,
            """
                     public class B {
                        jakarta.xml.bind.annotation.A a = null;
                        public void test() { a.foo(); }
                     }
              """
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
                     import static javax.xml.bind.annotation.A.stat;
                     public class B {
                         public void test() {
                             stat();
                         }
                     }
              """,
            """
                     import static jakarta.xml.bind.annotation.A.stat;
                     public class B {
                         public void test() {
                             stat();
                         }
                     }
              """
          )
        );
    }
}