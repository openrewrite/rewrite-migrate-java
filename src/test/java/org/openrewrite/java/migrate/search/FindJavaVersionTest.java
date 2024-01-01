/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

public class FindJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindJavaVersion());
    }

    @Test
    void test() {
        JavaVersion jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new JavaVersionTable.Row("17", "8")
              );
          }),
          //language=java
          java(
                """
            class A {
            }
            """,
            spec -> spec.markers(jv)),
          //language=java
          java(
                """
            class B {
            }
            """,
            spec -> spec.markers(jv))
        );
    }
}
