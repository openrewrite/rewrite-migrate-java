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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;

class AddJaxwsRuntimeGradleTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJaxwsRuntime());
    }

    @CsvSource({
      "implementation,implementation", "implementation,runtimeOnly",
      "testImplementation,implementation", "testImplementation,runtimeOnly",
      "testImplementation,testImplementation", "testImplementation,testRuntimeOnly"
    })
    @ParameterizedTest
    void providerAvailableToApi(String apiConfiguration, String providerConfiguration) {
        var declarations = declarations(apiConfiguration, providerConfiguration, "org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"testImplementation", "testRuntimeOnly"})
    void onlyProviderDoesNotSatisfyMainApi(String providerConfiguration) {
        var declarations = declarations("implementation", providerConfiguration, "org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "compileOnly"))));
    }

    @Test
    void providedMainProviderStillNeedsTestCoverage() {
        var declarations = declarations("implementation", "compileOnly", "org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "testImplementation"))));
    }

    @Test
    void missingMainProviderAndIdempotency() {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("implementation", List.of(api()));
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
            .after(after -> metro(after, "compileOnly", "testImplementation")))
        );
    }

    @Test
    void missingTestProvider() {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("testImplementation", List.of(api()));
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "testImplementation"))));
    }

    @Test
    void providersTogetherCoverMainAndTests() {
        var declarations = declarations("implementation", "compileOnly", "org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        declarations.put("testRuntimeOnly", List.of(dependency("com.sun.xml.ws", "jaxws-rt", "2.3.7")));
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))));
    }

    @Test
    void transitiveProvider() {
        ResolvedDependency provider = dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5").withDepth(1);
        ResolvedDependency library = dependency("org.apache.cxf", "cxf-spring-boot-starter-jaxws", "3.5.5").withDependencies(List.of(provider));
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("implementation", List.of(api(), library));
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))));
    }

    @Test
    void existingMetro() {
        var declarations = declarations("implementation", "implementation", "com.sun.xml.ws", "jaxws-rt", "2.3.7");
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))));
    }

    @Test
    void onlyMetroDoesNotSatisfyMainApi() {
        var declarations = declarations("implementation", "testImplementation", "com.sun.xml.ws", "jaxws-rt", "2.3.7");
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "compileOnly", "testImplementation"))));
    }

    @CsvSource({"org.apache.cxf,cxf-rt-transports-http,3.5.5", "org.springframework.ws,spring-ws-core,3.1.3", "org.apache.cxf,cxf-rt-frontend-jaxws,4.0.0"})
    @ParameterizedTest
    void incompatibleOrUnrelatedProvider(String group, String artifact, String version) {
        var declarations = declarations("implementation", "implementation", group, artifact, version);
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "compileOnly", "testImplementation"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pom", "sources"})
    void nonRuntimeProviderArtifact(String artifactKind) {
        ResolvedDependency provider = dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        if ("pom".equals(artifactKind)) {
            provider = provider.withType("pom").withRequested(provider.getRequested().withType("pom"));
        } else {
            provider = provider.withClassifier("sources").withRequested(provider.getRequested().withClassifier("sources"));
        }
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("implementation", List.of(api(), provider));
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))
          .after(after -> metro(after, "compileOnly", "testImplementation"))));
    }

    @Test
    void modernApiGuard() {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("implementation", List.of(dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "3.0.1")));
        rewriteRun(buildGradle(script(declarations), spec -> spec.markers(project("app", declarations))));
    }

    @Test
    void buildscriptProviderIsNotProjectProvider() {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("implementation", List.of(api()));
        ResolvedDependency cxf = dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        GradleDependencyConfiguration classpath = configuration("classpath", true, List.of(cxf));
        GradleProject project = project("app", declarations).withBuildscript(new GradleBuildscript(UUID.randomUUID(),
          List.of(MavenRepository.MAVEN_CENTRAL), Map.of("classpath", classpath)));
        String before = """
          buildscript {
              repositories { mavenCentral() }
              dependencies { classpath 'org.apache.cxf:cxf-rt-frontend-jaxws:3.5.5' }
          }

          """ + script(declarations);
        rewriteRun(buildGradle(before, spec -> spec.markers(project).after(after -> {
            assertThat(after.substring(0, after.indexOf("apply plugin"))).doesNotContain("com.sun.xml.ws");
            return metro(after, "compileOnly", "testImplementation");
        })));
    }

    @Test
    void projectIsolation() {
        var provider = declarations("implementation", "implementation", "org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5");
        var missing = new LinkedHashMap<String, List<ResolvedDependency>>();
        missing.put("implementation", List.of(api()));
        rewriteRun(
          buildGradle(script(provider), spec -> spec.path("provider/build.gradle").markers(project("provider", provider))),
          buildGradle(script(missing), spec -> spec.path("missing/build.gradle").markers(project("missing", missing))
            .after(after -> metro(after, "compileOnly", "testImplementation")))
        );
    }

    @CsvSource({"true,true", "true,false", "false,true"})
    @ParameterizedTest
    void opaqueRuntimeFallbackDoesNotPolluteCoveredProvider(boolean coveredHasApi, boolean coveredHasProvider) {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("sharedApi", List.of(api()));
        if (coveredHasProvider) {
            declarations.put("cxfProvider", List.of(dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5")));
        }
        var parents = new LinkedHashMap<String, List<String>>();
        parents.put("sharedApi", List.of());
        parents.put("cxfProvider", List.of());
        var coveredParents = new ArrayList<String>();
        if (coveredHasApi) {
            coveredParents.add("sharedApi");
        }
        if (coveredHasProvider) {
            coveredParents.add("cxfProvider");
        }
        parents.put("coveredRuntimeClasspath", coveredParents);
        parents.put("uncoveredRuntimeClasspath", List.of("sharedApi"));
        GradleProject project = project("app", declarations, parents);
        String before = script(declarations).replace("dependencies {", """
          configurations {
              sharedApi { canBeResolved = false; canBeConsumed = false }
              cxfProvider { canBeResolved = false; canBeConsumed = false }
              coveredRuntimeClasspath {
                  canBeResolved = true; canBeConsumed = false; canBeDeclared = false
                  extendsFrom(%s)
              }
              uncoveredRuntimeClasspath {
                  canBeResolved = true; canBeConsumed = false; canBeDeclared = false
                  extendsFrom(sharedApi)
              }
          }
          dependencies {""".formatted(String.join(", ", coveredParents)));
        if (coveredHasApi && coveredHasProvider) {
            // The only existing declaration bucket is shared with an already-covered runtime.
            rewriteRun(buildGradle(before, spec -> spec.markers(project)));
        } else {
            // Keep the existing fallback when every consumer needs a provider; unrelated providers do not block it.
            rewriteRun(buildGradle(before, spec -> spec.markers(project).after(after -> metro(after, "sharedApi"))));
        }
    }

    @Test
    void sharedImplementationDoesNotPolluteCoveredChildRuntime() {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.put("integrationTestImplementation", List.of(api()));
        declarations.put("functionalTestRuntimeOnly", List.of(dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5")));
        var parents = new LinkedHashMap<String, List<String>>();
        parents.put("integrationTestImplementation", List.of());
        parents.put("integrationTestRuntimeOnly", List.of());
        parents.put("integrationTestCompileClasspath", List.of("integrationTestImplementation"));
        parents.put("integrationTestRuntimeClasspath", List.of("integrationTestImplementation", "integrationTestRuntimeOnly"));
        parents.put("functionalTestImplementation", List.of("integrationTestImplementation"));
        parents.put("functionalTestRuntimeOnly", List.of());
        parents.put("functionalTestCompileClasspath", List.of("functionalTestImplementation"));
        parents.put("functionalTestRuntimeClasspath", List.of("functionalTestImplementation", "functionalTestRuntimeOnly"));
        String before = script(declarations).replace("dependencies {", """
          sourceSets { integrationTest {}; functionalTest {} }
          configurations.functionalTestImplementation.extendsFrom(configurations.integrationTestImplementation)
          dependencies {""");
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          buildGradle(before, spec -> spec.markers(project("app", declarations, parents))
            .after(after -> metro(after, "integrationTestRuntimeOnly")))
        );
    }

    private static Map<String, List<ResolvedDependency>> declarations(String apiConfiguration, String providerConfiguration,
                                                                     String group, String artifact, String version) {
        var declarations = new LinkedHashMap<String, List<ResolvedDependency>>();
        declarations.computeIfAbsent(apiConfiguration, ignored -> new ArrayList<>()).add(api());
        declarations.computeIfAbsent(providerConfiguration, ignored -> new ArrayList<>()).add(dependency(group, artifact, version));
        return declarations;
    }

    private static ResolvedDependency api() {
        return dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3");
    }

    private static ResolvedDependency dependency(String group, String artifact, String version) {
        List<GroupArtifact> exclusions = "org.apache.cxf".equals(group) && "cxf-rt-frontend-jaxws".equals(artifact) ?
          List.of(new GroupArtifact("jakarta.xml.ws", "jakarta.xml.ws-api")) : List.of();
        return ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, group, artifact, version, null))
          .requested(Dependency.builder().gav(new GroupArtifactVersion(group, artifact, version)).exclusions(exclusions).build())
          .type("jar").build();
    }

    private static GradleDependencyConfiguration configuration(String name, boolean resolvable, List<ResolvedDependency> dependencies) {
        return new GradleDependencyConfiguration(name, null, true, resolvable, false, !resolvable, List.of(),
          dependencies.stream().map(ResolvedDependency::getRequested).toList(), resolvable ? dependencies : List.of(), null, null);
    }

    private static GradleProject project(String name, Map<String, List<ResolvedDependency>> declarations) {
        return project(name, declarations, Map.of());
    }

    private static GradleProject project(String name, Map<String, List<ResolvedDependency>> declarations,
                                         Map<String, List<String>> additionalParents) {
        var parents = new LinkedHashMap<String, List<String>>();
        parents.put("implementation", List.of());
        parents.put("compileOnly", List.of());
        parents.put("runtimeOnly", List.of());
        parents.put("testImplementation", List.of("implementation"));
        parents.put("testCompileOnly", List.of());
        parents.put("testRuntimeOnly", List.of("runtimeOnly"));
        parents.put("compileClasspath", List.of("implementation", "compileOnly"));
        parents.put("runtimeClasspath", List.of("implementation", "runtimeOnly"));
        parents.put("testCompileClasspath", List.of("testImplementation", "testCompileOnly"));
        parents.put("testRuntimeClasspath", List.of("testImplementation", "testRuntimeOnly"));
        parents.putAll(additionalParents);
        var configurations = new LinkedHashMap<String, GradleDependencyConfiguration>();
        for (String configuration : parents.keySet()) {
            var dependencies = new ArrayList<ResolvedDependency>();
            collect(configuration, parents, declarations, dependencies);
            configurations.put(configuration, configuration(configuration, configuration.endsWith("Classpath"), dependencies));
        }
        for (Map.Entry<String, List<String>> entry : parents.entrySet()) {
            configurations.get(entry.getKey()).unsafeSetExtendsFrom(entry.getValue().stream().map(configurations::get).toList());
        }
        GradleProject project = GradleProject.builder().name(name).path(":" + name)
          .mavenRepositories(List.of(MavenRepository.MAVEN_CENTRAL)).nameToConfiguration(configurations).build();
        // Verify actual reference identity: equal names on disconnected objects do not model Gradle inheritance.
        for (GradleDependencyConfiguration configuration : project.getConfigurations()) {
            assertThat(configuration.isCanBeDeclared()).isNotEqualTo(configuration.isCanBeResolved());
            for (GradleDependencyConfiguration parent : configuration.getExtendsFrom()) {
                assertThat(project.getConfiguration(parent.getName())).isSameAs(parent);
            }
        }
        return project;
    }

    private static void collect(String name, Map<String, List<String>> parents,
                                Map<String, List<ResolvedDependency>> declarations, List<ResolvedDependency> result) {
        result.addAll(declarations.getOrDefault(name, List.of()));
        for (String parent : parents.get(name)) {
            collect(parent, parents, declarations, result);
        }
    }

    private static String script(Map<String, List<ResolvedDependency>> declarations) {
        var script = new StringBuilder("apply plugin: 'java'\nrepositories { mavenCentral() }\ndependencies {\n");
        declarations.forEach((configuration, dependencies) -> dependencies.forEach(d -> {
            String coordinate = d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion();
            if (d.getClassifier() != null) {
                coordinate += ":" + d.getClassifier();
            }
            if (d.getType() != null && !"jar".equals(d.getType())) {
                coordinate += "@" + d.getType();
            }
            script.append("    ").append(configuration);
            if ("org.apache.cxf".equals(d.getGroupId()) && "cxf-rt-frontend-jaxws".equals(d.getArtifactId())) {
                // A direct API declaration does not defeat Gradle's newest-version conflict resolution.
                script.append("('").append(coordinate)
                  .append("') { exclude group: 'jakarta.xml.ws', module: 'jakarta.xml.ws-api' }\n");
            } else {
                script.append(" '").append(coordinate).append("'\n");
            }
        }));
        return script.append("}\n").toString();
    }

    private static String metro(String after, String... expected) {
        Matcher matcher = Pattern.compile("(?m)^\\s*(\\w+) ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]").matcher(after);
        var configurations = new ArrayList<String>();
        while (matcher.find()) {
            configurations.add(matcher.group(1));
        }
        assertThat(configurations).containsExactlyInAnyOrder(expected);
        return after;
    }
}
