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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpgradeJavaGradleReleaseVersionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeJavaGradleReleaseVersion(25))
          .parser(JavaParser.fromJavaVersion().dependsOn(
            "package org.gradle.api.provider; public interface Provider<T> {}",
            "package org.gradle.api.provider; public interface Property<T> { void set(T value); void set(Provider<? extends T> value); }",
            "package org.gradle.api.tasks.compile; import org.gradle.api.provider.Property; public abstract class CompileOptions { public abstract Property<Integer> getRelease(); }",
            "package org.gradle.api.tasks.compile; public abstract class JavaCompile { public abstract CompileOptions getOptions(); }"
          ));
    }

    @DocumentExample
    @Test
    void upgradeJavaConvention() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(25)),
          java(
            """
              import org.gradle.api.tasks.compile.JavaCompile;
              class Convention {
                  void configure(JavaCompile compile) {
                      compile.getOptions().getRelease().set(17);
                  }
              }
              """,
            """
              import org.gradle.api.tasks.compile.JavaCompile;
              class Convention {
                  void configure(JavaCompile compile) {
                      compile.getOptions().getRelease().set(25);
                  }
              }
              """
          )
        );
    }

    @Test
    void upgradeAgainstGradleApi() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("gradle-api")),
          java(
            """
              import org.gradle.api.tasks.compile.CompileOptions;
              class Convention {
                  void configure(CompileOptions options) {
                      options.getRelease().set(17);
                  }
              }
              """,
            """
              import org.gradle.api.tasks.compile.CompileOptions;
              class Convention {
                  void configure(CompileOptions options) {
                      options.getRelease().set(25);
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesHigherEqualAndDynamicValues() {
        rewriteRun(
          java(
            """
              import org.gradle.api.provider.Provider;
              import org.gradle.api.tasks.compile.CompileOptions;
              class Convention {
                  static final int RELEASE = 17;
                  void configure(CompileOptions options, int release, Provider<Integer> provider) {
                      options.getRelease().set(26);
                      options.getRelease().set(25);
                      options.getRelease().set(RELEASE);
                      options.getRelease().set(release);
                      options.getRelease().set(Integer.parseInt("17"));
                      options.getRelease().set(provider);
                      options.getRelease().set((Integer) null);
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesUnrelatedPropertiesAndLookalikes() {
        rewriteRun(
          java(
            """
              import org.gradle.api.provider.Property;
              class Convention {
                  interface Lookalike { Property<Integer> getRelease(); }
                  void configure(Property<Integer> unrelated, Lookalike lookalike) {
                      unrelated.set(17);
                      lookalike.getRelease().set(17);
                  }
              }
              """
          )
        );
    }

    @Test
    void supportsOtherRequestedVersions() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaGradleReleaseVersion(21)),
          java(
            """
              import org.gradle.api.tasks.compile.CompileOptions;
              class Convention {
                  void configure(CompileOptions options) {
                      options.getRelease().set(8);
                      options.getRelease().set(25);
                  }
              }
              """,
            """
              import org.gradle.api.tasks.compile.CompileOptions;
              class Convention {
                  void configure(CompileOptions options) {
                      options.getRelease().set(21);
                      options.getRelease().set(25);
                  }
              }
              """
          )
        );
    }
}
