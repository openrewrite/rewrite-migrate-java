/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ExplicitRecordImportTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExplicitRecordImport())
          //language=java
          .parser(JavaParser.fromJavaVersion().dependsOn("""
                package com.acme.music;
                public class Record {
                    public String name;
                }
                """,
              """
                package com.acme.music;
                import java.util.List;
                public class RecordList {
                    public List<Record> records;
                }
                """
            )
          );
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/540")
    @Test
    void addImportFromSamePackage() {
        rewriteRun(
          //language=java
          java(
            """
              package com.acme.music;

              class Test {
                  Record record;
              }
              """,
            """
              package com.acme.music;

              import com.acme.music.Record;

              class Test {
                  Record record;
              }
              """
          )
        );
    }

    @Test
    void noChangeIfUsingVar() {
        rewriteRun(
          //language=java
          java(
            """
              package com.acme.music;

              import com.acme.music.RecordList;

              class Test {
                  void test() {
                      for (var record : new RecordList().records) {
                      }
                  }
              }
              """,
            s -> s.markers(javaVersion(17))
          )
        );
    }

    @Test
    void noChangeIfAlreadyFullyQualified() {
        rewriteRun(
          //language=java
          java(
            """
              package com.acme.music;

              class Test {
                  com.acme.music.Record record;
              }
              """,
            // Perhaps undesired, but also unlikely, so not worth changing
            """
              package com.acme.music;

              import com.acme.music.Record;

              class Test {
                  Record record;
              }
              """
          )
        );
    }


    @Test
    void noChangeIfAlreadyImported() {
        rewriteRun(
          //language=java
          java(
            """
              package com.acme.music;

              import com.acme.music.Record;

              class Test {
                  Record record;
              }
              """
          )
        );
    }

    @Test
    void noImportAddedForJavaLangRecord() {
        rewriteRun(
          //language=java
          java(
            """
              package foo.bar;

              class Test {
                  Record record;
              }
              """
          )
        );
    }
}
