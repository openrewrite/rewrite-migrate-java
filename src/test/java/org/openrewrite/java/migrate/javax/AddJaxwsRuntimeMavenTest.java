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
import org.openrewrite.Issue;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.stream.Collectors;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.assertj.core.api.Assertions.assertThat;

class AddJaxwsRuntimeMavenTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJaxwsRuntime());
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1314")
    @Test
    void existingCxfProvider() {
        rewriteRun(
          pomXml(
            """
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>cxf-sample</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>org.apache.cxf</groupId>
                        <artifactId>cxf-rt-frontend-jaxws</artifactId>
                        <version>3.5.5</version>
                    </dependency>
                </dependencies>
            </project>
            """
          )
        );
    }

    @Test
    void transitiveCxfProvider() {
        rewriteRun(pomXml(pom(dependency("org.apache.cxf", "cxf-spring-boot-starter-jaxws", "3.5.5", "compile"))));
    }

    @ParameterizedTest
    @CsvSource({"compile,compile", "provided,provided", "runtime,runtime", "test,compile", "test,test"})
    void providerAvailableToApi(String apiScope, String providerScope) {
        rewriteRun(pomXml(pom(api(apiScope) + cxf(providerScope, "3.5.5"))));
    }

    @Test
    void testOnlyProviderDoesNotSatisfyMainApi() {
        rewriteRun(pomXml(pom(api("compile") + cxf("test", "3.5.5")), spec -> spec.after(after -> metro(after, "provided"))));
    }

    @Test
    void runtimeOnlyProviderSatisfiesMainRuntime() {
        rewriteRun(pomXml(pom(api("compile") + cxf("runtime", "3.5.5"))));
    }

    @Test
    void providedProviderSatisfiesProvidedContract() {
        rewriteRun(pomXml(pom(api("compile") + cxf("provided", "3.5.5"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile", "test"})
    void missingProvider(String scope) {
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          pomXml(pom(api(scope)), spec -> spec.after(after -> metro(after, "test".equals(scope) ? "test" : "provided")))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile", "provided", "runtime", "test"})
    void existingMetroSatisfiesTestApi(String scope) {
        rewriteRun(pomXml(pom(api("test") + dependency("com.sun.xml.ws", "jaxws-rt", "2.3.7", scope))));
    }

    @Test
    void existingMetro() {
        rewriteRun(pomXml(pom(api("compile") + dependency("com.sun.xml.ws", "jaxws-rt", "2.3.7", "provided"))));
    }

    @Test
    void testOnlyMetroDoesNotSatisfyMainApi() {
        rewriteRun(pomXml(pom(api("compile") + dependency("com.sun.xml.ws", "jaxws-rt", "2.3.7", "test")),
          spec -> spec.after(after -> metro(after, "provided"))));
    }

    @ParameterizedTest
    @CsvSource({"org.apache.cxf,cxf-rt-transports-http,3.5.5", "org.springframework.ws,spring-ws-core,3.1.3"})
    void unrelatedLibrariesAreNotProviders(String group, String artifact, String version) {
        rewriteRun(pomXml(pom(api("compile") + dependency(group, artifact, version, "compile")),
          spec -> spec.after(after -> metro(after, "provided"))));
    }

    @Test
    void jakartaCxfDoesNotSatisfyLegacyApi() {
        rewriteRun(pomXml(pom(api("compile") + cxf("compile", "4.0.0")),
          spec -> spec.after(after -> metro(after, "provided"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"pom", "sources"})
    void nonRuntimeProviderArtifact(String artifactKind) {
        String provider = cxf("compile", "3.5.5");
        String element = "pom".equals(artifactKind) ? "type" : "classifier";
        provider = provider.replace("<version>3.5.5</version>", "<version>3.5.5</version>\n            <" + element + ">" + artifactKind + "</" + element + ">");
        rewriteRun(pomXml(pom(api("compile") + provider), spec -> spec.after(after -> metro(after, "provided"))));
    }

    @Test
    void emptyClassifierIsTheDefaultRuntimeJar() {
        String provider = cxf("compile", "3.5.5").replace("<scope>compile</scope>",
          "<scope>compile</scope><classifier></classifier>");
        rewriteRun(pomXml(pom(api("compile") + provider), spec -> spec.beforeRecipe(document -> {
            MavenResolutionResult model = document.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
            ResolvedDependency resolved = model.getDependencies().get(Scope.Compile).stream()
              .filter(d -> "org.apache.cxf".equals(d.getGroupId()) && "cxf-rt-frontend-jaxws".equals(d.getArtifactId()))
              .findFirst().orElseThrow();
            assertThat(resolved.getClassifier()).isEmpty();
            assertThat(resolved.getType()).isEqualTo("jar");
            assertThat(resolved.getVersion()).isEqualTo("3.5.5");
        })));
    }

    @Test
    void modernApiGuard() {
        rewriteRun(pomXml(pom(dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "3.0.1", "compile"))));
    }

    @Test
    void managedProviderIsNotOnClasspath() {
        String before = pom(api("compile")).replace("    <dependencies>", """
              <dependencyManagement>
                  <dependencies>
          %s        </dependencies>
              </dependencyManagement>
              <dependencies>""".formatted(dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "3.5.5", "compile").indent(4)));
        rewriteRun(pomXml(before, spec -> spec.after(after -> metro(after, "provided"))));
    }

    @Test
    void excludedProviderIsNotOnClasspath() {
        String starter = dependency("org.apache.cxf", "cxf-spring-boot-starter-jaxws", "3.5.5", "compile")
          .replace("        </dependency>", """
                      <exclusions>
                          <exclusion>
                              <groupId>org.apache.cxf</groupId>
                              <artifactId>cxf-rt-frontend-jaxws</artifactId>
                          </exclusion>
                      </exclusions>
                  </dependency>""");
        rewriteRun(pomXml(pom(api("compile") + starter), spec -> spec.after(after -> metro(after, "provided"))));
    }

    @Test
    void moduleIsolation() {
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          mavenProject("provider", pomXml(pom("provider", cxf("compile", "3.5.5")))),
          mavenProject("api", pomXml(pom("api", api("compile")), spec -> spec.after(after -> metro(after, "provided"))))
        );
    }

    private static String cxf(String scope, String version) {
        // Keep the API's scope/namespace under the fixture's control, independent of the provider's own API dependency.
        return dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", version, scope)
          .replace("        </dependency>", """
                      <exclusions>
                          <exclusion>
                              <groupId>jakarta.xml.ws</groupId>
                              <artifactId>jakarta.xml.ws-api</artifactId>
                          </exclusion>
                      </exclusions>
                  </dependency>""");
    }

    private static String api(String scope) {
        return dependency("jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3", scope);
    }

    private static String dependency(String group, String artifact, String version, String scope) {
        return """
                  <dependency>
                      <groupId>%s</groupId>
                      <artifactId>%s</artifactId>
                      <version>%s</version>
                      <scope>%s</scope>
                  </dependency>
          """.formatted(group, artifact, version, scope);
    }

    private static String pom(String dependencies) {
        return pom("app", dependencies);
    }

    private static String pom(String artifact, String dependencies) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>%s</artifactId>
              <version>1</version>
              <dependencies>
          %s    </dependencies>
          </project>
          """.formatted(artifact, dependencies);
    }

    private static String metro(String after, String scope) {
        List<Xml.Tag> dependencies = Xml.Tag.build(after).getChild("dependencies").orElseThrow().getChildren("dependency").stream()
          .filter(d -> "com.sun.xml.ws".equals(d.getChildValue("groupId").orElse("")) &&
                       "jaxws-rt".equals(d.getChildValue("artifactId").orElse("")))
          .collect(Collectors.toList());
        assertThat(dependencies).hasSize(1);
        assertThat(dependencies.get(0).getChildValue("scope")).contains(scope);
        assertThat(dependencies.get(0).getChildValue("version").orElseThrow()).startsWith("2.3.");
        return after;
    }

}
