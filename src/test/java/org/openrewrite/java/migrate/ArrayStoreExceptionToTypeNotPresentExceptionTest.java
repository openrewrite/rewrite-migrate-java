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

    /**
     * Only the try's resources and body are protected by its catches: a call in a finally block, a sibling
     * catch, or a body that runs after the try never throws into these handlers. The immediately invoked
     * lambda and the instance initializer do run inside the protected region, but the bodies of lambdas and
     * classes created in the try are conservatively skipped as a whole, costing only a missed migration.
     */
    @Test
    void retainWhenGetAnnotationIsOutsideTheProtectedRegion() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.annotation.Annotation;
              import java.util.List;
              import java.util.function.Function;

              class Example {
                  void inFinally(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } finally {
                          type.getAnnotation(Override.class);
                      }
                  }

                  void inSiblingCatch(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (IllegalArgumentException e) {
                          type.getAnnotation(Override.class);
                      }
                  }

                  Runnable inDeferredLambda(Class<?> type, Object value) {
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

                  Function<Class<Override>, Override> inMethodReference(Class<?> type, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          return type::getAnnotation;
                      } catch (ArrayStoreException e) {
                          recover(e);
                          return null;
                      }
                  }

                  void inImmediatelyInvokedLambda(List<Class<?>> types, Object value) {
                      try {
                          Object[] values = new String[1];
                          values[0] = value;
                          types.forEach(type -> type.getAnnotation(Override.class));
                      } catch (ArrayStoreException e) {
                          recover(e);
                      }
                  }

                  Runnable inAnonymousClass(Class<?> type, Object value) {
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

                  Runnable inLocalClass(Class<?> type, Object value) {
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

                  Runnable inAnonymousClassInstanceInitializer(Class<?> type, Object value) {
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

    /**
     * A sibling catch of {@code TypeNotPresentException} or a supertype already handles it, a catch of a
     * subclass would become unreachable, and a multi-catch parameter is typed as the least upper bound
     * rather than {@code ArrayStoreException}, so none of these tries change.
     */
    @Test
    void retainWhenTypeNotPresentExceptionIsAlreadyHandled() {
        rewriteRun(
          //language=java
          java(
            """
              class Example {
                  void existingCatch(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (TypeNotPresentException e) {
                          recover(e);
                      }
                  }

                  void existingMultiCatch(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException | IllegalStateException e) {
                          recover(e);
                      }
                  }

                  void existingBroaderCatch(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recover(e);
                      } catch (RuntimeException e) {
                          recover(e);
                      }
                  }

                  void existingSubclassCatch(Class<?> type) {
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
     * The last two handlers mirror real consumers: WildFly's {@code BusinessViewAnnotationProcessor} throws
     * its own deployment error without referencing the parameter, and Grails' {@code DomainClassArtefactHandler}
     * swallows the failure with an empty catch.
     */
    @Test
    void widenCatchWhenHandlerAcceptsRuntimeException() {
        // %1$s fills every catch site with the single formatted argument
        //language=java
        var source = """
          class Example {
              void logAndRethrow(Class<?> type) {
                  try {
                      type.getAnnotation(Override.class);
                  } catch (%1$s e) {
                      System.out.println("failed: " + e.getMessage());
                      e.printStackTrace();
                      throw e;
                  }
              }

              void wrap(Class<?> type) {
                  try {
                      type.getAnnotation(Override.class);
                  } catch (%1$s e) {
                      throw new IllegalStateException("wrap", e);
                  }
              }

              void assign(Class<?> type) {
                  try {
                      type.getAnnotation(Override.class);
                  } catch (%1$s e) {
                      RuntimeException cause = e;
                      cause = e;
                      recover(cause);
                      recover(e);
                  }
              }

              void requireAnnotation(Class<?> type) {
                  try {
                      type.getAnnotation(Override.class);
                  } catch (%1$s e) {
                      throw new IllegalStateException("missing class in annotation on " + type.getName());
                  }
              }

              boolean isDomainClass(Class<?> type) {
                  Deprecated artefact = null;
                  try {
                      artefact = type.getAnnotation(Deprecated.class);
                  } catch (%1$s e) {
                      // a reference to a class that no longer exists
                  }
                  return artefact != null;
              }

              void recover(RuntimeException e) {
              }
          }
          """;
        rewriteRun(
          java(
            source.formatted("ArrayStoreException"),
            source.formatted("ArrayStoreException | TypeNotPresentException")
          )
        );
    }

    /**
     * The allow list deliberately stops at the common handler shapes; any use of the parameter it does not
     * recognize retains the catch, which only costs a migration that is not applied.
     */
    @Test
    void retainCatchWhoseHandlerUsesTheExceptionBeyondTheAllowList() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;
              import java.util.function.Supplier;

              class Example {
                  RuntimeException returnBroaderType(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e;
                      }
                  }

                  Runnable methodReference(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return e::printStackTrace;
                      }
                  }

                  Supplier<ArrayStoreException> expressionLambda(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                          return null;
                      } catch (ArrayStoreException e) {
                          return () -> e;
                      }
                  }

                  void genericInference(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          Objects.requireNonNull(e);
                      }
                  }

                  void ternary(Class<?> type, boolean flag) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          RuntimeException chosen = flag ? e : null;
                          recover(chosen);
                      }
                  }

                  void appendToMessage(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          String message = "failed: ";
                          message += e;
                          System.out.println(message);
                      }
                  }

                  void synchronize(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          synchronized (e) {
                              recover(e);
                          }
                      }
                  }

                  void passToVarargs(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          log("failed", e);
                      }
                  }

                  void passToNarrowerParameter(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          recoverNarrow(e);
                      }
                  }

                  void assignToNarrowerVariable(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException copy = e;
                          recover(copy);
                      }
                  }

                  void storeInArrayInitializer(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          ArrayStoreException[] all = {e};
                          recover(all[0]);
                      }
                  }

                  void castToNarrowerType(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          // The cast still compiles, but would throw for the TypeNotPresentException values a widened handler receives
                          Object narrowed = (ArrayStoreException) e;
                          recover(e);
                      }
                  }

                  void reassignParameter(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          // A multi-catch parameter is implicitly final, so this would not compile widened
                          e = new ArrayStoreException("wrapped");
                          recover(e);
                      }
                  }

                  void readTheExceptionClass(Class<?> type) {
                      try {
                          type.getAnnotation(Override.class);
                      } catch (ArrayStoreException e) {
                          // Per JLS 4.3.2 `e.getClass()` is typed over the receiver's static type, so it widens with it
                          Class<? extends ArrayStoreException> narrow = e.getClass();
                          Class<?> wide = e.getClass();
                          recover(e);
                      }
                  }

                  void log(String message, ArrayStoreException... exceptions) {
                  }

                  void recover(RuntimeException e) {
                  }

                  void recoverNarrow(ArrayStoreException e) {
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
     * name would even compile while binding the catch to the wrong type; the file is left unchanged.
     */
    @Test
    void retainWhenNestedClassShadowsSimpleName() {
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
              """
          )
        );
    }

    @Test
    void retainWhenImportShadowsSimpleName() {
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
              """
          )
        );
    }

    /**
     * A same-package class shadows the simple name even when this file never references it, which is why the
     * scanner records every source-declared class of this name.
     */
    @Test
    void retainWhenSamePackageClassShadowsSimpleName() {
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
     * Within one {@code JavaProject} the declaration still shadows: the marked sibling source is left
     * unchanged exactly as an unmarked one is.
     */
    @Test
    void retainWhenShadowingClassIsDeclaredInTheSameJavaProject() {
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
            spec -> spec.markers(module)
          )
        );
    }

    /**
     * A nested class inherited from a supertype shadows the simple name inside the subclass; it would compile
     * while binding the catch to the inherited type, so the file is left unchanged.
     */
    @Test
    void retainWhenInheritedNestedClassShadowsSimpleName() {
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
              """
          )
        );
    }

}
