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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateCollectionsSingletonListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsSingletonList())
          .allSources(s -> s.markers(javaVersion(9)));
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/186")
    @Test
    void templateError() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              interface ConnectionListener {
               void onCreate();
              }

              class A {
                  public void setConnectionListeners(List<? extends ConnectionListener> listeners) {
                  }

                  public void test() {
                      setConnectionListeners(Collections.singletonList(new ConnectionListener() {
                          @Override
                          public void onCreate() {
                          }
                      }));
                  }
              }
              """,
            """
              import java.util.List;

              interface ConnectionListener {
               void onCreate();
              }

              class A {
                  public void setConnectionListeners(List<? extends ConnectionListener> listeners) {
                  }

                  public void test() {
                      setConnectionListeners(List.of(new ConnectionListener() {
                          @Override
                          public void onCreate() {
                          }
                      }));
                  }
              }
              """
          )
        );
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonList() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  List<String> list = Collections.singletonList("ABC");
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> list = List.of("ABC");
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonListStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;
              import static java.util.Collections.singletonList;

              class Test {
                  List<String> list = singletonList("ABC");
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> list = List.of("ABC");
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonListCustomType() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;
              import java.time.LocalDate;

              class Test {
                  List<LocalDate> list = Collections.singletonList(LocalDate.now());
              }
              """,
            """
              import java.util.List;
              import java.time.LocalDate;

              class Test {
                  List<LocalDate> list = List.of(LocalDate.now());
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/150")
    @Test
    void lombokAllArgsConstructor() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("lombok")),
          //language=java
          java(
            """
              import lombok.AllArgsConstructor;
              import java.util.List;

              import static java.util.Collections.singletonList;

              enum FooEnum {
                  FOO, BAR;

                  @AllArgsConstructor
                  public enum BarEnum {
                      foobar(singletonList(FOO));

                      private final List<FooEnum> expectedStates;
                  }
              }
              """,
            """
              import lombok.AllArgsConstructor;
              import java.util.List;

              enum FooEnum {
                  FOO, BAR;

                  @AllArgsConstructor
                  public enum BarEnum {
                      foobar(List.of(FOO));

                      private final List<FooEnum> expectedStates;
                  }
              }"""
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/571")
    @Test
    void shouldNotConvertLiteralNull() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  List<String> list = Collections.singletonList(null);
              }
              """
          )
        );
    }
}
