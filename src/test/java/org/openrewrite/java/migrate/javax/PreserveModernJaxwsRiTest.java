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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.maven.Assertions.pomXml;

class PreserveModernJaxwsRiTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-11.yml",
          "org.openrewrite.java.migrate.javax.AddJaxwsDependencies");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.3.2", "3.0.0", "4.0.5"})
    void removeOnlyLegacyGradleDeclarations(String version) {
        ResolvedDependency dependency = ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, "com.sun.xml.ws", "jaxws-ri", version, null))
          .requested(Dependency.builder().gav(new GroupArtifactVersion("com.sun.xml.ws", "jaxws-ri", version)).build())
          .type("pom").build();
        GradleDependencyConfiguration configuration = new GradleDependencyConfiguration("implementation", null,
          true, true, false, false, Collections.emptyList(), Collections.singletonList(dependency.getRequested()),
          Collections.singletonList(dependency), null, null);
        String script = """
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'com.sun.xml.ws:jaxws-ri:%s'
          }
          """.formatted(version);
        rewriteRun(
          spec -> spec.recipe(new RemoveLegacyJaxwsRi()),
          buildGradle(script, spec -> {
              spec.markers(GradleProject.builder().name("sample").path(":sample")
                .nameToConfiguration(Collections.singletonMap("implementation", configuration)).build());
              if (version.startsWith("2.")) {
                  spec.after(after -> {
                      assertThat(after).doesNotContain("jaxws-ri");
                      return after;
                  });
              }
          })
        );
    }

    @Test
    void stillRemoveLegacyManagedPom() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><metro.version>2.3.2</metro.version></properties>
            <dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>com.sun.xml.ws</groupId>
                  <artifactId>jaxws-ri</artifactId>
                  <version>${metro.version}</version>
                  <type>pom</type>
                </dependency>
              </dependencies>
            </dependencyManagement>
          </project>
          """, """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><metro.version>2.3.2</metro.version></properties>
          </project>
          """));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.0", "4.0.5"})
    void preserveManagedPomWithPropertyVersion(String version) {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><metro.version>%s</metro.version></properties>
            <dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>com.sun.xml.ws</groupId>
                  <artifactId>jaxws-ri</artifactId>
                  <version>${metro.version}</version>
                  <type>pom</type>
                </dependency>
              </dependencies>
            </dependencyManagement>
          </project>
          """.formatted(version)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.0", "4.0.5"})
    void preserveDirectPomWithPropertyVersion(String version) {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><metro.version>%s</metro.version></properties>
            <dependencies>
              <dependency>
                <groupId>com.sun.xml.ws</groupId>
                <artifactId>jaxws-ri</artifactId>
                <version>${metro.version}</version>
                <type>pom</type>
              </dependency>
            </dependencies>
          </project>
          """.formatted(version)));
    }
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void managedLegacyDirectPom(boolean standalone) {
        rewriteRun(
          spec -> {
              if (standalone) {
                  spec.recipe(new RemoveLegacyJaxwsRi());
              }
          },
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1</version>
                  <properties>
                      <metro.version>2.3.2</metro.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.sun.xml.ws</groupId>
                              <artifactId>jaxws-ri</artifactId>
                              <version>${metro.version}</version>
                              <type>pom</type>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>com.sun.xml.ws</groupId>
                          <artifactId>jaxws-ri</artifactId>
                          <type>pom</type>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1</version>
                  <properties>
                      <metro.version>2.3.2</metro.version>
                  </properties>
              </project>
              """
          )
        );
    }
}
