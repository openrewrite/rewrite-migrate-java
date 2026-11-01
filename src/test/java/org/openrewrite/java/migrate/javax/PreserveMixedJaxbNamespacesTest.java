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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.*;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.*;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.maven.Assertions.pomXml;

class PreserveMixedJaxbNamespacesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.javax.AddJaxbAPIDependencies");
    }

    @Test
    void runtimeGraphRequiresSeparateJavaxApi() {
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'jakarta.xml.bind:jakarta.xml.bind-api:2.3.3'
              runtimeOnly 'org.glassfish.jaxb:jaxb-runtime:2.3.9'
          }
          """, """
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'jakarta.xml.bind:jakarta.xml.bind-api:2.3.3'
              runtimeOnly "javax.xml.bind:jaxb-api:2.3.1"
              runtimeOnly 'org.glassfish.jaxb:jaxb-runtime:2.3.9'
          }
          """, s -> s.markers(graph(true))));
    }

    @Test
    void modernOnlyGradleDoesNotAddLegacyApi() {
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.2'
          }
          """, s -> s.markers(graph(false))));
    }

    @Test
    void existingGradleLegacyApiAndScopeArePreserved() {
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.2'
              compileOnly 'javax.xml.bind:jaxb-api:2.3.1'
              runtimeOnly 'org.glassfish.jaxb:jaxb-runtime:2.3.9'
          }
          """, s -> s.markers(graph(true).withNameToConfiguration(Map.of("compileOnly",
          new GradleDependencyConfiguration("compileOnly", null, true, true, false, false,
            Collections.emptyList(), List.of(Dependency.builder().gav(new GroupArtifactVersion("javax.xml.bind", "jaxb-api", "2.3.1")).build()),
            List.of(dep("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2"), dep("org.glassfish.jaxb", "jaxb-runtime", "2.3.9")), null, null))))));
    }

    @Test
    void mavenMixedNamespacesAddSeparateApi() {
        String dependencies = dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2", "") +
          dependency("org.glassfish.jaxb", "jaxb-runtime", "2.3.9", "runtime");
        rewriteRun(pomXml(pom(dependencies), pom(dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2", "") +
          dependency("javax.xml.bind", "jaxb-api", "2.3.1", "runtime") +
          dependency("org.glassfish.jaxb", "jaxb-runtime", "2.3.9", "runtime"))));
    }

    @Test
    void mavenExistingLegacyScopeAndVersionArePreserved() {
        rewriteRun(pomXml(pom(dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.1", "") +
          dependency("javax.xml.bind", "jaxb-api", "2.3.0", "provided") +
          dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.6.5", "runtime"))));
    }

    @Test
    void modernOnlyMavenDoesNotAddLegacyApi() {
        rewriteRun(pomXml(pom(dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2", "") +
          dependency("org.glassfish.jaxb", "jaxb-runtime", "4.0.5", "runtime"))));
    }

    @Test
    void legacyOnlyBehaviorIsUnchanged() {
        rewriteRun(pomXml(pom(dependency("javax.xml.bind", "jaxb-api", "2.3.1", "")),
          pom(dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3", ""))));
    }

    @Test
    void transitiveModernMavenApiWithLegacyJaxws() {
        String modern = dependency("org.quartz-scheduler", "quartz", "2.5.0", "");
        String legacy = dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "runtime");
        rewriteRun(pomXml(pom(modern + legacy),
          pom(modern + legacy + dependency("javax.xml.bind", "jaxb-api", "2.3.1", "runtime"))));
    }

    @Test
    void managedModernApiPreservesLegacyCoordinate() {
        String input = pom(dependency("javax.xml.bind", "jaxb-api", "2.3.1", "provided"));
        input = input.replace("  <dependencies>", "  <dependencyManagement>\n  <dependencies>\n" +
          dependency("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2", "") +
          "  </dependencies>\n  </dependencyManagement>\n  <dependencies>");
        rewriteRun(pomXml(input));
    }

    private static String pom(String dependencies) {
        return "<project>\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>example</groupId>\n" +
          "  <artifactId>sample</artifactId>\n  <version>1</version>\n  <dependencies>\n" +
          dependencies + "  </dependencies>\n</project>";
    }

    private static String dependency(String group, String artifact, String version, String scope) {
        return "    <dependency>\n      <groupId>" + group + "</groupId>\n      <artifactId>" + artifact +
          "</artifactId>\n      <version>" + version + "</version>\n" +
          (scope.isEmpty() ? "" : "      <scope>" + scope + "</scope>\n") + "    </dependency>\n";
    }

    private static GradleProject graph(boolean legacy) {
        Map<String, GradleDependencyConfiguration> configs = new HashMap<>();
        configs.put("compileClasspath", configuration("compileClasspath", List.of(dep("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3"))));
        ResolvedDependency modern = dep("example", "modern-library", "1").withDependencies(List.of(dep("jakarta.xml.bind", "jakarta.xml.bind-api", "4.0.2")));
        List<ResolvedDependency> runtime = new ArrayList<>(List.of(modern));
        if (legacy) {
            runtime.add(dep("example", "legacy-library", "1").withDependencies(List.of(dep("org.glassfish.jaxb", "jaxb-runtime", "2.3.9"))));
        }
        configs.put("testRuntimeClasspath", configuration("testRuntimeClasspath", runtime));
        configs.put("runtimeOnly", configuration("runtimeOnly", List.of()));
        return GradleProject.builder().name("sample").path(":sample").mavenRepositories(List.of(MavenRepository.MAVEN_CENTRAL)).nameToConfiguration(configs).build();
    }

    private static GradleDependencyConfiguration configuration(String name, List<ResolvedDependency> resolved) {
        return new GradleDependencyConfiguration(name, null, true, true, false, false,
          Collections.emptyList(), Collections.emptyList(), resolved, null, null);
    }

    private static ResolvedDependency dep(String group, String artifact, String version) {
        return ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, group, artifact, version, null))
          .requested(Dependency.builder().gav(new GroupArtifactVersion(group, artifact, version)).build())
          .depth(0).type("jar").build();
    }
}
