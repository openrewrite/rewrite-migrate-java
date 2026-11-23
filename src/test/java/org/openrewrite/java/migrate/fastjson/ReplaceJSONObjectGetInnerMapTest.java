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
package org.openrewrite.java.migrate.fastjson;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceJSONObjectGetInnerMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceJSONObjectGetInnerMap())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void innerMapIsTheJSONObject() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONObject;

              import java.util.Map;

              class A {
                  Map<String, Object> a(JSONObject o) {
                      return o.getInnerMap();
                  }

                  Map<String, Object> b(String s) {
                      return JSON.parseObject(s).getInnerMap();
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONObject;

              import java.util.Map;

              class A {
                  Map<String, Object> a(JSONObject o) {
                      return o;
                  }

                  Map<String, Object> b(String s) {
                      return JSON.parseObject(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void inSubclass() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;

              import java.util.Map;

              class A extends JSONObject {
                  Map<String, Object> a() {
                      return getInnerMap();
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSONObject;

              import java.util.Map;

              class A extends JSONObject {
                  Map<String, Object> a() {
                      return this;
                  }
              }
              """
          )
        );
    }
}
