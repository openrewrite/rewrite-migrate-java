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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class CallFastjsonStaticMethodsOnJSONTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CallFastjsonStaticMethodsOnJSON())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void staticMethodsFastjson2JSONObjectDoesNotDeclare() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;

              import java.util.List;

              class A {
                  List<String> a(String s) {
                      return JSONObject.parseArray(s, String.class);
                  }

                  Object b(Object o) {
                      return JSONObject.toJSON(o);
                  }

                  byte[] c(Object o) {
                      return JSONObject.toJSONBytes(o);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;

              import java.util.List;

              class A {
                  List<String> a(String s) {
                      return JSON.parseArray(s, String.class);
                  }

                  Object b(Object o) {
                      return JSON.toJSON(o);
                  }

                  byte[] c(Object o) {
                      return JSON.toJSONBytes(o);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticMethodsFastjson2JSONArrayDoesNotDeclare() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONArray;
              import com.alibaba.fastjson.JSONObject;

              class A {
                  JSONObject a(String s) {
                      return JSONArray.parseObject(s);
                  }

                  JSONArray b(String s) {
                      return JSONArray.parseArray(s);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONArray;
              import com.alibaba.fastjson.JSONObject;

              class A {
                  JSONObject a(String s) {
                      return JSON.parseObject(s);
                  }

                  JSONArray b(String s) {
                      return JSONArray.parseArray(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void parseThroughJSONObjectKeepsParsingArrays() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;

              class A {
                  Object a(String s) {
                      return JSONObject.parse(s);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;

              class A {
                  Object a(String s) {
                      return JSON.parse(s);
                  }
              }
              """
          )
        );
    }

    @Test
    void featureOverloadFastjson2JSONObjectDoesNotDeclare() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  JSONObject a(String s) {
                      return JSONObject.parseObject(s, Feature.SupportAutoType);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  JSONObject a(String s) {
                      return JSON.parseObject(s, Feature.SupportAutoType);
                  }
              }
              """
          )
        );
    }

    @Test
    void anotherJSONTypeIsImported() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "fastjson")
            .dependsOn("package org.example; public class JSON {}")),
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;
              import org.example.JSON;

              class A {
                  JSON json;

                  Object a(Object o) {
                      return JSONObject.toJSON(o);
                  }
              }
              """,
            """
              import org.example.JSON;

              class A {
                  JSON json;

                  Object a(Object o) {
                      return com.alibaba.fastjson.JSON.toJSON(o);
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void staticMethodsFastjson2Declares() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSONArray;
                  import com.alibaba.fastjson.JSONObject;
                  import com.alibaba.fastjson.serializer.SerializerFeature;

                  import java.util.List;

                  class A {
                      JSONObject a(String s) {
                          return JSONObject.parseObject(s);
                      }

                      String b(String s) {
                          return JSONObject.parseObject(s, String.class);
                      }

                      String c(Object o) {
                          return JSONObject.toJSONString(o, SerializerFeature.PrettyFormat);
                      }

                      List<String> d(String s) {
                          return JSONArray.parseArray(s, String.class);
                      }

                      String e(Object o) {
                          return JSONArray.toJSONString(o);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void callsOnJSON() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSON;

                  class A {
                      Object a(Object o) {
                          return JSON.toJSON(o);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void instanceMethods() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSONObject;

                  class A {
                      String a(JSONObject o) {
                          return o.toJSONString();
                      }
                  }
                  """
              )
            );
        }
    }
}
