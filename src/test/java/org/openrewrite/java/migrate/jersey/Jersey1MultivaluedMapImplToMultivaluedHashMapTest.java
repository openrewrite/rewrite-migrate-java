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

class Jersey1MultivaluedMapImplToMultivaluedHashMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1MultivaluedMapImplToMultivaluedHashMap())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void queryParameters() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import javax.ws.rs.core.MultivaluedMap;

              class Test {
                  String search(WebResource resource, String query, int limit) {
                      MultivaluedMap<String, String> params = new MultivaluedMapImpl();
                      params.add("q", query);
                      params.add("limit", String.valueOf(limit));
                      return resource.queryParams(params).get(String.class);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.WebResource;

              import javax.ws.rs.core.MultivaluedHashMap;
              import javax.ws.rs.core.MultivaluedMap;

              class Test {
                  String search(WebResource resource, String query, int limit) {
                      MultivaluedMap<String, String> params = new MultivaluedHashMap<String, String>();
                      params.add("q", query);
                      params.add("limit", String.valueOf(limit));
                      return resource.queryParams(params).get(String.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void declaredTypes() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import java.util.List;
              import java.util.Map;

              class Test {
                  private final MultivaluedMapImpl form = new MultivaluedMapImpl();
                  private List<MultivaluedMapImpl> forms;

                  MultivaluedMapImpl copy(MultivaluedMapImpl original) {
                      MultivaluedMapImpl copy = new MultivaluedMapImpl(original);
                      for (Map.Entry<String, List<String>> entry : original.entrySet()) {
                          copy.put(entry.getKey(), entry.getValue());
                      }
                      return copy;
                  }

                  boolean isImpl(Object o) {
                      return o instanceof MultivaluedMapImpl && ((MultivaluedMapImpl) o).isEmpty();
                  }
              }
              """,
            """
              import javax.ws.rs.core.MultivaluedHashMap;

              import java.util.List;
              import java.util.Map;

              class Test {
                  private final MultivaluedHashMap<String, String> form = new MultivaluedHashMap<String, String>();
                  private List<MultivaluedHashMap<String, String>> forms;

                  MultivaluedHashMap<String, String> copy(MultivaluedHashMap<String, String> original) {
                      MultivaluedHashMap<String, String> copy = new MultivaluedHashMap<String, String>(original);
                      for (Map.Entry<String, List<String>> entry : original.entrySet()) {
                          copy.put(entry.getKey(), entry.getValue());
                      }
                      return copy;
                  }

                  boolean isImpl(Object o) {
                      return o instanceof MultivaluedHashMap && ((MultivaluedHashMap<String, String>) o).isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void valuesThatAreNotStrings() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              class Test {
                  MultivaluedMapImpl params(int page, Long since, String name) {
                      MultivaluedMapImpl params = new MultivaluedMapImpl();
                      params.add("page", page);
                      params.putSingle("since", since);
                      params.add("name", name);
                      return params;
                  }
              }
              """,
            """
              import javax.ws.rs.core.MultivaluedHashMap;

              import java.util.Objects;

              class Test {
                  MultivaluedHashMap<String, String> params(int page, Long since, String name) {
                      MultivaluedHashMap<String, String> params = new MultivaluedHashMap<String, String>();
                      params.add("page", String.valueOf(page));
                      params.putSingle("since", Objects.toString(since, ""));
                      params.add("name", name);
                      return params;
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesSubclassesAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              class Form extends MultivaluedMapImpl {
                  static MultivaluedMapImpl create() {
                      return new MultivaluedMapImpl() {{
                          add("a", "b");
                      }};
                  }
              }
              """
          )
        );
    }
}
