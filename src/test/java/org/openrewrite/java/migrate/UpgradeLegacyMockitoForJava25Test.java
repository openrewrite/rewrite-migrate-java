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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeLegacyMockitoForJava25Test implements RewriteTest {
    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>org.example</groupId>
          <artifactId>example</artifactId>
          <version>1</version>
          <dependencies>
              <dependency>
                  <groupId>org.mockito</groupId>
                  <artifactId>mockito-all</artifactId>
                  <version>1.10.19</version>
                  <scope>test</scope>
              </dependency>
          </dependencies>
      </project>
      """;

    private static String mockitoCorePom(String version) {
        return POM.replace("mockito-all", "mockito-core").replace("1.10.19", version);
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "mockito-all-1.10.19"));
    }

    private static final String TRANSITIVE_POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <parent>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-parent</artifactId>
              <version>1.4.0.RELEASE</version>
              <relativePath/>
          </parent>
          <groupId>org.example</groupId>
          <artifactId>example</artifactId>
          <version>1</version>
          <dependencies>
              <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
              </dependency>
          </dependencies>
      </project>
      """;

    @Test
    void excludeTransitiveOnlyLegacyMockitoFromApiMigration() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeMockito1ForJava25"),
          mavenProject("example",
            pomXml(TRANSITIVE_POM),
            srcTestJava(java("""
              import static org.mockito.Matchers.eq;

              class ExampleTest {
                  String match(String value) {
                      return eq(value);
                  }
              }
              """))
          )
        );
    }

    @Test
    void compositeLeavesTransitiveLegacyMockitoApisAndAgentAlone() {
        rewriteRun(
          mavenProject("example",
            pomXml(TRANSITIVE_POM),
            srcTestJava(java("""
              import static org.mockito.Matchers.eq;

              class ExampleTest {
                  String match(String value) {
                      return eq(value);
                  }
              }
              """))
          )
        );
    }

    @Test
    void newlyAddedSurefireOverridesLegacyParentVersion() {
        rewriteRun(pomXml(
          TRANSITIVE_POM.replace("    </dependencies>", """
                        <dependency>
                            <groupId>org.mockito</groupId>
                            <artifactId>mockito-core</artifactId>
                            <version>5.20.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>"""),
          spec -> spec.after(pom -> {
              assertThat(pom).containsPattern("(?s)<artifactId>maven-surefire-plugin</artifactId>\\s*<version>3\\.5\\.[^<]+</version>")
                .contains("-javaagent:${org.mockito:mockito-core:jar}")
                .contains("<version>1.4.0.RELEASE</version>");
              return pom;
          })
        ));
    }

    @Test
    void replaceObsoleteAggregateAndMigrateRemovedMatcherApi() {
        rewriteRun(
          mavenProject("example",
            pomXml(POM, spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("<artifactId>mockito-all</artifactId>")
              .contains("<artifactId>mockito-core</artifactId>")
              .containsPattern("<version>5\\.\\d+.*</version>").actual())),
            srcTestJava(java(
              """
                import static org.mockito.Matchers.eq;

                class ExampleTest {
                    String match(String value) {
                        return eq(value);
                    }
                }
                """,
              """
                import static org.mockito.ArgumentMatchers.eq;

                class ExampleTest {
                    String match(String value) {
                        return eq(value);
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void migrateRemovedMatcherApiWhenUpgradingMockito3() {
        rewriteRun(
          mavenProject("example",
            pomXml(mockitoCorePom("3.0.0"),
              spec -> spec.after(pom -> assertThat(pom)
                .containsPattern("<version>5\\.\\d+.*</version>").actual())),
            srcTestJava(java(
              """
                import java.util.List;

                import static org.mockito.Matchers.anyListOf;
                import static org.mockito.Matchers.eq;

                class ExampleTest {
                    String match(String value) {
                        return eq(value);
                    }

                    List<String> matchList() {
                        return anyListOf(String.class);
                    }
                }
                """,
              """
                import java.util.List;

                import static org.mockito.ArgumentMatchers.anyList;
                import static org.mockito.ArgumentMatchers.eq;

                class ExampleTest {
                    String match(String value) {
                        return eq(value);
                    }

                    List<String> matchList() {
                        return anyList();
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void migrateRemovedMatcherApiWhenUpgradingMockito2() {
        rewriteRun(
          mavenProject("example",
            pomXml(mockitoCorePom("2.8.47"),
              spec -> spec.after(pom -> assertThat(pom)
                .containsPattern("<version>5\\.\\d+.*</version>").actual())),
            srcTestJava(java(
              """
                import static org.mockito.Matchers.any;

                class ExampleTest {
                    Object match() {
                        return any();
                    }
                }
                """,
              """
                import static org.mockito.ArgumentMatchers.any;

                class ExampleTest {
                    Object match() {
                        return any();
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void keepAnyClassMatcherWhenUpgradingMockito3() {
        rewriteRun(
          mavenProject("example",
            pomXml(mockitoCorePom("3.0.0"),
              spec -> spec.after(pom -> assertThat(pom)
                .containsPattern("<version>5\\.\\d+.*</version>").actual())),
            srcTestJava(java(
              """
                import static org.mockito.Matchers.any;

                class ExampleTest {
                    String match() {
                        return any(String.class);
                    }
                }
                """,
              """
                import static org.mockito.ArgumentMatchers.any;

                class ExampleTest {
                    String match() {
                        return any(String.class);
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void preserveModernMatcherNullSemantics() {
        rewriteRun(
          mavenProject("example",
            pomXml(mockitoCorePom("5.20.0"),
              spec -> spec.after(pom -> assertThat(pom).contains("<artifactId>mockito-core</artifactId>").actual())),
            srcTestJava(java(
              """
                import static org.mockito.Mockito.any;

                class ExampleTest {
                    String match() {
                        return any(String.class);
                    }
                }
                """
            ))
          )
        );
    }

    @Test
    void leaveBuildWithoutMockitoAlone() {
        rewriteRun(pomXml("""
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.example</groupId>
              <artifactId>example</artifactId>
              <version>1</version>
          </project>
          """));
    }
}
