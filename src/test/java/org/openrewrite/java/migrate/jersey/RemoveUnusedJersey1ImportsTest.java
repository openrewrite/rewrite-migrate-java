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
package org.openrewrite.java.migrate.jersey;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveUnusedJersey1ImportsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveUnusedJersey1Imports())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1.19", "jersey-core-1.19"));
    }

    @DocumentExample
    @Test
    void removeUnusedImports() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.*;
              import com.sun.jersey.core.util.Base64;
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import java.util.List;

              import static com.sun.jersey.api.client.ClientResponse.Status.OK;

              class A {
                  List<MultivaluedMapImpl> forms;
              }
              """,
            """
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import java.util.List;

              class A {
                  List<MultivaluedMapImpl> forms;
              }
              """
          )
        );
    }

    @Test
    void keepImportsInUse() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.*;
              import com.sun.jersey.core.util.Base64;

              import static com.sun.jersey.api.client.ClientResponse.Status.OK;

              class A {
                  boolean ok(ClientResponse response) {
                      return response.getClientResponseStatus() == OK;
                  }

                  byte[] encode(String s) {
                      return Base64.encode(s);
                  }
              }
              """
          )
        );
    }
}
