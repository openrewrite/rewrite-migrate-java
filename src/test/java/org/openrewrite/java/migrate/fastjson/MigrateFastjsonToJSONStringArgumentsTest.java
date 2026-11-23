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

class MigrateFastjsonToJSONStringArgumentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateFastjsonToJSONStringArguments())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void prettyFormatFlag() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, true);
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o, false);
                  }

                  String c(Object o, boolean pretty) {
                      return JSON.toJSONString(o, pretty);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.PrettyFormat);
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o);
                  }

                  String c(Object o, boolean pretty) {
                      return JSON.toJSONString(o, pretty ? new SerializerFeature[]{SerializerFeature.PrettyFormat} : new SerializerFeature[0]);
                  }
              }
              """
          )
        );
    }

    @Test
    void prettyFormatFlagThroughJSONObject() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;

              class A {
                  String a(Object o) {
                      return JSONObject.toJSONString(o, true);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSONObject.toJSONString(o, SerializerFeature.PrettyFormat);
                  }
              }
              """
          )
        );
    }

    @Test
    void serializeConfigBecomesWriterContext() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializeConfig;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  SerializeConfig config = new SerializeConfig();

                  String a(Object o) {
                      return JSON.toJSONString(o, config, SerializerFeature.WriteMapNullValue);
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o, config);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializeConfig;
              import com.alibaba.fastjson.serializer.SerializerFeature;
              import com.alibaba.fastjson2.JSONWriter;

              class A {
                  SerializeConfig config = new SerializeConfig();

                  String a(Object o) {
                      return JSON.toJSONString(o, new JSONWriter.Context(config, SerializerFeature.WriteMapNullValue));
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o, new JSONWriter.Context(config));
                  }
              }
              """
          )
        );
    }

    @Test
    void iso8601DateFormat() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.UseISO8601DateFormat, SerializerFeature.WriteMapNullValue);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONStringWithDateFormat(o, "iso8601", SerializerFeature.WriteMapNullValue);
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultFeaturesMask() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import static com.alibaba.fastjson.serializer.SerializerFeature.WRITE_MAP_NULL_FEATURES;

              class A {
                  String defaults(Object o) {
                      return JSON.toJSONString(o, JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.PrettyFormat);
                  }

                  String nulls(Object o) {
                      return JSON.toJSONString(o, WRITE_MAP_NULL_FEATURES, SerializerFeature.PrettyFormat);
                  }

                  String mask(Object o, int features) {
                      return JSON.toJSONString(o, features, SerializerFeature.PrettyFormat);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String defaults(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.PrettyFormat);
                  }

                  String nulls(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullBooleanAsFalse, SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullNumberAsZero, SerializerFeature.WriteNullStringAsEmpty, SerializerFeature.PrettyFormat);
                  }

                  String mask(Object o, int features) {
                      return JSON.toJSONString(o, features, SerializerFeature.PrettyFormat);
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeToOtherOverloads() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.PrettyFormat);
                  }
              }
              """
          )
        );
    }
}
