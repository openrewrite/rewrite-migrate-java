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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.guava.NoGuava")
          )
          .parser(
            JavaParser.fromJavaVersion()
              .logCompilationWarningsAndErrors(true)
              .classpath("guava")
          );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/39#issuecomment-910673213")
    void preferJavaUtilObjectsHashCode() {
        rewriteRun(java("""
                package com.acme.product;

                import com.google.common.base.Objects;

                class MyPojo {
                    String x;

                    @Override
                    public int hashCode() {
                        return Objects.hashCode(x);
                    }
                }
            """,
          """
                package com.acme.product;

                import java.util.Objects;

                class MyPojo {
                    String x;

                    @Override
                    public int hashCode() {
                        return Objects.hash(x);
                    }
                }
            """
        ));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/39#issuecomment-910673213")
    void preferJavaUtilObjectsEquals() {
        rewriteRun(java("""
                import com.google.common.base.Objects;

                class Test {
                    static boolean isEqual(Object obj0, Object obj1) {
                        return Objects.equal(obj0, obj1);
                    }
                }
            """,
          """
                import java.util.Objects;

                class Test {
                    static boolean isEqual(Object obj0, Object obj1) {
                        return Objects.equals(obj0, obj1);
                    }
                }
            """
        ));
    }

}
