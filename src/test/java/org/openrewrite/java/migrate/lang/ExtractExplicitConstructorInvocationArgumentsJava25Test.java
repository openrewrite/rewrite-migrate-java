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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.condition.JRE.JAVA_25;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@EnabledForJreRange(min = JAVA_25)
class ExtractExplicitConstructorInvocationArgumentsJava25Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .allSources(src -> src.markers(javaVersion(25)))
          .recipe(new ExtractExplicitConstructorInvocationArguments());
    }

    @Test
    void extractsSymphonyConstructorArgumentsBeforeSuperInvocation() {
        rewriteRun(
          java(
            """
              import java.time.Clock;
              import java.util.Objects;

              class Parent {
                  Parent(Clock clock, String config) {
                  }
              }

              class Child extends Parent {
                  Child(Clock clock, String config) {
                      super(Objects.requireNonNull(clock), Objects.requireNonNull(config));
                  }
              }
              """,
            """
              import java.time.Clock;
              import java.util.Objects;

              class Parent {
                  Parent(Clock clock, String config) {
                  }
              }

              class Child extends Parent {
                  Child(Clock clock, String config) {
                      Clock clock1 = Objects.requireNonNull(clock);
                      String config1 = Objects.requireNonNull(config);
                      super(clock1, config1);
                  }
              }
              """
          )
        );
    }
}
