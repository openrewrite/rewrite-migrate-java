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

class RemoveObsoleteFastjsonFeaturesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveObsoleteFastjsonFeatures())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void varargsArguments() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue);
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
                  }

                  String c(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.DisableCircularReferenceDetect);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.WriteMapNullValue);
                  }

                  String b(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.WriteMapNullValue);
                  }

                  String c(Object o) {
                      return JSON.toJSONString(o);
                  }
              }
              """
          )
        );
    }

    @Test
    void parserFeatures() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  JSONObject a(String s) {
                      return JSON.parseObject(s, Feature.OrderedField);
                  }

                  <T> T b(String s, Class<T> type) {
                      return JSON.parseObject(s, type, Feature.AllowSingleQuotes, Feature.SupportAutoType);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  JSONObject a(String s) {
                      return JSON.parseObject(s);
                  }

                  <T> T b(String s, Class<T> type) {
                      return JSON.parseObject(s, type, Feature.SupportAutoType);
                  }
              }
              """
          )
        );
    }

    @Test
    void multilineArgumentsKeepTheirLayout() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.serializer.SerializerFeature;
              import com.alibaba.fastjson.support.config.FastJsonConfig;

              class A {
                  void configure(FastJsonConfig config) {
                      config.setSerializerFeatures(
                              SerializerFeature.DisableCircularReferenceDetect,
                              SerializerFeature.PrettyFormat,
                              SerializerFeature.QuoteFieldNames,
                              SerializerFeature.WriteMapNullValue);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.serializer.SerializerFeature;
              import com.alibaba.fastjson.support.config.FastJsonConfig;

              class A {
                  void configure(FastJsonConfig config) {
                      config.setSerializerFeatures(
                              SerializerFeature.PrettyFormat,
                              SerializerFeature.WriteMapNullValue);
                  }
              }
              """
          )
        );
    }

    @Test
    void removesImportOnceUnused() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.SortField);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticallyImportedFeature() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;

              import static com.alibaba.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect;
              import static com.alibaba.fastjson.serializer.SerializerFeature.PrettyFormat;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, DisableCircularReferenceDetect, PrettyFormat);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;

              import static com.alibaba.fastjson.serializer.SerializerFeature.PrettyFormat;

              class A {
                  String a(Object o) {
                      return JSON.toJSONString(o, PrettyFormat);
                  }
              }
              """
          )
        );
    }

    @Test
    void arrayInitializers() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  SerializerFeature[] features = {SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect};
                  SerializerFeature[] none = new SerializerFeature[]{SerializerFeature.QuoteFieldNames};
              }
              """,
            """
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  SerializerFeature[] features = {SerializerFeature.WriteMapNullValue};
                  SerializerFeature[] none = new SerializerFeature[]{};
              }
              """
          )
        );
    }

    @Test
    void annotationAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.annotation.JSONField;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  @JSONField(name = "a", serialzeFeatures = SerializerFeature.DisableCircularReferenceDetect)
                  String a;

                  @JSONField(serialzeFeatures = {SerializerFeature.WriteDateUseDateFormat})
                  java.util.Date b;

                  @JSONField(serialzeFeatures = {SerializerFeature.WriteMapNullValue, SerializerFeature.QuoteFieldNames}, ordinal = 3)
                  String c;
              }
              """,
            """
              import com.alibaba.fastjson.annotation.JSONField;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  @JSONField(name = "a")
                  String a;

                  @JSONField
                  java.util.Date b;

                  @JSONField(serialzeFeatures = {SerializerFeature.WriteMapNullValue}, ordinal = 3)
                  String c;
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void featuresFastjson2Has() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSON;
                  import com.alibaba.fastjson.serializer.SerializerFeature;

                  class A {
                      String a(Object o) {
                          return JSON.toJSONString(o, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void featureOutsideOfFeatureVarargs() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.serializer.SerializerFeature;

                  class A {
                      SerializerFeature f = SerializerFeature.DisableCircularReferenceDetect;

                      void log(Object... args) {
                      }

                      void a() {
                          log("{}", SerializerFeature.QuoteFieldNames);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void emptyArrayOfAnotherAnnotation() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.serializer.SerializerFeature;

                  @SuppressWarnings({})
                  class A {
                      SerializerFeature f = SerializerFeature.PrettyFormat;
                  }
                  """
              )
            );
        }
    }
}
