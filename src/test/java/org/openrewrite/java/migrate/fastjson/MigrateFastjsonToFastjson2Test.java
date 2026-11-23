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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;

class MigrateFastjsonToFastjson2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.fastjson.MigrateFastjsonToFastjson2")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void migrateCommonApis() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.JSONArray;
              import com.alibaba.fastjson.JSONObject;
              import com.alibaba.fastjson.TypeReference;
              import com.alibaba.fastjson.parser.Feature;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.util.List;
              import java.util.Map;

              class A {
                  String write(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect);
                  }

                  String pretty(Object o) {
                      return JSONObject.toJSONString(o, true);
                  }

                  String dated(Object o) {
                      return JSON.toJSONStringWithDateFormat(o, "yyyy-MM-dd", SerializerFeature.WriteDateUseDateFormat);
                  }

                  List<String> list(String s) {
                      return JSONObject.parseArray(s, String.class);
                  }

                  Map<String, Integer> map(String s) {
                      return JSON.parseObject(s, new TypeReference<Map<String, Integer>>() {}, Feature.OrderedField);
                  }

                  String name(String s) {
                      JSONObject o = JSON.parseObject(s);
                      JSONArray items = o.getJSONArray("items");
                      return items.getJSONObject(0).getString("name");
                  }

                  JSONObject ordered() {
                      return new JSONObject(true);
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.JSONArray;
              import com.alibaba.fastjson2.JSONObject;
              import com.alibaba.fastjson2.TypeReference;
              import com.alibaba.fastjson2.JSONWriter;

              import java.util.List;
              import java.util.Map;

              class A {
                  String write(Object o) {
                      return JSON.toJSONString(o, JSONWriter.Feature.WriteMapNullValue);
                  }

                  String pretty(Object o) {
                      return JSONObject.toJSONString(o, JSONWriter.Feature.PrettyFormat);
                  }

                  String dated(Object o) {
                      return JSON.toJSONString(o, "yyyy-MM-dd");
                  }

                  List<String> list(String s) {
                      return JSON.parseArray(s, String.class);
                  }

                  Map<String, Integer> map(String s) {
                      return JSON.parseObject(s, new TypeReference<Map<String, Integer>>() {});
                  }

                  String name(String s) {
                      JSONObject o = JSON.parseObject(s);
                      JSONArray items = o.getJSONArray("items");
                      return items.getJSONObject(0).getString("name");
                  }

                  JSONObject ordered() {
                      return new JSONObject();
                  }
              }
              """
          )
        );
    }

    @Test
    void annotations() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.annotation.JSONField;
              import com.alibaba.fastjson.annotation.JSONType;
              import com.alibaba.fastjson.parser.Feature;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.util.Date;

              @JSONType(orders = {"name", "created"}, serialzeFeatures = SerializerFeature.WriteMapNullValue)
              class User {
                  @JSONField(name = "user_name", serialzeFeatures = SerializerFeature.WriteNullStringAsEmpty)
                  String name;

                  @JSONField(format = "yyyy-MM-dd", serialzeFeatures = SerializerFeature.WriteDateUseDateFormat)
                  Date created;

                  @JSONField(parseFeatures = Feature.SupportAutoType)
                  Object payload;
              }
              """,
            """
              import com.alibaba.fastjson2.annotation.JSONField;
              import com.alibaba.fastjson2.annotation.JSONType;
              import com.alibaba.fastjson2.JSONReader;
              import com.alibaba.fastjson2.JSONWriter;

              import java.util.Date;

              @JSONType(orders = {"name", "created"}, serializeFeatures = JSONWriter.Feature.WriteMapNullValue)
              class User {
                  @JSONField(name = "user_name", serializeFeatures = JSONWriter.Feature.WriteNullStringAsEmpty)
                  String name;

                  @JSONField(format = "yyyy-MM-dd")
                  Date created;

                  @JSONField(deserializeFeatures = JSONReader.Feature.SupportAutoType)
                  Object payload;
              }
              """
          )
        );
    }

    @Test
    void globalConfiguration() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.ParserConfig;
              import com.alibaba.fastjson.serializer.SerializeConfig;
              import com.alibaba.fastjson.serializer.SerializerFeature;
              import com.alibaba.fastjson.serializer.ToStringSerializer;

              class A {
                  static {
                      JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
                      JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteMapNullValue.getMask();
                      ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
                      ParserConfig.getGlobalInstance().addAccept("com.example.");
                      SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
                      serializeConfig.put(Long.class, ToStringSerializer.instance);
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.JSONFactory;
              import com.alibaba.fastjson2.JSONReader;
              import com.alibaba.fastjson2.JSONWriter;
              import com.alibaba.fastjson2.writer.ObjectWriterImplToString;
              import com.alibaba.fastjson2.writer.ObjectWriterProvider;

              class A {
                  static {
                      JSON.config(JSONWriter.Feature.WriteMapNullValue);
                      JSON.config(JSONReader.Feature.SupportAutoType, true);
                      JSONFactory.getDefaultObjectReaderProvider().addAutoTypeAccept("com.example.");
                      ObjectWriterProvider serializeConfig = JSONFactory.getDefaultObjectWriterProvider();
                      serializeConfig.register(Long.class, ObjectWriterImplToString.INSTANCE);
                  }
              }
              """
          )
        );
    }

    @Test
    void valueFilterImplementation() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.ValueFilter;

              class A {
                  String write(Object o) {
                      return JSON.toJSONString(o, new ValueFilter() {
                          @Override
                          public Object process(Object object, String name, Object value) {
                              return value == null ? "" : value;
                          }
                      });
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.filter.ValueFilter;

              class A {
                  String write(Object o) {
                      return JSON.toJSONString(o, new ValueFilter() {
                          @Override
                          public Object apply(Object object, String name, Object value) {
                              return value == null ? "" : value;
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void mavenDependency() {
        rewriteRun(
          mavenProject("app",
            //language=java
            srcMainJava(
              java(
                """
                  import com.alibaba.fastjson.JSON;

                  class A {
                      String write(Object o) {
                          return JSON.toJSONString(o);
                      }
                  }
                  """,
                """
                  import com.alibaba.fastjson2.JSON;

                  class A {
                      String write(Object o) {
                          return JSON.toJSONString(o);
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.alibaba</groupId>
                            <artifactId>fastjson</artifactId>
                            <version>1.2.83</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  Matcher version = Pattern.compile("<groupId>com.alibaba.fastjson2</groupId>\\s*<artifactId>fastjson2</artifactId>\\s*<version>(2\\.0\\.\\d+)</version>").matcher(pom);
                  assertThat(version.find()).describedAs(pom).isTrue();
                  assertThat(pom).doesNotContain("<artifactId>fastjson</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void springSupportOnSpring5() {
        rewriteRun(
          mavenProject("app",
            //language=java
            srcMainJava(
              java(
                """
                  import com.alibaba.fastjson.serializer.SerializerFeature;
                  import com.alibaba.fastjson.support.config.FastJsonConfig;
                  import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

                  class WebConfig {
                      FastJsonHttpMessageConverter converter() {
                          return new FastJsonHttpMessageConverter();
                      }

                      FastJsonConfig config() {
                          FastJsonConfig config = new FastJsonConfig();
                          config.setSerializerFeatures(SerializerFeature.WriteMapNullValue, SerializerFeature.DisableCircularReferenceDetect);
                          return config;
                      }
                  }
                  """,
                """
                  import com.alibaba.fastjson2.support.config.FastJsonConfig;
                  import com.alibaba.fastjson2.JSONWriter;
                  import com.alibaba.fastjson2.support.spring.http.converter.FastJsonHttpMessageConverter;

                  class WebConfig {
                      FastJsonHttpMessageConverter converter() {
                          return new FastJsonHttpMessageConverter();
                      }

                      FastJsonConfig config() {
                          FastJsonConfig config = new FastJsonConfig();
                          config.setWriterFeatures(JSONWriter.Feature.WriteMapNullValue);
                          return config;
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.alibaba</groupId>
                            <artifactId>fastjson</artifactId>
                            <version>1.2.83</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-web</artifactId>
                            <version>5.3.39</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<artifactId>fastjson2-extension-spring5</artifactId>")
                    .doesNotContain("<artifactId>fastjson2-extension-spring6</artifactId>")
                    .doesNotContain("<artifactId>fastjson</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void springSupportOnSpring6() {
        rewriteRun(
          mavenProject("app",
            //language=java
            srcMainJava(
              java(
                """
                  import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

                  class WebConfig {
                      FastJsonHttpMessageConverter converter() {
                          return new FastJsonHttpMessageConverter();
                      }
                  }
                  """,
                """
                  import com.alibaba.fastjson2.support.spring6.http.converter.FastJsonHttpMessageConverter;

                  class WebConfig {
                      FastJsonHttpMessageConverter converter() {
                          return new FastJsonHttpMessageConverter();
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.alibaba</groupId>
                            <artifactId>fastjson</artifactId>
                            <version>1.2.83</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-web</artifactId>
                            <version>6.1.13</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<artifactId>fastjson2-extension-spring6</artifactId>")
                    .doesNotContain("<artifactId>fastjson2-extension-spring5</artifactId>")
                    .doesNotContain("<artifactId>fastjson</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void gradleDependency() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'com.alibaba:fastjson:1.2.83'
              }
              """,
            spec -> spec.after(gradle -> {
                assertThat(gradle).containsPattern("implementation 'com.alibaba.fastjson2:fastjson2:2\\.0\\.\\d+'");
                return gradle;
            })
          )
        );
    }

    @Test
    void dependencyOfAModuleThatGetsFastjsonFromAnotherDependency() {
        rewriteRun(
          mavenProject("app",
            //language=java
            srcTestJava(
              java(
                """
                  import com.alibaba.fastjson.JSON;

                  class ATest {
                      String write(Object o) {
                          return JSON.toJSONString(o);
                      }
                  }
                  """,
                """
                  import com.alibaba.fastjson2.JSON;

                  class ATest {
                      String write(Object o) {
                          return JSON.toJSONString(o);
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .containsPattern("<artifactId>fastjson2</artifactId>\\s*<version>2\\.0\\.\\d+</version>\\s*<scope>test</scope>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void javaVersionBelowWhatFastjson2Requires() {
        rewriteRun(
          mavenProject("app",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <properties>
                        <java.version>1.7</java.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.alibaba</groupId>
                            <artifactId>fastjson</artifactId>
                            <version>1.2.83</version>
                        </dependency>
                    </dependencies>
                    <profiles>
                        <profile>
                            <id>default</id>
                            <build>
                                <plugins>
                                    <plugin>
                                        <artifactId>maven-compiler-plugin</artifactId>
                                        <configuration>
                                            <source>1.6</source>
                                            <target>1.6</target>
                                        </configuration>
                                    </plugin>
                                </plugins>
                            </build>
                        </profile>
                    </profiles>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<java.version>1.8</java.version>")
                    .contains("<source>1.8</source>")
                    .contains("<target>1.8</target>")
                    .contains("<artifactId>fastjson2</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void javaVersionOfJava8IsKept() {
        rewriteRun(
          mavenProject("app",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <properties>
                        <!-- Build args -->
                        <maven.compiler.source>1.8</maven.compiler.source>
                        <maven.compiler.target>1.8</maven.compiler.target>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.alibaba</groupId>
                            <artifactId>fastjson</artifactId>
                            <version>1.2.83</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <source>${maven.compiler.source}</source>
                                    <target>${maven.compiler.target}</target>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .contains("<!-- Build args -->")
                    .contains("<maven.compiler.source>1.8</maven.compiler.source>")
                    .contains("<source>${maven.compiler.source}</source>")
                    .doesNotContain("<release>")
                    .contains("<artifactId>fastjson2</artifactId>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void gradleJavaVersionBelowWhatFastjson2Requires() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("app",
            //language=groovy
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }
                repositories {
                    mavenCentral()
                }
                java {
                    sourceCompatibility = JavaVersion.VERSION_1_7
                    targetCompatibility = JavaVersion.VERSION_1_7
                }
                dependencies {
                    implementation 'com.alibaba:fastjson:1.2.83'
                }
                """,
              spec -> spec.after(gradle -> {
                  assertThat(gradle)
                    .doesNotContain("VERSION_1_7")
                    .contains("sourceCompatibility = JavaVersion.VERSION_1_8")
                    .contains("targetCompatibility = JavaVersion.VERSION_1_8")
                    .containsPattern("implementation 'com.alibaba.fastjson2:fastjson2:2\\.0\\.\\d+'");
                  return gradle;
              })
            )
          )
        );
    }

    @Test
    void javaVersionOfARepositoryWithoutFastjsonIsKept() {
        rewriteRun(
          mavenProject("app",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.source>1.7</maven.compiler.source>
                        <maven.compiler.target>1.7</maven.compiler.target>
                    </properties>
                </project>
                """
            )
          )
        );
    }
}
