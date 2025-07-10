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
package org.openrewrite.java.migrate.nio.file;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PathsGetToPathOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.Java8toJava11");
    }

    @DocumentExample
    @Test
    void convertPathsGetToPathOf() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.file.Path;
              import java.nio.file.Paths;
              import java.net.URI;
              class A {
                  Path pathA = Paths.get("path");
                  Path pathB = Paths.get("path", "subpath");
                  Path pathC = Paths.get(URI.create("file:///path"));
              }
              """,
            """
              import java.nio.file.Path;
              import java.net.URI;
              class A {
                  Path pathA = Path.of("path");
                  Path pathB = Path.of("path", "subpath");
                  Path pathC = Path.of(URI.create("file:///path"));
              }
              """
          )
        );
    }
}
