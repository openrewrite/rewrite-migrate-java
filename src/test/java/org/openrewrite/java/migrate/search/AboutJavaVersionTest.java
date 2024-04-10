/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionRow;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class AboutJavaVersionTest implements RewriteTest {

    @DocumentExample
    @Test
    void aboutJavaVersion() {
        JavaVersion jv = new JavaVersion(randomId(), "me", "me", "11.0.15+10", "11.0.15+10");
        rewriteRun(
          spec -> spec.recipe(new AboutJavaVersion(null))
            .dataTable(JavaVersionRow.class, rows ->
              assertThat(rows).containsExactly(new JavaVersionRow("", "", jv.getCreatedBy(),
                jv.getCreatedBy(), jv.getSourceCompatibility(), Integer.toString(jv.getMajorVersion()), jv.getTargetCompatibility()))),
          java(
            //language=java
            """
              class Test {
              }
              """,
            //language=java
            """
              /*~~(Java version: 11)~~>*/class Test {
              }
              """,
            spec -> spec.markers(jv)
          ),
          java(
            //language=java
            """
              class Test2 {
              }
              """,
            //language=java
            """
              /*~~(Java version: 11)~~>*/class Test2 {
              }
              """,
            spec -> spec.markers(jv.withId(randomId()))
          )
        );
    }
}
