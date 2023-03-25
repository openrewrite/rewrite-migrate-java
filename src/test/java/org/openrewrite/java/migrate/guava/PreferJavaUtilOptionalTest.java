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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/197")
class PreferJavaUtilOptionalTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.guava.PreferJavaUtilOptional")
          )
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void optionalAbsent() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              Optional absentToEmpty() {
                  return Optional.absent();
              }
          }
          """, """
          import java.util.Optional;

          class A {
              Optional absentToEmpty() {
                  return Optional.empty();
              }
          }
          """));
    }

    @Test
    @ExpectedToFail("Not yet implemented")
    void removeToJavaUtil() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Optional;

          class A {
              boolean absentToEmpty() {
                  return Optional.absent().toJavaUtil().isEmpty();
              }
          }
          """, """
          import java.util.Optional;

          class A {
              boolean absentToEmpty() {
                  return Optional.empty().isEmpty();
              }
          }
          """));
    }
}
