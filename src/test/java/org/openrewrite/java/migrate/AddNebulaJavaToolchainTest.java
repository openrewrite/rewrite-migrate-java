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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.kotlin.Assertions.kotlin;

class AddNebulaJavaToolchainTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeBuildToJava25");
    }

    private static GradleProject nebula(String id) {
        return GradleProject.builder().id(Tree.randomId())
          .plugins(Collections.singletonList(new GradlePluginDescriptor("nebula.plugin.netflixossproject.NetflixOssProjectPlugin", id)))
          .build();
    }

    @Test
    void overrideImplicitJava8() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              """,
            """
              plugins { id 'java' }

              java {
                  toolchain {
                      languageVersion = JavaLanguageVersion.of(25)
                  }
              }
              """,
            spec -> spec.markers(nebula("nebula.netflixoss"))
          )
        );
    }

    @Test
    void overrideImplicitJava8InKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins { java }
              """,
            """
              plugins { java }

              java {
                  toolchain {
                      languageVersion.set(JavaLanguageVersion.of(25))
                  }
              }
              """,
            spec -> spec.markers(nebula("com.netflix.nebula.netflixoss"))
          )
        );
    }

    @Test
    void preserveExplicitConfiguration() {
        rewriteRun(
          buildGradle(
            """
              plugins { id 'java' }
              java {
                  toolchain { languageVersion = JavaLanguageVersion.of(26) }
              }
              """,
            spec -> spec.markers(nebula("nebula.netflixoss"))
          ),
          buildGradle(
            """
              plugins { id 'java' }
              java {
                  sourceCompatibility = 26
                  targetCompatibility = 26
              }
              """,
            spec -> spec.path("explicit/build.gradle").markers(nebula("nebula.netflixoss"))
          )
        );
    }

    @Test
    void leaveKotlinModulesToTheirVersionGatedMigration() {
        JavaProject module = new JavaProject(Tree.randomId(), "kotlin-module", null);
        rewriteRun(
          buildGradle(
            "plugins { id 'java' }",
            spec -> spec.markers(module, nebula("nebula.netflixoss"))
          ),
          kotlin("class Example", spec -> spec.markers(module))
        );
    }

    @Test
    void leaveOtherBuildsAlone() {
        rewriteRun(
          buildGradle("plugins { id 'java' }"),
          buildGradle(
            "plugins { id 'base' }",
            spec -> spec.path("base/build.gradle").markers(GradleProject.builder().id(Tree.randomId())
              .plugins(Collections.singletonList(new GradlePluginDescriptor("org.gradle.api.plugins.BasePlugin", "base"))).build())
          )
        );
    }
}
