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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.java.Assertions.*;

class KarafCommonAnnotationsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/add-common-annotations-dependencies.yml",
          "org.openrewrite.java.migrate.javax.AddCommonAnnotationsDependencies");
    }

    @ParameterizedTest
    @ValueSource(strings = {"compile", "test", "provided", "runtime"})
    void preserveCoordinateAndScopeForExternalHarness(String scope) {
        rewriteRun(pomXml(project("""
          <dependency>
            <groupId>org.apache.karaf.itests</groupId>
            <artifactId>common</artifactId>
            <version>4.4.11</version>
            <scope>test</scope>
          </dependency>
          """ + annotation("javax.annotation", "javax.annotation-api", "1.3.2", scope))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.1.1", "3.0.0"})
    void preserveModernAnnotationNamespaceAndScopes(String modernVersion) {
        rewriteRun(pomXml(project("""
          <dependency>
            <groupId>org.apache.karaf.itests</groupId>
            <artifactId>common</artifactId>
            <version>4.4.11</version>
            <scope>test</scope>
          </dependency>
          """ + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test") +
          annotation("jakarta.annotation", "jakarta.annotation-api", modernVersion, "compile"))));
    }

    @Test
    void unrelatedModuleStillMigratesLegacyCoordinate() {
        rewriteRun(pomXml(
          project(annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test")),
          project(annotation("jakarta.annotation", "jakarta.annotation-api", "1.3.5", "provided"))));
    }

    @Test
    void sourceAnnotationsDoNotAddAnEquivalentJakartaOneApi() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.annotation; public @interface Resource {}")),
          mavenProject("sample",
            pomXml(project("""
              <dependency>
                <groupId>org.apache.karaf.itests</groupId>
                <artifactId>common</artifactId>
                <version>4.4.11</version>
                <scope>test</scope>
              </dependency>
              """ + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "provided"))),
            srcMainJava(java("""
              import javax.annotation.Resource;
              @Resource class Example {}
              """))));
    }

    @Test
    void managedHarnessOnlyProtectsTheModuleThatUsesIt() {
        rewriteRun(mavenProject("suite",
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>suite</artifactId><version>1</version>
              <packaging>pom</packaging>
              <modules><module>harness</module><module>other</module></modules>
              <dependencyManagement><dependencies>
                <dependency>
                  <groupId>org.apache.karaf.itests</groupId><artifactId>common</artifactId><version>4.4.11</version>
                </dependency>
              </dependencies></dependencyManagement>
            </project>
            """),
          mavenProject("harness", pomXml(module("harness", """
            <dependency>
              <groupId>org.apache.karaf.itests</groupId><artifactId>common</artifactId><scope>test</scope>
            </dependency>
            """ + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test")))),
          mavenProject("other", pomXml(
            module("other", annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test")),
            module("other", annotation("jakarta.annotation", "jakarta.annotation-api", "1.3.5", "provided"))))));
    }

    @Test
    void sourceReactorCommonDoesNotImplyAnExternalBinaryContract() {
        rewriteRun(mavenProject("suite",
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>suite</artifactId><version>1</version>
              <packaging>pom</packaging>
              <modules><module>common</module><module>app</module></modules>
            </project>
            """),
          mavenProject("common", pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>example</groupId><artifactId>suite</artifactId><version>1</version></parent>
              <groupId>org.apache.karaf.itests</groupId><artifactId>common</artifactId><version>4.4.11</version>
            </project>
            """)),
          mavenProject("app", pomXml(
            module("app", harness() + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test")),
            module("app", harness() + annotation("jakarta.annotation", "jakarta.annotation-api", "1.3.5", "provided"))))));
    }

    @Test
    void unverifiedHarnessVersionDoesNotDisableMigration() {
        String common = harness().replace("4.4.11", "4.4.10");
        rewriteRun(pomXml(
          project(common + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test")),
          project(common + annotation("jakarta.annotation", "jakarta.annotation-api", "1.3.5", "provided"))));
    }

    @Test
    void transitiveExternalHarnessIsResolvedWithinTheConsumingModule() {
        rewriteRun(mavenProject("suite",
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>suite</artifactId><version>1</version>
              <packaging>pom</packaging>
              <modules><module>wrapper</module><module>app</module></modules>
            </project>
            """),
          mavenProject("wrapper", pomXml(module("wrapper", harness().replace("<scope>test</scope>", "<scope>compile</scope>")))),
          mavenProject("app", pomXml(module("app", """
            <dependency>
              <groupId>example</groupId><artifactId>wrapper</artifactId><version>1</version><scope>test</scope>
            </dependency>
            """ + annotation("javax.annotation", "javax.annotation-api", "1.3.2", "test"))))));
    }

    private static String harness() {
        return """
          <dependency>
            <groupId>org.apache.karaf.itests</groupId>
            <artifactId>common</artifactId>
            <version>4.4.11</version>
            <scope>test</scope>
          </dependency>
          """;
    }

    private static String module(String artifact, String dependencies) {
        return """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <parent><groupId>example</groupId><artifactId>suite</artifactId><version>1</version></parent>
            <artifactId>%s</artifactId>
            <dependencies>
          """.formatted(artifact) + dependencies.indent(4) + "  </dependencies>\n</project>\n";
    }

    private static String project(String dependencies) {
        return """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>example</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <dependencies>
          """ + dependencies.indent(4) + "  </dependencies>\n</project>\n";
    }

    private static String annotation(String group, String artifact, String version, String scope) {
        return """
          <dependency>
            <groupId>%s</groupId>
            <artifactId>%s</artifactId>
            <version>%s</version>
            <scope>%s</scope>
          </dependency>
          """.formatted(group, artifact, version, scope);
    }
}
