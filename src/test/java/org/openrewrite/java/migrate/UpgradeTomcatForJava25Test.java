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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeTomcatForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeTomcatForJava25");
    }

    @ParameterizedTest
    @ValueSource(strings = {"7.0.109", "8.5.100", "9.0.124"})
    void leavesOtherTomcatLinesAlone(String version) {
        rewriteRun(pomXml(project(version, "", "")));
    }

    @Test
    void preservesBootParentAndItsTomcatVersionPin() {
        rewriteRun(pomXml(project("${tomcat.version}",
          """
            <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>1.3.8.RELEASE</version>
            </parent>
            """, "")));
    }

    @Test
    void preservesBootBomAndStarterEvenWithAnExplicitContainerVersion() {
        rewriteRun(
          pomXml(project("8.0.36",
            """
              <dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-dependencies</artifactId>
                          <version>1.3.8.RELEASE</version>
                          <type>pom</type>
                          <scope>import</scope>
                      </dependency>
                  </dependencies>
              </dependencyManagement>
              """, "")),
          pomXml(project("8.0.36", "",
            """
              <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-tomcat</artifactId>
                  <version>1.3.8.RELEASE</version>
              </dependency>
              """), spec -> spec.path("starter/pom.xml"))
        );
    }

    @Test
    void preservesTomcatWhenEffectiveManagementContainsBootDependencies() {
        rewriteRun(pomXml(project("8.0.36",
          """
            <dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot</artifactId>
                        <version>1.5.1.RELEASE</version>
                    </dependency>
                </dependencies>
            </dependencyManagement>
            """, "")));
    }

    @Test
    void leavesStandaloneJdbcAtItsExplicitVersion() {
        rewriteRun(pomXml(project("8.0.36", "",
          """
            <dependency>
                <groupId>org.apache.tomcat</groupId>
                <artifactId>tomcat-jdbc</artifactId>
                <version>8.0.36</version>
            </dependency>
            """),
          spec -> spec.after(actual -> assertThat(actual)
            .containsPattern("<artifactId>tomcat-embed-core</artifactId>\\s*<version>9\\.0\\.\\d+</version>")
            .containsPattern("<artifactId>tomcat-jdbc</artifactId>\\s*<version>8\\.0\\.36</version>")
            .actual())));
    }

    @Test
    void preservesVersionPropertySharedWithJdbc() {
        rewriteRun(pomXml(project("${tomcat.version}", "",
          """
            <dependency>
                <groupId>org.apache.tomcat</groupId>
                <artifactId>tomcat-jdbc</artifactId>
                <version>${tomcat.version}</version>
            </dependency>
            """)));
    }

    private static String project(String version, String extraConfig, String extraDependency) {
        return """
          <project>
              <groupId>example</groupId>
              <artifactId>embedded</artifactId>
              <version>1</version>
              <properties><tomcat.version>8.0.36</tomcat.version></properties>
              %s
              <dependencies>
                  <dependency>
                      <groupId>org.apache.tomcat.embed</groupId>
                      <artifactId>tomcat-embed-core</artifactId>
                      <version>%s</version>
                  </dependency>
                  %s
              </dependencies>
          </project>
          """.formatted(extraConfig.stripTrailing(), version, extraDependency.stripTrailing());
    }
}
