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

class MigrateFastjsonGlobalConfigurationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateFastjsonGlobalConfiguration())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void defaultGenerateFeature() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  static {
                      JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteMapNullValue.getMask();
                      JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
                      JSON.DEFAULT_GENERATE_FEATURE &= ~SerializerFeature.WriteNullListAsEmpty.getMask();
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  static {
                      JSON.config(SerializerFeature.WriteMapNullValue);
                      JSON.config(SerializerFeature.WriteNullListAsEmpty, false);
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultParserFeature() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  void configure() {
                      JSON.DEFAULT_PARSER_FEATURE |= Feature.SupportAutoType.getMask();
                      JSON.DEFAULT_PARSER_FEATURE |= Feature.OrderedField.getMask();
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  void configure() {
                      JSON.config(Feature.SupportAutoType);
                  }
              }
              """
          )
        );
    }

    @Test
    void featureConfigAssignedToTheDefaults() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  void configure(boolean nulls) {
                      JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.WriteMapNullValue, true);
                      JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.WriteNullListAsEmpty, nulls);
                      JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.SortField, true);
                      JSON.DEFAULT_PARSER_FEATURE = Feature.config(JSON.DEFAULT_PARSER_FEATURE, Feature.SupportAutoType, false);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  void configure(boolean nulls) {
                      JSON.config(SerializerFeature.WriteMapNullValue);
                      JSON.config(SerializerFeature.WriteNullListAsEmpty, nulls);
                      JSON.config(Feature.SupportAutoType, false);
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultDateFormat() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;

              class A {
                  void configure() {
                      JSON.DEFFAULT_DATE_FORMAT = "yyyy-MM-dd";
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;

              class A {
                  void configure() {
                      JSON.configWriterDateFormat("yyyy-MM-dd");
                  }
              }
              """
          )
        );
    }

    @Test
    void autoTypeSupport() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.parser.ParserConfig;

              class A {
                  void configure() {
                      ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  void configure() {
                      JSON.config(Feature.SupportAutoType, true);
                  }
              }
              """
          )
        );
    }

    @Test
    void globalSerializeConfigOfFastJsonConfig() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.serializer.SerializeConfig;
              import com.alibaba.fastjson.serializer.ToStringSerializer;
              import com.alibaba.fastjson.support.config.FastJsonConfig;

              class A {
                  FastJsonConfig config() {
                      FastJsonConfig fastJsonConfig = new FastJsonConfig();
                      SerializeConfig serializeConfig = SerializeConfig.globalInstance;
                      serializeConfig.put(Long.class, ToStringSerializer.instance);
                      fastJsonConfig.setSerializeConfig(serializeConfig);
                      return fastJsonConfig;
                  }
              }
              """,
            """
              import com.alibaba.fastjson.serializer.SerializeConfig;
              import com.alibaba.fastjson.serializer.ToStringSerializer;
              import com.alibaba.fastjson.support.config.FastJsonConfig;

              class A {
                  FastJsonConfig config() {
                      FastJsonConfig fastJsonConfig = new FastJsonConfig();
                      SerializeConfig serializeConfig = SerializeConfig.globalInstance;
                      serializeConfig.put(Long.class, ToStringSerializer.instance);
                      return fastJsonConfig;
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {

        @Test
        void ownSerializeConfigOfFastJsonConfig() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.serializer.SerializeConfig;
                  import com.alibaba.fastjson.support.config.FastJsonConfig;

                  class A {
                      void configure(FastJsonConfig fastJsonConfig) {
                          SerializeConfig serializeConfig = new SerializeConfig();
                          fastJsonConfig.setSerializeConfig(serializeConfig);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void disablingAFeatureFastjson2DoesNotHave() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSON;
                  import com.alibaba.fastjson.parser.Feature;

                  class A {
                      void configure() {
                          JSON.DEFAULT_PARSER_FEATURE &= ~Feature.UseBigDecimal.getMask();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void featureConfigOfAnotherMask() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.JSON;
                  import com.alibaba.fastjson.serializer.SerializerFeature;

                  class A {
                      int features = 0;

                      void configure() {
                          JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(features, SerializerFeature.WriteMapNullValue, true);
                          JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, SerializerFeature.SkipTransientField, false);
                      }
                  }
                  """
              )
            );
        }

        @Test
        void autoTypeSupportOfOwnParserConfig() {
            rewriteRun(
              //language=java
              java(
                """
                  import com.alibaba.fastjson.parser.ParserConfig;

                  class A {
                      void configure() {
                          ParserConfig config = new ParserConfig();
                          config.setAutoTypeSupport(true);
                      }
                  }
                  """
              )
            );
        }
    }
}
