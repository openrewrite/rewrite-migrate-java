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

class ReplaceFastjsonSqlDateGettersTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceFastjsonSqlDateGetters())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void jsonObjectGetters() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONObject;

              import java.sql.Timestamp;

              class A {
                  Timestamp created(JSONObject o) {
                      return o.getTimestamp("created");
                  }

                  java.sql.Date day(JSONObject o) {
                      return o.getSqlDate("day");
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSONObject;

              import java.sql.Date;
              import java.sql.Timestamp;

              class A {
                  Timestamp created(JSONObject o) {
                      return o.getObject("created", Timestamp.class);
                  }

                  Date day(JSONObject o) {
                      return o.getObject("day", Date.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void jsonArrayGetters() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSONArray;

              import java.util.Date;

              class A {
                  Date first(JSONArray a) {
                      return a.getSqlDate(0);
                  }

                  Date second(JSONArray a) {
                      return a.getTimestamp(1);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSONArray;

              import java.sql.Timestamp;
              import java.util.Date;

              class A {
                  Date first(JSONArray a) {
                      return a.getObject(0, java.sql.Date.class);
                  }

                  Date second(JSONArray a) {
                      return a.getObject(1, Timestamp.class);
                  }
              }
              """
          )
        );
    }
}
