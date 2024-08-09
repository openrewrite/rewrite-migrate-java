/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.net;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class URLConstructorsToURITest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new URLConstructorsToURIRecipes());
    }

    @Test
    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/191")
    void urlConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              import java.net.URL;

              class Test {
                  void urlConstructor(String spec) throws Exception {
                      URL url1 = new URL(spec);
                      URL url2 = new URL(spec, "localhost", "file");
                      URL url3 = new URL(spec, "localhost", 8080, "file");
                  }
              }
              """,
            """
              import java.net.URI;
              import java.net.URL;

              class Test {
                  void urlConstructor(String spec) throws Exception {
                      URL url1 = URI.create(spec).toURL();
                      URL url2 = new URI(spec, null, "localhost", -1, "file", null, null).toURL();
                      URL url3 = new URI(spec, null, "localhost", 8080, "file", null, null).toURL();
                  }
              }
              """
          )
        );
    }
}
