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
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class ArrayStoreExceptionToTypeNotPresentExceptionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ArrayStoreExceptionToTypeNotPresentException());
    }

    @DocumentExample
    @Test
    void alsoCatchTypeNotPresentException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.*;
              import java.util.*;

              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught Exception");
                      }
                      try {
                          Object.class.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught ArrayStoreException");
                      }
                  }
              }
              """,
            """
              import java.lang.annotation.*;
              import java.util.*;

              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          System.out.println("Caught Exception");
                      }
                      try {
                          Object.class.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          System.out.println("Caught ArrayStoreException");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainOtherCaughtExceptions() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                          o.getClass().getAnnotation(Override.class);
                      } catch (NullPointerException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArrayStoreExceptionWithoutClassGetAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              public class Test {
                  public void testMethod() {
                      try {
                          Object o = "test";
                      } catch (ArrayStoreException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArrayStoreExceptionWhenBodyCanStillThrowIt() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.Annotation;

              class Example {
                  void inspect(Class<?> type, Class<? extends Annotation> annotation, Object value) {
                      try {
                          type.getAnnotation(annotation);
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              import java.lang.annotation.Annotation;

              class Example {
                  void inspect(Class<?> type, Class<? extends Annotation> annotation, Object value) {
                      try {
                          type.getAnnotation(annotation);
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupInTryWithResourcesInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try (Resource resource = new Resource(type.getAnnotation(Override.class))) {
                          resource.use();
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  static class Resource implements AutoCloseable {
                      Resource(Object annotation) {
                      }

                      void use() {
                      }

                      @Override
                      public void close() {
                      }
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try (Resource resource = new Resource(type.getAnnotation(Override.class))) {
                          resource.use();
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  static class Resource implements AutoCloseable {
                      Resource(Object annotation) {
                      }

                      void use() {
                      }

                      @Override
                      public void close() {
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInFinally() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } finally {
                          type.getAnnotation(Override.class);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInSiblingCatch() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (IllegalArgumentException e) {
                          type.getAnnotation(Override.class);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInDeferredLambda() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  Runnable inspectLater(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          return () -> type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return () -> {
                          };
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInMethodReference() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.Function;

              class Example {
                  Function<Class<Override>, Override> inspectLater(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          return type::getAnnotation;
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return null;
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * Whether a lambda created inside the try runs before the try completes cannot be decided locally, so the
     * handler is left alone.
     */
    @Test
    void lookupOnlyInImmediatelyInvokedLambda() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Example {
                  void inspect(List<Class<?>> types, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          types.forEach(type -> type.getAnnotation(Override.class));
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInAnonymousClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  Runnable inspectLater(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          return new Runnable() {
                              @Override
                              public void run() {
                                  type.getAnnotation(Override.class);
                              }
                          };
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return null;
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void lookupOnlyInLocalClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  Runnable inspectLater(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          class Inspector implements Runnable {
                              @Override
                              public void run() {
                                  type.getAnnotation(Override.class);
                              }
                          }
                          return new Inspector();
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return null;
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * Instance initializers run at the {@code new}, inside the protected region, so this lookup can throw into
     * the enclosing catch. The whole anonymous body is left out regardless, costing only a missed migration.
     */
    @Test
    void lookupOnlyInAnonymousClassInstanceInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.Annotation;

              class Example {
                  Runnable inspectLater(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          return new Runnable() {
                              final Annotation a = type.getAnnotation(Override.class);

                              @Override
                              public void run() {
                              }
                          };
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return null;
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void existingTypeNotPresentExceptionCatch() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void existingMultiCatch() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | IllegalStateException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void existingBroaderCatchAlreadyHandlesTypeNotPresentException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (RuntimeException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * A catch of a `TypeNotPresentException` subclass would become unreachable if the earlier handler widened.
     */
    @Test
    void existingTypeNotPresentExceptionSubclassCatch() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (MissingType e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  static class MissingType extends TypeNotPresentException {
                      MissingType() {
                          super("Missing", null);
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * The widened parameter is typed `RuntimeException`, which `Throwable` methods, concatenation and rethrow
     * all tolerate.
     */
    @Test
    void alsoCatchWhenHandlerLogsAndRethrowsTheException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          System.out.println("failed: " + e.getMessage());
                          e.printStackTrace();
                          throw e;
                      }
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          System.out.println("failed: " + e.getMessage());
                          e.printStackTrace();
                          throw e;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchWhenHandlerWrapsTheException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          throw new IllegalStateException("wrap", e);
                      }
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          throw new IllegalStateException("wrap", e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchWhenHandlerAssignsTheExceptionToABroaderVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          RuntimeException cause = e;
                          RuntimeException chosen = flag ? e : null;
                          recover(cause);
                          recover(chosen);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          RuntimeException cause = e;
                          RuntimeException chosen = flag ? e : null;
                          recover(cause);
                          recover(chosen);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchWhenHandlerReturnsTheExceptionAsABroaderType() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  RuntimeException inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e;
                      }
                  }
              }
              """,
            """
              class Example {
                  RuntimeException inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          return e;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchWhenHandlerUsesAMethodReferenceOnTheException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  Runnable printer(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e::printStackTrace;
                      }
                  }
              }
              """,
            """
              class Example {
                  Runnable printer(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          return e::printStackTrace;
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * `Objects.requireNonNull` reports an inferred `ArrayStoreException` parameter, but `<T> T requireNonNull(T)`
     * simply re-infers after widening.
     */
    @Test
    void alsoCatchWhenHandlerChecksTheExceptionWithRequireNonNull() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Objects.requireNonNull(e);
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              import java.util.Objects;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          Objects.requireNonNull(e);
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * The widened parameter is typed `RuntimeException`, which an `ArrayStoreException...` parameter rejects.
     */
    @Test
    void retainCatchThatPassesTheExceptionToAVarargsParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void log(String message, ArrayStoreException... exceptions) {
                  }

                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          log("failed", e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatPassesTheExceptionToANarrowerParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(ArrayStoreException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatAssignsTheExceptionToANarrowerVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException copy = e;
                          recover(copy);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatUsesTheExceptionInATernaryInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException copy = flag ? e : null;
                          recover(copy);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatPassesATernaryToANarrowerParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(flag ? e : null);
                      }
                  }

                  void recover(ArrayStoreException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatStoresTheExceptionInAnArrayInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException[] all = {e};
                          recover(all[0]);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatReturnsTheExceptionFromAnExpressionLambda() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.Supplier;

              class Example {
                  Supplier<ArrayStoreException> inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return () -> e;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatReturnsTheExceptionAsTheNarrowerType() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  ArrayStoreException inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e;
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * The cast still compiles, but throws for the `TypeNotPresentException` values the widened handler receives.
     */
    @Test
    void retainCatchThatCastsTheExceptionToTheNarrowerType() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Object narrowed = (ArrayStoreException) e;
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * An unresolvable receiving method has unknowable requirements, so the catch is left alone.
     */
    @Test
    void retainCatchThatPassesTheExceptionToAnUnresolvableMethod() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Unknown.log(e);
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * A multi-catch parameter is implicitly final, so widening this handler would not compile.
     */
    @Test
    void retainCatchThatReassignsTheException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          e = new ArrayStoreException("wrapped");
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * Only the caught exception is implicitly final; an unrelated variable that happens to share its name is not.
     */
    @Test
    void alsoCatchWhenAnUnrelatedSameNamedVariableIsAssigned() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                          run(new Runnable() {
                              String e;

                              @Override
                              public void run() {
                                  e = "unrelated";
                              }
                          });
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  void run(Runnable runnable) {
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          recover(e);
                          run(new Runnable() {
                              String e;

                              @Override
                              public void run() {
                                  e = "unrelated";
                              }
                          });
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  void run(Runnable runnable) {
                  }
              }
              """
          )
        );
    }

    /**
     * Kotlin has no multi-catch, so Kotlin sources are left alone.
     */
    @Test
    void kotlinFileNotChanged() {
        rewriteRun(
          //language=kotlin
          kotlin(
            """
              class Example {
                  fun inspect(type: Class<*>) {
                      try {
                          type.getAnnotation(Override::class.java)
                      } catch (e: ArrayStoreException) {
                          recover(e)
                      }
                  }

                  fun recover(e: RuntimeException) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArrayStoreExceptionWhenLookupIsNotTypeAttributed() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          //language=java
          java(
            """
              class Example {
                  void inspect(Unknown type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * Per JLS 4.3.2 the type of {@code e.getClass()} is {@code Class<? extends |E|>} where {@code |E|} is the
     * erasure of the receiver's static type, so widening the receiver would change this initializer's type
     * from {@code Class<? extends ArrayStoreException>} to {@code Class<? extends RuntimeException>}, which no
     * longer compiles.
     */
    @Test
    void retainCatchThatReadsTheExceptionClassAsTheNarrowerClassType() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Class<? extends ArrayStoreException> narrow = e.getClass();
                          Class<? extends ArrayStoreException> viaTernary = flag ? (e.getClass()) : null;
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatPassesTheExceptionClassToANarrowerClassParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          report(e.getClass());
                      }
                  }

                  void report(Class<? extends ArrayStoreException> failure) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainCatchThatBindsTheExceptionClassMethodReference() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.Supplier;

              class Example {
                  Supplier<Class<? extends ArrayStoreException>> inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e::getClass;
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * {@code Class.cast()} returns the class's own type argument, so after widening it would return
     * {@code RuntimeException} rather than {@code ArrayStoreException}.
     */
    @Test
    void retainCatchThatCastsThroughTheExceptionClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type, Object value) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException narrowed = e.getClass().cast(value);
                          recover(narrowed);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * {@code Objects.requireNonNull(e)} re-infers the parameter's own type, so {@code getClass()} on its
     * result depends on the widening just as it does on the parameter directly.
     */
    @Test
    void retainCatchThatReadsTheLaunderedExceptionClass() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Class<? extends ArrayStoreException> narrow = Objects.requireNonNull(e).getClass();
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * {@code Class<?>} and signatures like {@code getName()} that do not involve the class's type argument
     * tolerate the widened {@code Class<? extends RuntimeException>}, so common logging keeps migrating.
     */
    @Test
    void alsoCatchWhenHandlerReadsTheExceptionClassGenerically() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Class<?> wide = e.getClass();
                          String name = e.getClass().getName();
                          System.out.println("caught " + name + wide);
                      }
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          Class<?> wide = e.getClass();
                          String name = e.getClass().getName();
                          System.out.println("caught " + name + wide);
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * Every {@code TypeNotPresentException} the inner try does not catch reaches the enclosing handler, so
     * widening the inner catch would steal the exception from it.
     */
    @Test
    void retainWhenEnclosingTryHandlesTypeNotPresentException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          try {
                              type.getAnnotation(Override.class);
                          } catch (ArrayStoreException e) {
                              recover(e);
                          }
                      } catch (TypeNotPresentException e) {
                          handleMissingType(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  void handleMissingType(TypeNotPresentException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * From inside a catch block the enclosing try's catches are no longer reachable, so an inner try there is
     * not stealing from them and still migrates.
     */
    @Test
    void alsoCatchInsideHandlerOfEnclosingTry() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getTypeParameters();
                      } catch (TypeNotPresentException missing) {
                          try {
                              type.getAnnotation(Override.class);
                          } catch (ArrayStoreException e) {
                              recover(e);
                          }
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getTypeParameters();
                      } catch (TypeNotPresentException missing) {
                          try {
                              type.getAnnotation(Override.class);
                          } catch (ArrayStoreException | TypeNotPresentException e) {
                              recover(e);
                          }
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * The nested class shadows the simple name, and because it extends {@code RuntimeException} the simple
     * name would even compile while binding the catch to the wrong type; the fully qualified name is emitted.
     */
    @Test
    void alsoCatchFullyQualifiedWhenNestedClassShadowsSimpleName() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  static class TypeNotPresentException extends RuntimeException {
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | java.lang.TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }

                  static class TypeNotPresentException extends RuntimeException {
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchFullyQualifiedWhenImportShadowsSimpleName() {
        rewriteRun(
          //language=java
          java(
            """
              package shadow;

              public class TypeNotPresentException {
              }
              """
          ),
          //language=java
          java(
            """
              package example;

              import shadow.TypeNotPresentException;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              package example;

              import shadow.TypeNotPresentException;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | java.lang.TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * A same-package class shadows the simple name even when this file never references it, which is why the
     * scanner records every source-declared class of this name.
     */
    @Test
    void alsoCatchFullyQualifiedWhenSamePackageClassShadowsSimpleName() {
        rewriteRun(
          //language=java
          java(
            """
              package example;

              class TypeNotPresentException {
              }
              """
          ),
          //language=java
          java(
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | java.lang.TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * The scanner's record of declared {@code TypeNotPresentException} classes is scoped per
     * {@code JavaProject} marker: a class declared in one module of a multi-module repository is not on
     * another module's horizon, so there the simple name is emitted.
     */
    @Test
    void alsoCatchSimpleNameWhenShadowingClassIsDeclaredInAnotherJavaProject() {
        var moduleA = new JavaProject(randomId(), "module-a", null);
        var moduleB = new JavaProject(randomId(), "module-b", null);
        rewriteRun(
          //language=java
          java(
            """
              package example;

              class TypeNotPresentException {
              }
              """,
            spec -> spec.markers(moduleA)
          ),
          //language=java
          java(
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            spec -> spec.markers(moduleB)
          )
        );
    }

    /**
     * Within one {@code JavaProject} the declaration still shadows: the marked sibling source qualifies the
     * name exactly as an unmarked one does.
     */
    @Test
    void alsoCatchFullyQualifiedWhenShadowingClassIsDeclaredInTheSameJavaProject() {
        var module = new JavaProject(randomId(), "module-a", null);
        rewriteRun(
          //language=java
          java(
            """
              package example;

              class TypeNotPresentException {
              }
              """,
            spec -> spec.markers(module)
          ),
          //language=java
          java(
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              package example;

              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | java.lang.TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            spec -> spec.markers(module)
          )
        );
    }

    /**
     * A nested class inherited from a supertype shadows the simple name inside the subclass; it would compile
     * while binding the catch to the inherited type, so the fully qualified name is emitted.
     */
    @Test
    void alsoCatchFullyQualifiedWhenInheritedNestedClassShadowsSimpleName() {
        rewriteRun(
          //language=java
          java(
            """
              package example;

              public class Base {
                  public static class TypeNotPresentException extends RuntimeException {
                  }
              }
              """
          ),
          //language=java
          java(
            """
              package example;

              class Example extends Base {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              package example;

              class Example extends Base {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | java.lang.TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * {@code message += e} concatenates like {@code message = message + e}, which tolerates any
     * {@code RuntimeException}.
     */
    @Test
    void alsoCatchWhenHandlerAppendsTheExceptionToAMessage() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          String message = "failed: ";
                          message += e;
                          System.out.println(message);
                      }
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          String message = "failed: ";
                          message += e;
                          System.out.println(message);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void alsoCatchWhenHandlerSynchronizesOnTheException() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          synchronized (e) {
                              recover(e);
                          }
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              class Example {
                  void inspect(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          synchronized (e) {
                              recover(e);
                          }
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }

    /**
     * An unbraced statement discards the call's value exactly like a braced one, so migration must not depend
     * on brace style.
     */
    @Test
    void alsoCatchWhenHandlerChecksTheExceptionInAnUnbracedIf() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          if (flag) Objects.requireNonNull(e);
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """,
            """
              import java.util.Objects;

              class Example {
                  void inspect(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | TypeNotPresentException e) {
                          if (flag) Objects.requireNonNull(e);
                          recover(e);
                      }
                  }

                  void recover(RuntimeException e) {
                  }
              }
              """
          )
        );
    }
}
