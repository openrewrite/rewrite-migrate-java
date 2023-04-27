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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/197")
class PreferJavaUtilOptionalTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.guava.NoGuava")
          )
          .parser(JavaParser.fromJavaVersion().classpath("rewrite-java", "guava"));
    }

    @DocumentExample
    @Test
    void absentToEmpty() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              Optional<String> foo() {
                  return Optional.absent();
              }
          }
          """, """
          import java.util.Optional;

          class A {
              Optional<String> foo() {
                  return Optional.empty();
              }
          }
          """));
    }

    @Test
    void orNullToOrElseNull() {
        // Comparison to java.util.Optional: this method is equivalent to Java 8's Optional.orElse(null).
        //language=java
        rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  String foo(Optional<String> optional) {
                      return optional.orNull();
                  }
              }
              """, """
              import java.util.Optional;

              class A {
                  String foo(Optional<String> optional) {
                      return optional.orElse(null);
                  }
              }
              """));
    }

    @Test
    void orToOrElse() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.or("other");
              }
          }
          """, """
          import java.util.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.orElse("other");
              }
          }
          """));
    }

    @Test
    void orSupplierToOrElseGetWithLambda() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.or(() -> "other");
              }
          }
          """, """
          import java.util.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.orElseGet(() -> "other");
              }
          }
          """));
    }

    @Test
    void orSupplierToOrElseGetWithSupplierArgument() {
        //language=java
        rewriteRun(
          java("""
              import com.google.common.base.Optional;
              import com.google.common.base.Supplier;

              class A {
                  String foo(Optional<String> optional, Supplier<String> supplier) {
                      return optional.or(supplier);
                  }
              }
              """, """
              import java.util.Optional;
              import java.util.function.Supplier;

              class A {
                  String foo(Optional<String> optional, Supplier<String> supplier) {
                      return optional.orElseGet(supplier);
                  }
              }
              """));
    }

    @Test
    void transformToMap() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.transform(String::toUpperCase);
              }
          }
          """, """
          import java.util.Optional;

          class A {
              Optional<String> foo(Optional<String> optional) {
                  return optional.map(String::toUpperCase);
              }
          }
          """));
    }

    @Test
    void removeFromJavaUtil() {
        //language=java
        rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  Optional<String> foo(java.util.Optional<String> optional) {
                      return Optional.fromJavaUtil(optional);
                  }
              }
              """, """
              import java.util.Optional;

              class A {
                  Optional<String> foo(java.util.Optional<String> optional) {
                      return optional;
                  }
              }
              """));
    }

    @Test
    void removeToJavaUtil() {
        //language=java
        rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  boolean foo() {
                      return Optional.absent().toJavaUtil().isEmpty();
                  }
              }
              """, """
              import java.util.Optional;

              class A {
                  boolean foo() {
                      return Optional.empty().isEmpty();
                  }
              }
              """));
    }

    @Test
    void orOptionalToOrSupplier() {
        // Comparison to java.util.Optional: this method has no equivalent in Java 8's Optional class; write thisOptional.isPresent() ? thisOptional : secondChoice instead.
        //language=java
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(9))),
          java("""
              import com.google.common.base.Optional;

              class A {
                  Optional<String> foo(Optional<String> firstChoice, Optional<String> secondChoice) {
                      return firstChoice.or(secondChoice);
                  }
              }
              """, """
              import java.util.Optional;

              class A {
                  Optional<String> foo(Optional<String> firstChoice, Optional<String> secondChoice) {
                      return firstChoice.or(() -> secondChoice);
                  }
              }
              """));
    }

    @Nested
    class NotYetImplemented {

        @DocumentExample
        @Test
        @ExpectedToFail("Not yet implemented")
        void getCatchIllegalStateExceptionToNoSuchElementException() {
            // > when the value is absent, this method throws IllegalStateException, whereas the Java 8 counterpart throws NoSuchElementException.
            // Sure hope no one actually does this, but you never know.
            //language=java
            rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  String foo(Optional<String> optional) {
                      try {
                          return optional.get();
                      } catch (IllegalStateException e) {
                          return "";
                      }
                  }
              }
              """, """
              import java.util.Optional;

              class A {
                  String foo(Optional<String> optional) {
                      try {
                          return optional.get();
                      } catch (NoSuchElementException e) {
                          return "";
                      }
                  }
              }
              """));
        }

        @Test
        @ExpectedToFail("Not yet implemented")
        void asSetToStreamCollectToSet() {
            // Comparison to java.util.Optional: this method has no equivalent in Java 8's Optional class. However, some use cases can be written with calls to optional.stream().
            //language=java
            rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  Set<String> foo(Optional<String> optional) {
                      return optional.asSet();
                  }
              }
              """, """
              import java.util.Optional;
              import java.util.Set;
              import java.util.stream.Collectors;

              class A {
                  Set<String> foo(Optional<String> optional) {
                      return optional.stream().collect(Collectors.toSet());
                  }
              }
              """));
        }

        @Test
        @ExpectedToFail("Not yet implemented")
        void presentInstances() {
            // Comparison to java.util.Optional: this method has no equivalent in Java 8's Optional class; use optionals.stream().filter(Optional::isPresent).map(Optional::get) instead.
            //language=java
            rewriteRun(java("""
              import com.google.common.base.Optional;

              class A {
                  Iterable<String> foo(Iterable<Optional<String>> optionals) {
                      return Optional.presentInstances(optionals);
                  }
              }
              """, """
              import java.util.Optional;
              import java.util.stream.Collectors;
                        
              class A {
                  Iterable<String> foo(Iterable<Optional<String>> optionals) {
                      return optionals.stream().flatMap(Optional::stream).collect(Collectors.toList());
                  }
              }
              """));
        }
    }
}