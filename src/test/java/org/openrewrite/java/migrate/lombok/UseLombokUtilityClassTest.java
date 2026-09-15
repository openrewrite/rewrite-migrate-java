/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class UseLombokUtilityClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokUtilityClass())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/512")
    @Test
    void utilityClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Numbers {
                  private static final int OFFSET = 1;
                  private static int calls;

                  static int add(int left, int right) {
                      calls++;
                      return left + right + OFFSET;
                  }
              }
              """,
            """
              import lombok.experimental.UtilityClass;

              @UtilityClass
              class Numbers {
                  private final int OFFSET = 1;
                  private int calls;

                  int add(int left, int right) {
                      calls++;
                      return left + right + OFFSET;
                  }
              }
              """
          )
        );
    }

    @Test
    void usesQualifiedAnnotationWhenUtilityClassTypeExists() {
        rewriteRun(
          //language=java
          java(
            """
              class UtilityClass {
              }

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            """
              class UtilityClass {
              }

              @lombok.experimental.UtilityClass
              class Numbers {
                  int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          )
        );
    }

    @Test
    void usesQualifiedAnnotationWhenNestedUtilityClassTypeExists() {
        rewriteRun(
          //language=java
          java(
            """
              class Outer {
                  class UtilityClass {
                  }

                  class Numbers {
                      static int add(int left, int right) {
                          return left + right;
                      }
                  }
              }
              """,
            """
              class Outer {
                  class UtilityClass {
                  }

                  @lombok.experimental.UtilityClass
                  class Numbers {
                      int add(int left, int right) {
                          return left + right;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertInstantiatedClass() {
        rewriteRun(
          //language=java
          java(
            """
              package example;

              public class Numbers {
                  public static int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              package example;

              class UsesNumbers {
                  private final Numbers numbers = new Numbers();
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertConstructorMethodReference() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.Supplier;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }

              class UsesNumbers {
                  private final Supplier<Numbers> factory = Numbers::new;
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertInheritedClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }

              class ExtendedNumbers extends Numbers {
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertNonStarStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              package example;

              public class Numbers {
                  public static int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              package other;

              import static example.Numbers.add;

              class UsesNumbers {
                  int addOne(int value) {
                      return add(value, 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertNestedNonStarStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              package example;

              public class Outer {
                  public static class Numbers {
                      public static int add(int left, int right) {
                          return left + right;
                      }
                  }
              }
              """
          ),
          //language=java
          java(
            """
              package other;

              import static example.Outer.Numbers.add;

              class UsesNumbers {
                  int addOne(int value) {
                      return add(value, 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertClassesWithInstanceMembers() {
        rewriteRun(
          //language=java
          java(
            """
              class Numbers {
                  private int offset;

                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class MoreNumbers {
                  int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertClassesWithConstructorsOrMainMethods() {
        rewriteRun(
          //language=java
          java(
            """
              class Numbers {
                  private Numbers() {
                  }

                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Application {
                  public static void main(String[] args) {
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertNestedClassWithinNonStaticMemberClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Outer {
                  class Inner {
                      class Utilities {
                          static final int VALUE = 1;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotConvertWhenLombokConfigForbidsUtilityClass() {
        rewriteRun(
          text(
            """
              LOMBOK.UTILITYCLASS.FLAGUSAGE = ERROR
              """,
            spec -> spec.path("lombok.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void honorsCloserLombokConfig() {
        rewriteRun(
          text(
            """
              lombok.utilityClass.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.utilityClass.flagUsage = warning
              """,
            spec -> spec.path("src/main/lombok.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            """
              package example;

              import lombok.experimental.UtilityClass;

              @UtilityClass
              class Numbers {
                  int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void honorsClearLombokConfig() {
        rewriteRun(
          text(
            """
              lombok.utilityClass.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              clear lombok.utilityClass.flagUsage
              """,
            spec -> spec.path("src/main/lombok.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            """
              package example;

              import lombok.experimental.UtilityClass;

              @UtilityClass
              class Numbers {
                  int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void honorsImportedLombokConfig() {
        rewriteRun(
          text(
            """
              import utility.config
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.utilityClass.flagUsage = error
              """,
            spec -> spec.path("utility.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void skipsUnresolvedImportedLombokConfig() {
        rewriteRun(
          text(
            """
              import missing.config
              """,
            spec -> spec.path("lombok.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void honorsStopBubblingLombokConfig() {
        rewriteRun(
          text(
            """
              lombok.utilityClass.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("src/main/lombok.config")
          ),
          //language=java
          java(
            """
              package example;

              class Numbers {
                  static int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            """
              package example;

              import lombok.experimental.UtilityClass;

              @UtilityClass
              class Numbers {
                  int add(int left, int right) {
                      return left + right;
                  }
              }
              """,
            spec -> spec.path("src/main/java/example/Numbers.java")
          )
        );
    }

    @Test
    void convertsClassNestedInInterfaceMember() {
        rewriteRun(
          //language=java
          java(
            """
              interface Outer {
                  class Container {
                      class Utilities {
                          static final int VALUE = 1;
                      }
                  }
              }
              """,
            """
              import lombok.experimental.UtilityClass;

              interface Outer {
                  class Container {
                      @UtilityClass
                      class Utilities {
                          final int VALUE = 1;
                      }
                  }
              }
              """
          )
        );
    }
}
