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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.config.Environment;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.java.JavaParser;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import java.util.Collections;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.java.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class JaxwsNamespaceTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
          .build().activateRecipes("org.openrewrite.java.migrate.javax.AddJaxwsDependencies"));
    }

    @ParameterizedTest
    @CsvSource({"implementation,3.0.1","implementation,4.0.2","compileOnly,4.0.2","runtimeOnly,4.0.2","testImplementation,4.0.2"})
    void retainJavaxCoordinateBesideTransitiveJakarta(String scope, String version) {
        ResolvedDependency modern = dependency("jakarta.xml.ws", "jakarta.xml.ws-api", version).withDepth(1);
        ResolvedDependency cxf = dependency("org.apache.cxf", "cxf-rt-frontend-jaxws", "4.1.2")
          .withDependencies(Collections.singletonList(modern));
        GradleDependencyConfiguration configuration = new GradleDependencyConfiguration(
          "compileClasspath", null, true, true, false, false, Collections.emptyList(),
          Collections.emptyList(), Collections.singletonList(cxf), null, null);
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              %s 'javax.xml.ws:jaxws-api:2.3.1'
          }
          """.formatted(scope), spec -> spec.markers(GradleProject.builder().name("app").path(":app")
            .nameToConfiguration(Collections.singletonMap("compileClasspath", configuration)).build())));
    }

    @Test
    void retainBothMavenNamespaces() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>example</groupId>
            <artifactId>app</artifactId>
            <version>1</version>
            <dependencies>
              <dependency>
                <groupId>jakarta.xml.ws</groupId>
                <artifactId>jakarta.xml.ws-api</artifactId>
                <version>4.0.2</version>
              </dependency>
              <dependency>
                <groupId>javax.xml.ws</groupId>
                <artifactId>jaxws-api</artifactId>
                <version>2.3.1</version>
                <scope>provided</scope>
              </dependency>
            </dependencies>
          </project>
          """));
    }


    @ParameterizedTest
    @CsvSource({
      "jakarta.xml.ws,jakarta.xml.ws-api,2.3.3,false",
      "jakarta.xml.ws,jakarta.xml.ws-api,3.0.1,true",
      "jakarta.xml.ws,jakarta.xml.ws-api,4.0.2,true",
      "jakarta.jws,jakarta.jws-api,2.1.0,false",
      "jakarta.jws,jakarta.jws-api,3.0.0,true",
      "jakarta.xml.ws,unrelated,4.0.2,false",
      "unrelated,jakarta.xml.ws-api,4.0.2,false"
    })
    void distinguishNamespaces(String group, String artifact, String version, boolean modern) {
        GradleDependencyConfiguration configuration = new GradleDependencyConfiguration(
          "compileClasspath", null, true, true, false, false, Collections.emptyList(),
          Collections.emptyList(), Collections.singletonList(dependency(group, artifact, version)), null, null);
        GradleProject model = GradleProject.builder().name("app").path(":app")
          .nameToConfiguration(Collections.singletonMap("compileClasspath", configuration)).build();
        SourceFile source = GradleParser.builder().build().parse("plugins { id 'java' }").findFirst().orElseThrow();
        source = source.withMarkers(source.getMarkers().add(model));
        Tree selected = new JaxwsNamespace(modern).getVisitor().visit(source, new InMemoryExecutionContext());
        Tree rejected = new JaxwsNamespace(!modern).getVisitor().visit(source, new InMemoryExecutionContext());
        assertThat(selected.getMarkers().findFirst(SearchResult.class)).isPresent();
        assertThat(rejected).isSameAs(source);
    }

    @Test
    void retainJavaxCoordinateWithManagedModernApi() {
        rewriteRun(pomXml("""
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
                  <version>4.0.2</version>
                </dependency>
              </dependencies>
            </dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>javax.xml.ws</groupId>
                <artifactId>jaxws-api</artifactId>
                <version>2.3.1</version>
                <scope>provided</scope>
              </dependency>
            </dependencies>
          </project>
          """));
    }

    @ParameterizedTest
    @CsvSource({
      "javax.xml.ws,jaxws-api,2.3.1,javax.xml.ws.http.HTTPException",
      "javax.jws,javax.jws-api,1.1,javax.jws.WebService"
    })
    void supplyJavaxApiWithoutReplacingModernApi(String group, String artifact, String version, String type) {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.xml.ws.http; public class HTTPException extends RuntimeException {}",
            "package javax.jws; public @interface WebService {}")),
          mavenProject("app", srcMainJava(java("""
            class Example {
                %s error;
            }
            """.formatted(type))), pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>4.0.2</version>
                </dependency>
              </dependencies>
            </project>
            """, """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>4.0.2</version>
                </dependency>
                <dependency>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </dependency>
              </dependencies>
            </project>
            """.formatted(group, artifact, version))));
    }


    @ParameterizedTest
    @CsvSource({
      "jakarta.xml.ws,jakarta.xml.ws-api,4.0.2,jakarta.jws,jakarta.jws-api,2.1.0,javax.jws.WebService",
      "jakarta.jws,jakarta.jws-api,3.0.0,jakarta.xml.ws,jakarta.xml.ws-api,2.3.3,javax.xml.ws.http.HTTPException"
    })
    void doNotDuplicateAnEquivalentLegacyApi(String modernGroup, String modernArtifact, String modernVersion,
                                             String legacyGroup, String legacyArtifact, String legacyVersion, String type) {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.xml.ws.http; public class HTTPException extends RuntimeException {}",
            "package javax.jws; public @interface WebService {}")),
          mavenProject("app", srcMainJava(java("""
            class Example {
                %s legacy;
            }
            """.formatted(type))), pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
              <dependencies>
                <dependency>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </dependency>
                <dependency>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </dependency>
              </dependencies>
            </project>
            """.formatted(modernGroup, modernArtifact, modernVersion, legacyGroup, legacyArtifact, legacyVersion))));
    }

    private static ResolvedDependency dependency(String group, String artifact, String version) {
        return ResolvedDependency.builder().gav(new ResolvedGroupArtifactVersion(null, group, artifact, version, null))
          .requested(Dependency.builder().gav(new GroupArtifactVersion(group, artifact, version)).build()).type("jar").build();
    }
}
