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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class InliningsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Inlinings())
          .parser(JavaParser.fromJavaVersion().classpath("guava", "error_prone_annotations"));
    }

    @Test
    @DocumentExample
    void inlineMe() {
        //language=java
        rewriteRun(
          java(
            """
              package m;

              import com.google.errorprone.annotations.InlineMe;
              import java.time.Duration;

              public final class MyClass {
                  private final Duration deadline;

                  public Duration getDeadline() {
                      return deadline;
                  }

                  @Deprecated
                  @InlineMe(replacement = "this.getDeadline().toMillis()")
                  public long getDeadlineMillis() {
                      return getDeadline().toMillis();
                  }
              }
              """,
            SourceSpec::skip
          ),
          java(
            """
              import m.MyClass;
              class Foo {
                  void foo(MyClass myClass) {
                      myClass.getDeadlineMillis();
                  }
              }
              """,
            """
              import m.MyClass;
              class Foo {
                  void foo(MyClass myClass) {
                      myClass.getDeadline().toMillis();
                  }
              }
              """
          )
        );
    }
}
