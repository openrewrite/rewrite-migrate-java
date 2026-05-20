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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

class FindJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindJavaVersion());
    }

    @DocumentExample
    @Test
    void twoClassesInSameProjectLeadToTwoRows() {
        var project = new JavaProject(randomId(), "demo", null);
        var jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new JavaVersionTable.Row("demo", "17", "8"),
                new JavaVersionTable.Row("demo", "17", "8")
              );
          }),
          //language=java
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(project, jv)),
          //language=java
          java(
            """
              class B {
              }
              """,
            spec -> spec.markers(project, jv))
        );
    }

    @Test
    void identicalJavaVersionMarkersAcrossProjectsAreEachReported() {
        // Reproduces customer-requests#2409: across multiple projects with the same JDK,
        // every project must appear as its own row in the data table. Previously a
        // recipe-instance HashSet deduplicated by JavaVersion content, so identical
        // markers in different projects were silently dropped.
        var projectA = new JavaProject(randomId(), "project-a", null);
        var projectB = new JavaProject(randomId(), "project-b", null);
        var projectC = new JavaProject(randomId(), "project-c", null);
        var jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows -> {
              assertThat(rows).containsExactlyInAnyOrder(
                new JavaVersionTable.Row("project-a", "17", "8"),
                new JavaVersionTable.Row("project-b", "17", "8"),
                new JavaVersionTable.Row("project-c", "17", "8")
              );
          }),
          //language=java
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(projectA, jv)),
          //language=java
          java(
            """
              class B {
              }
              """,
            spec -> spec.markers(projectB, jv)),
          //language=java
          java(
            """
              class C {
              }
              """,
            spec -> spec.markers(projectC, jv))
        );
    }
}
