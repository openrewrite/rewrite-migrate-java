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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateFastjsonForJakarta10Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.MigrateFastjsonForJakarta10")
          .parser(JavaParser.fromJavaVersion().classpath("fastjson"));
    }

    @DocumentExample
    @Test
    void serializerFeatureBecomesWriterFeature() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String toJson(Object o) {
                      return JSON.toJSONString(o, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue);
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.JSONWriter;
              import com.alibaba.fastjson2.JSONWriter.Feature;

              class A {
                  String toJson(Object o) {
                      return JSON.toJSONString(o, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteMapNullValue);
                  }
              }
              """
          )
        );
    }

    @Test
    void parserFeatureBecomesReaderFeature() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;

              class A {
                  <T> T fromJson(String json, Class<T> type) {
                      return JSON.parseObject(json, type, Feature.AllowUnQuotedFieldNames);
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.JSONReader;
              import com.alibaba.fastjson2.JSONReader.Feature;

              class A {
                  <T> T fromJson(String json, Class<T> type) {
                      return JSON.parseObject(json, type, JSONReader.Feature.AllowUnQuotedFieldNames);
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("`ChangeType` to a nested type also adds an unused `import JSONReader.Feature`, which clashes with `JSONWriter.Feature`")
    @Test
    void readerAndWriterFeatureInOneFile() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.parser.Feature;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              class A {
                  String roundTrip(String json) {
                      Object o = JSON.parseObject(json, Object.class, Feature.AllowUnQuotedFieldNames);
                      return JSON.toJSONString(o, SerializerFeature.PrettyFormat);
                  }
              }
              """,
            """
              import com.alibaba.fastjson2.JSON;
              import com.alibaba.fastjson2.JSONReader;
              import com.alibaba.fastjson2.JSONWriter;

              class A {
                  String roundTrip(String json) {
                      Object o = JSON.parseObject(json, Object.class, JSONReader.Feature.AllowUnQuotedFieldNames);
                      return JSON.toJSONString(o, JSONWriter.Feature.PrettyFormat);
                  }
              }
              """
          )
        );
    }
}
