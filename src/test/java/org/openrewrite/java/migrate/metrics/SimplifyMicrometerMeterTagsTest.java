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
package org.openrewrite.java.migrate.metrics;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SimplifyMicrometerMeterTagsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new SimplifyMicrometerMeterTags())
          .parser(JavaParser.fromJavaVersion().classpath("micrometer-core"));
    }

    @DocumentExample
    @Test
    void simplifyNewArray() {
        //language=java
        rewriteRun(
          java(
            """
              import io.micrometer.core.instrument.*;
              class Test {
                  Counter c = Counter.builder("counter")
                      .tags(new String[] { "key", "value" })
                      .register(Metrics.globalRegistry);
              }
              """,
            """
              import io.micrometer.core.instrument.*;
              class Test {
                  Counter c = Counter.builder("counter")
                      .tag("key", "value")
                      .register(Metrics.globalRegistry);
              }
              """
          )
        );
    }

    @Test
    void simplifyExistingArray() {
        //language=java
        rewriteRun(
          java(
            """
              import io.micrometer.core.instrument.*;
              class Test {
                  String[] tags = new String[] { "key", "value" };
                  Counter c = Counter.builder("counter")
                      .tags(tags)
                      .register(Metrics.globalRegistry);
              }
              """,
            """
              import io.micrometer.core.instrument.*;
              class Test {
                  String[] tags = new String[] { "key", "value" };
                  Counter c = Counter.builder("counter")
                      .tag(tags[0], tags[1])
                      .register(Metrics.globalRegistry);
              }
              """
          )
        );
    }
}
