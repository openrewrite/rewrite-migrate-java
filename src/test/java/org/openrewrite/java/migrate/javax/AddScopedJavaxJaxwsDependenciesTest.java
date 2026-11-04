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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddScopedJavaxJaxwsDependenciesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-11.yml", "org.openrewrite.java.migrate.javax.AddJaxwsDependencies")
          .parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.xml.ws.http; public class HTTPException extends RuntimeException {}",
            "package javax.jws; public @interface WebService {}"));
    }

    @ParameterizedTest
    @CsvSource({"test,jakarta.xml.ws,jakarta.xml.ws-api,2.3.3", "runtime,jakarta.xml.ws,jakarta.xml.ws-api,2.3.3", "test,javax.xml.ws,jaxws-api,2.3.1", "runtime,javax.xml.ws,jaxws-api,2.3.1"})
    void promoteExistingEquivalentForMain(String scope, String group, String artifact, String version) {
        String before = pom(group, artifact, version, scope);
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          pomXml(before, before.replace("<scope>" + scope + "</scope>", "<scope>compile</scope>"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile", "provided"})
    void keepSuitableMainScope(String scope) {
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          pomXml(pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", scope))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile", "provided", "runtime", "test"})
    void testOnlyUseDoesNotBroadenExistingApi(String scope) {
        rewriteRun(mavenProject("app", srcTestJava(java("class ExampleTest { javax.xml.ws.http.HTTPException error; }")),
          pomXml(pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", scope))));
    }

    @Test
    void noSourceUseDoesNotPromoteRuntimeApi() {
        rewriteRun(pomXml(pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "runtime")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"testImplementation", "runtimeOnly"})
    void gradlePromotesExistingEquivalentForMain(String configuration) {
        String before = gradle(configuration);
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle(before, before.replace(configuration + " '", "implementation '"),
            spec -> spec.markers(gradleModel(configuration, false))));
    }

    @Test
    void gradleCompileOnlyIsAlreadySuitableForMain() {
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle(gradle("compileOnly"),
            spec -> spec.markers(gradleModel("compileOnly", true))));
    }

    @Test
    void gradleTestOnlyUseKeepsTestApi() {
        rewriteRun(srcTestJava(java("class ExampleTest { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle(gradle("testImplementation"),
            spec -> spec.markers(gradleModel("testImplementation", false))));
    }

    @Test
    void gradleExistingTransitiveCompileApiIsSuitable() {
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle("plugins { id 'java' }",
            spec -> spec.markers(gradleModel("unrelated", true))));
    }

    @Test
    void siblingModuleUseDoesNotPromoteAnotherModule() {
        String before = pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "test");
        rewriteRun(
          mavenProject("using", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
            pomXml(before, before.replace("<scope>test</scope>", "<scope>compile</scope>"))),
          mavenProject("other", pomXml(before)));
    }

    @Test
    void childModuleUseDoesNotPromoteAggregator() {
        String before = pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "test");
        rewriteRun(pomXml(before.replace("<version>1</version>", "<version>1</version>\n  <packaging>pom</packaging>")),
          mavenProject("child", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
            pomXml(before, before.replace("<scope>test</scope>", "<scope>compile</scope>"))));
    }

    @Test
    void buildSrcJavaDoesNotPromoteApplicationApi() {
        rewriteRun(java("class Tool { javax.xml.ws.http.HTTPException error; }", spec -> spec.path("buildSrc/src/main/java/Tool.java")),
          org.openrewrite.gradle.Assertions.buildGradle(gradle("runtimeOnly"),
            spec -> spec.markers(gradleModel("runtimeOnly", false))));
    }

    @Test
    void customSourceSetSuppliesExistingAliasOnlyToItsOwnCompileConfiguration() {
        var model = gradleModel("runtimeOnly", false);
        var configurations = new java.util.LinkedHashMap<>(model.getNameToConfiguration());
        configurations.put("integrationTestImplementation", configuration("integrationTestImplementation", java.util.List.of()));
        configurations.put("integrationTestCompileClasspath", configuration("integrationTestCompileClasspath",
          java.util.List.of(dependency("jakarta.jws", "jakarta.jws-api", "3.0.0"))));
        var custom = model.withNameToConfiguration(configurations);
        rewriteRun(java("class Integration { javax.xml.ws.http.HTTPException error; }", spec ->
            spec.path("src/integrationTest/java/Integration.java").markers(new org.openrewrite.java.marker.JavaSourceSet(
              org.openrewrite.Tree.randomId(), "integrationTest", java.util.List.of(), java.util.Map.of()))),
          org.openrewrite.gradle.Assertions.buildGradle(gradle("runtimeOnly"), spec -> spec.markers(custom).after(after -> {
              org.assertj.core.api.Assertions.assertThat(after)
                .contains("runtimeOnly 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'")
                .contains("integrationTestImplementation")
                .doesNotContain("javax.xml.ws");
              return after;
          })));
    }

    @Test
    void toolConfigurationDoesNotSupplyAnApplicationCompileApi() {
        var base = gradleModel("classpath", false);
        var configurations = new java.util.LinkedHashMap<>(base.getNameToConfiguration());
        configurations.remove("testCompileClasspath");
        var model = base.withNameToConfiguration(configurations);
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle("plugins { id 'java' }", spec -> spec.markers(model).after(after -> {
              org.assertj.core.api.Assertions.assertThat(after).contains("implementation").contains("javax.xml.ws:jaxws-api:2.3.1")
                .doesNotContain("jakarta.xml.ws-api:2.3.3");
              return after;
          })));
    }

    @Test
    void managedOnlyEquivalentVersionDoesNotPreventAddingAnApiForTestSources() {
        String before = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
            <dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>2.3.3</version>
                </dependency>
              </dependencies>
            </dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>jakarta.jws</groupId>
                <artifactId>jakarta.jws-api</artifactId>
                <version>3.0.0</version>
              </dependency>
            </dependencies>
          </project>
          """;
        rewriteRun(mavenProject("app", srcTestJava(java("class ExampleTest { javax.xml.ws.http.HTTPException error; }")),
          pomXml(before, spec -> spec.after(after -> {
              org.assertj.core.api.Assertions.assertThat(after).contains("javax.xml.ws").contains("<scope>test</scope>");
              return after;
          }))));
    }

    @Test
    void keepBothLegacyAliasesWhenAlreadySuitable() {
        String before = pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "provided")
          .replace("  </dependencies>", """
                  <dependency>
                    <groupId>javax.xml.ws</groupId>
                    <artifactId>jaxws-api</artifactId>
                    <version>2.3.1</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>""");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")), pomXml(before)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"jakarta.jws", "javax.jws"})
    void promoteJwsEquivalentAliasIndependentlyOfModernWs(String group) {
        String before = pom(group, "javax.jws".equals(group) ? "javax.jws-api" : "jakarta.jws-api",
          "javax.jws".equals(group) ? "1.1" : "2.1.0", "test")
          .replaceFirst("<groupId>jakarta.jws</groupId>\\s*<artifactId>jakarta.jws-api</artifactId>\\s*<version>3.0.0</version>",
            "<groupId>jakarta.xml.ws</groupId>\n        <artifactId>jakarta.xml.ws-api</artifactId>\n        <version>4.0.2</version>");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.jws.WebService service; }")),
          pomXml(before, before.replace("<scope>test</scope>", "<scope>compile</scope>"))));
    }

    @Test
    void mainUseSuppliesTransitiveRuntimeAliasWithoutBroadeningItsProvider() {
        String before = pom("com.sun.xml.ws", "jaxws-rt", "2.3.7", "runtime");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          pomXml(before, spec -> spec.after(after -> {
              org.assertj.core.api.Assertions.assertThat(after).contains("<artifactId>jakarta.xml.ws-api</artifactId>")
                .doesNotContain("<groupId>javax.xml.ws</groupId>")
                .contains("<artifactId>jaxws-rt</artifactId>\n      <version>2.3.7</version>\n      <scope>runtime</scope>");
              return after;
          }))));
    }

    @Test
    void gradleTransitiveRuntimeAliasIsAddedToMainCompileWithoutMovingWrapper() {
        var model = gradleModel("runtimeOnly", false);
        var configurations = new java.util.LinkedHashMap<>(model.getNameToConfiguration());
        var wrapper = dependency("example", "wrapper", "1").withDependencies(java.util.List.of(
          dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3")));
        configurations.put("runtimeOnly", configuration("runtimeOnly", java.util.List.of(wrapper)));
        configurations.put("runtimeClasspath", configuration("runtimeClasspath", java.util.List.of(wrapper)));
        var runtime = model.withNameToConfiguration(configurations);
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle("""
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                runtimeOnly 'example:wrapper:1'
            }
            """, spec -> spec.markers(runtime).after(after -> {
                org.assertj.core.api.Assertions.assertThat(after).contains("runtimeOnly 'example:wrapper:1'")
                  .contains("implementation").contains("jakarta.xml.ws:jakarta.xml.ws-api:2.3.3").doesNotContain("javax.xml.ws");
                return after;
            })));
    }

    @Test
    void neverPromoteAnAliasThatWouldDowngradeModernMainApi() {
        var model = gradleModel("testImplementation", false);
        var configurations = new java.util.LinkedHashMap<>(model.getNameToConfiguration());
        configurations.put("compileClasspath", configuration("compileClasspath", java.util.List.of(
          dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "4.0.2"))));
        var mixed = model.withNameToConfiguration(configurations);
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle("""
            plugins { id 'java' }
            repositories { mavenCentral() }
            dependencies {
                implementation 'jakarta.xml.ws:jakarta.xml.ws-api:4.0.2'
                testImplementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
            }
            """, spec -> spec.markers(mixed).after(after -> {
                org.assertj.core.api.Assertions.assertThat(after).contains("implementation 'jakarta.xml.ws:jakarta.xml.ws-api:4.0.2'")
                  .contains("testImplementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'")
                  .contains("javax.xml.ws:jaxws-api:2.3.1");
                return after;
            })));
    }

    @Test
    void gradlePromotionLeavesOtherProjectClosuresAlone() {
        String before = gradle("runtimeOnly") + """
          subprojects {
              dependencies {
                  runtimeOnly 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              }
          }
          """;
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          org.openrewrite.gradle.Assertions.buildGradle(before,
            before.replaceFirst("runtimeOnly '", "implementation '"), spec -> spec.markers(gradleModel("runtimeOnly", false))));
    }

    @Test
    void sourceProjectIdentityPreventsWrongBuildAssociation() {
        var sourceProject = new org.openrewrite.java.marker.JavaProject(org.openrewrite.Tree.randomId(), "source", null);
        var otherProject = new org.openrewrite.java.marker.JavaProject(org.openrewrite.Tree.randomId(), "other", null);
        rewriteRun(srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }", spec -> spec.markers(sourceProject))),
          org.openrewrite.gradle.Assertions.buildGradle(gradle("runtimeOnly"),
            spec -> spec.markers(gradleModel("runtimeOnly", false), otherProject)));
    }

    @Test
    void unmodelledChildBuildStillSeparatesItsSourcesFromParent() {
        rewriteRun(pomXml(pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "test")),
          org.openrewrite.test.SourceSpecs.dir("child",
            org.openrewrite.test.SourceSpecs.text("unmodelled child", spec -> spec.path("pom.xml")),
            srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }"))));
    }

    @Test
    void promotePropertyCoordinatesWithoutRewritingTheirVersion() {
        String before = pom("${api.group}", "${api.artifact}", "${api.version}", "test")
          .replace("  <dependencies>", """
                <properties>
                  <api.group>jakarta.xml.ws</api.group>
                  <api.artifact>jakarta.xml.ws-api</api.artifact>
                  <api.version>2.3.3</api.version>
                </properties>
                <dependencies>""");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          pomXml(before, before.replace("<scope>test</scope>", "<scope>compile</scope>"))));
    }

    @Test
    void providedScopeFromDependencyManagementIsAlreadySuitable() {
        String before = pom("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", "provided")
          .replace("  <dependencies>", """
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.xml.ws</groupId>
                      <artifactId>jakarta.xml.ws-api</artifactId>
                      <version>2.3.3</version>
                      <scope>provided</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>""")
          .replace("      <scope>provided</scope>\n    </dependency>\n  </dependencies>", "    </dependency>\n  </dependencies>");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")), pomXml(before)));
    }

    @Test
    void publicLegacyNormalizationStillUsesJakartaEe8Api() {
        String before = pom("javax.xml.ws", "jaxws-api", "2.3.1", "compile")
          .replace("    <dependency>\n      <groupId>jakarta.jws</groupId>\n      <artifactId>jakarta.jws-api</artifactId>\n      <version>3.0.0</version>\n    </dependency>\n", "");
        rewriteRun(mavenProject("app", srcMainJava(java("class Example { javax.xml.ws.http.HTTPException error; }")),
          pomXml(before, spec -> spec.after(after -> {
              org.assertj.core.api.Assertions.assertThat(after).contains("<groupId>jakarta.xml.ws</groupId>")
                .contains("<artifactId>jakarta.xml.ws-api</artifactId>").contains("<version>2.3.")
                .doesNotContain("<groupId>javax.xml.ws</groupId>");
              return after;
          }))));
    }

    static String gradle(String configuration) {
        return """
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              %s 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
          }
          """.formatted(configuration);
    }

    static org.openrewrite.gradle.marker.GradleProject gradleModel(String configuration, boolean mainAvailable) {
        var legacy = dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3");
        var modern = dependency("jakarta.jws", "jakarta.jws-api", "3.0.0");
        var configurations = new java.util.LinkedHashMap<String, org.openrewrite.gradle.marker.GradleDependencyConfiguration>();
        configurations.put("compileClasspath", configuration("compileClasspath", mainAvailable ? java.util.List.of(modern, legacy) : java.util.List.of(modern)));
        configurations.put("implementation", configuration("implementation", java.util.List.of()));
        configurations.put(configuration, configuration(configuration, java.util.List.of(legacy)));
        configurations.put("testCompileClasspath", configuration("testCompileClasspath", java.util.List.of(modern, legacy)));
        return org.openrewrite.gradle.marker.GradleProject.builder().name("app").path(":")
          .mavenRepositories(java.util.List.of(org.openrewrite.maven.tree.MavenRepository.MAVEN_CENTRAL))
          .nameToConfiguration(configurations).build();
    }

    static org.openrewrite.gradle.marker.GradleDependencyConfiguration configuration(String name, java.util.List<org.openrewrite.maven.tree.ResolvedDependency> dependencies) {
        return new org.openrewrite.gradle.marker.GradleDependencyConfiguration(name, null, true, true, false, true,
          java.util.List.of(), dependencies.stream().map(org.openrewrite.maven.tree.ResolvedDependency::getRequested).toList(), dependencies, null, null);
    }

    static org.openrewrite.maven.tree.ResolvedDependency dependency(String group, String artifact, String version) {
        return org.openrewrite.maven.tree.ResolvedDependency.builder()
          .gav(new org.openrewrite.maven.tree.ResolvedGroupArtifactVersion(null, group, artifact, version, null))
          .requested(org.openrewrite.maven.tree.Dependency.builder().gav(new org.openrewrite.maven.tree.GroupArtifactVersion(group, artifact, version)).build())
          .type("jar").build();
    }

    static String pom(String group, String artifact, String version, String scope) {
        return """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
            <dependencies>
              <dependency>
                <groupId>jakarta.jws</groupId>
                <artifactId>jakarta.jws-api</artifactId>
                <version>3.0.0</version>
              </dependency>
              <dependency>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
                <scope>%s</scope>
              </dependency>
            </dependencies>
          </project>
          """.formatted(group, artifact, version, scope);
    }
}
