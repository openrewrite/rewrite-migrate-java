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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaPreconditionsCheckNotNullFormatTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaPreconditionsCheckNotNullFormat())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void formatsTheMessageOnlyWhenTheReferenceIsNull() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Preconditions;

              import static com.google.common.base.Preconditions.checkNotNull;

              class A {
                  private final Object value;

                  A(Object value, String key, int id) {
                      this.value = Preconditions.checkNotNull(value, "no value for %s", key);
                      checkNotNull(key, "no key in %s at %s", this, id);
                  }
              }
              """,
            """
              import static java.util.Objects.requireNonNull;

              class A {
                  private final Object value;

                  A(Object value, String key, int id) {
                      this.value = requireNonNull(value, () -> String.format("no value for %s", key));
                      requireNonNull(key, () -> String.format("no key in %s at %s", this, id));
                  }
              }
              """
          )
        );
    }

    @Test
    void leftAloneWhereTheLambdaCannotCaptureAnArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import static com.google.common.base.Preconditions.checkNotNull;

              class A {
                  void loop(Object[] values) {
                      for (int i = 0; i < values.length; i++) {
                          checkNotNull(values[i], "no value at %s", i);
                      }
                  }

                  void reassigned(Object value, String key) {
                      key = key.trim();
                      checkNotNull(value, "no value for %s", key);
                  }

                  void compound(Object value, int count) {
                      count += 1;
                      checkNotNull(value, "no value after %s", count);
                  }
              }
              """
          )
        );
    }

    @Test
    void leftAloneWhereStringFormatWouldFormatDifferently() {
        //language=java
        rewriteRun(
          java(
            """
              import static com.google.common.base.Preconditions.checkNotNull;

              class A {
                  static final String TEMPLATE = "no value for %s";

                  void f(Object value, int id, Object[] ids) {
                      checkNotNull(value, "%s is 100%% missing", id);
                      checkNotNull(value, "no value for %s", id, id);
                      checkNotNull(value, TEMPLATE, id);
                      checkNotNull(value, "no value for %s", ids);
                  }
              }
              """
          )
        );
    }
}
