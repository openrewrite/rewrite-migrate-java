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
import org.openrewrite.marker.GitProvenance;
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

    private static GitProvenance gitProvenance(String origin) {
        return new GitProvenance(randomId(), origin, "main", "abc123", null, null, null);
    }

    @DocumentExample
    @Test
    void multiModuleRepositoryCollapsesToOneRow() {
        // All modules in a multi-module repository share one GitProvenance, so the recipe
        // emits a single row identifying the repository as a whole.
        var git = gitProvenance("https://github.com/example/demo.git");
        var moduleA = new JavaProject(randomId(), "module-a", null);
        var moduleB = new JavaProject(randomId(), "module-b", null);
        var jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows ->
            assertThat(rows).containsExactly(
              new JavaVersionTable.Row("17", "8")
            )),
          //language=java
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(git, moduleA, jv)),
          //language=java
          java(
            """
              class B {
              }
              """,
            spec -> spec.markers(git, moduleB, jv))
        );
    }

    @Test
    void heterogeneousVersionsInOneRepoPickLowestTarget() {
        // When modules in the same repo target different JDKs, the row reports the lowest
        // target — the migration floor for the repository.
        var git = gitProvenance("https://github.com/example/mixed.git");
        var legacy = new JavaProject(randomId(), "legacy-module", null);
        var modern = new JavaProject(randomId(), "modern-module", null);
        var java8 = new JavaVersion(randomId(), "Sam", "Shelter", "8", "8");
        var java17 = new JavaVersion(randomId(), "Sam", "Shelter", "17", "17");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows ->
            assertThat(rows).containsExactly(
              new JavaVersionTable.Row("8", "8")
            )),
          //language=java
          java(
            """
              class Legacy {
              }
              """,
            spec -> spec.markers(git, legacy, java8)),
          //language=java
          java(
            """
              class Modern {
              }
              """,
            spec -> spec.markers(git, modern, java17))
        );
    }

    @Test
    void identicalJavaVersionMarkersAcrossRepositoriesAreEachReported() {
        // Reproduces customer-requests#2409: across multiple repositories with the same JDK,
        // every repository must contribute its own row in the data table. Previously a
        // recipe-instance HashSet deduplicated by JavaVersion content, so identical markers
        // in different repositories were silently dropped.
        var gitA = gitProvenance("https://github.com/example/repo-a.git");
        var gitB = gitProvenance("https://github.com/example/repo-b.git");
        var gitC = gitProvenance("https://github.com/example/repo-c.git");
        var project = new JavaProject(randomId(), "service", null);
        var jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows ->
            assertThat(rows).containsExactly(
              new JavaVersionTable.Row("17", "8"),
              new JavaVersionTable.Row("17", "8"),
              new JavaVersionTable.Row("17", "8")
            )),
          //language=java
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(gitA, project, jv)),
          //language=java
          java(
            """
              class B {
              }
              """,
            spec -> spec.markers(gitB, project, jv)),
          //language=java
          java(
            """
              class C {
              }
              """,
            spec -> spec.markers(gitC, project, jv))
        );
    }

    @Test
    void withoutGitProvenanceFallsBackToPerProject() {
        // When no GitProvenance is available (local non-git source trees, some test setups),
        // the recipe falls back to one row per JavaProject so distinct modules are not silently merged.
        var projectOne = new JavaProject(randomId(), "module-a", null);
        var projectTwo = new JavaProject(randomId(), "module-b", null);
        var jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows ->
            assertThat(rows).containsExactly(
              new JavaVersionTable.Row("17", "8"),
              new JavaVersionTable.Row("17", "8")
            )),
          //language=java
          java(
            """
              class A {
              }
              """,
            spec -> spec.markers(projectOne, jv)),
          //language=java
          java(
            """
              class B {
              }
              """,
            spec -> spec.markers(projectTwo, jv))
        );
    }
}
