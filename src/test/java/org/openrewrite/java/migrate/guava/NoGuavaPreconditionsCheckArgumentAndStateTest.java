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
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class NoGuavaPreconditionsCheckArgumentAndStateTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaPreconditionsCheckArgumentAndState())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void checksBecomeIfThrow() {
        //language=java
        rewriteRun(
          java(
            """
              import static com.google.common.base.Preconditions.checkArgument;
              import static com.google.common.base.Preconditions.checkState;

              class A {
                  boolean open;

                  void f(int count, String name, Object state) {
                      checkArgument(count > 0);
                      checkArgument(name != null, "name is required");
                      checkState(isOpen(), "state %s for %s", state, name);
                  }

                  boolean isOpen() {
                      return open;
                  }
              }
              """,
            """
              class A {
                  boolean open;

                  void f(int count, String name, Object state) {
                      if (!(count > 0)) {
                          throw new IllegalArgumentException();
                      }
                      if (name == null) {
                          throw new IllegalArgumentException("name is required");
                      }
                      if (!isOpen()) {
                          throw new IllegalStateException(String.format("state %s for %s", state, name));
                      }
                  }

                  boolean isOpen() {
                      return open;
                  }
              }
              """
          )
        );
    }

    @Test
    void objectMessageAndNegatedCondition() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Preconditions;

              import java.util.List;

              class A {
                  void f(List<String> list, Object error, int index) {
                      Preconditions.checkState(!list.isEmpty(), error);
                      Preconditions.checkArgument(!(index < 0 || index > list.size()));
                      Preconditions.checkElementIndex(index, list.size());
                  }
              }
              """,
            """
              import com.google.common.base.Preconditions;

              import java.util.List;

              class A {
                  void f(List<String> list, Object error, int index) {
                      if (list.isEmpty()) {
                          throw new IllegalStateException(String.valueOf(error));
                      }
                      if (index < 0 || index > list.size()) {
                          throw new IllegalArgumentException();
                      }
                      Preconditions.checkElementIndex(index, list.size());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesChecksWhoseMessageOrPlacementWouldChange() {
        //language=java
        rewriteRun(
          java(
            """
              import static com.google.common.base.Preconditions.checkArgument;

              class A {
                  void f(int x, int i, String template, Object[] parts) {
                      checkArgument(x > 0, "%s is over 100%", x);
                      checkArgument(x > 0, "%s and %s", x);
                      checkArgument(x > 0, template, x);
                      checkArgument(x > 0, "parts %s", parts);
                      checkArgument(x > 0, "x %s", i++);
                      checkArgument(x > 0, null);
                      if (x > 1) checkArgument(x > 2); else checkArgument(x < 0);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveKotlinAlone() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          //language=kotlin
          kotlin(
            """
              import com.google.common.base.Preconditions

              fun f(name: String?) {
                  Preconditions.checkArgument(name != null, "name is required")
              }
              """
          )
        );
    }
}
