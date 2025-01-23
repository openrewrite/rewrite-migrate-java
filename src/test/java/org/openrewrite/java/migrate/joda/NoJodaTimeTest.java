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
package org.openrewrite.java.migrate.joda;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class NoJodaTimeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/no-joda-time.yml", "org.openrewrite.java.migrate.joda.NoJodaTime")
          .parser(JavaParser.fromJavaVersion().classpath("joda-time", "threeten-extra"));
    }

    @DocumentExample
    @Test
    void migrateJodaTime() {
        rewriteRun(
          mavenProject("foo",
            srcMainJava(
              // language=java
              java(
                """
                  import org.joda.time.DateTime;
                  import org.joda.time.Interval;

                  class A {
                      void foo() {
                          DateTime dt = new DateTime();
                          DateTime dt1 = new DateTime().plusDays(1);
                          Interval i = new Interval(dt, dt1);
                          System.out.println(i.toDuration());
                      }
                  }
                  """,
                """
                  import org.threeten.extra.Interval;

                  import java.time.ZonedDateTime;

                  class A {
                      void foo() {
                          ZonedDateTime dt = ZonedDateTime.now();
                          ZonedDateTime dt1 = ZonedDateTime.now().plusDays(1);
                          Interval i = Interval.of(dt.toInstant(), dt1.toInstant());
                          System.out.println(i.toDuration());
                      }
                  }
                  """
              ),
              //language=xml
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example.foobar</groupId>
                      <artifactId>foobar-core</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>joda-time</groupId>
                              <artifactId>joda-time</artifactId>
                              <version>2.12.3</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example.foobar</groupId>
                      <artifactId>foobar-core</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>joda-time</groupId>
                              <artifactId>joda-time</artifactId>
                              <version>2.12.3</version>
                          </dependency>
                          <dependency>
                              <groupId>org.threeten</groupId>
                              <artifactId>threeten-extra</artifactId>
                              <version>1.8.0</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }
}
