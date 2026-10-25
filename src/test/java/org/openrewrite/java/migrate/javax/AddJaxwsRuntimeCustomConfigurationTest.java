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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

class AddJaxwsRuntimeCustomConfigurationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddJaxwsRuntime()).beforeRecipe(withToolingApi());
    }

    @Test
    void missingCustomSourceSetProvider() {
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          buildGradle(
            """
              apply plugin: 'java'
              repositories { mavenCentral() }
              sourceSets { integrationTest {} }
              dependencies {
                  compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.7'
                  integrationTestImplementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              }
              """,
            spec -> spec.after(after -> {
                assertThat(after).containsPattern("(?m)^\\s*integrationTestImplementation ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]");
                assertThat(after).contains("compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.7'");
                return after;
            })
          )
        );
    }

    @ParameterizedTest
    @CsvSource({"org.apache.cxf:cxf-rt-frontend-jaxws:3.5.5", "com.sun.xml.ws:jaxws-rt:2.3.7"})
    void customProviderCoversCustomApi(String provider) {
        rewriteRun(buildGradle("""
          apply plugin: 'java'
          repositories { mavenCentral() }
          sourceSets { integrationTest {} }
          dependencies {
              compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.7'
              integrationTestImplementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              integrationTestRuntimeOnly '%s'
          }
          """.formatted(provider)));
    }

    @Test
    void customApiConfigurationFeedsCoveredStandardClasspaths() {
        rewriteRun(buildGradle("""
          apply plugin: 'java'
          repositories { mavenCentral() }
          configurations { soapApi }
          configurations.implementation.extendsFrom(configurations.soapApi)
          dependencies {
              soapApi 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              runtimeOnly 'org.apache.cxf:cxf-rt-frontend-jaxws:3.5.5'
          }
          """));
    }

    @Test
    void customApiConfigurationFeedsUncoveredStandardClasspaths() {
        rewriteRun(buildGradle("""
          apply plugin: 'java'
          repositories { mavenCentral() }
          configurations { soapApi }
          configurations.implementation.extendsFrom(configurations.soapApi)
          dependencies {
              soapApi 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
          }
          """, spec -> spec.after(after -> {
            assertThat(after).containsPattern("(?m)^\\s*compileOnly ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]");
            assertThat(after).containsPattern("(?m)^\\s*testImplementation ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]");
            assertThat(after).doesNotContain("soapApi \"com.sun.xml.ws", "soapApi 'com.sun.xml.ws");
            return after;
        })));
    }

    @Test
    void customProviderConfigurationFeedsStandardRuntime() {
        rewriteRun(buildGradle("""
          apply plugin: 'java'
          repositories { mavenCentral() }
          configurations { soapProvider }
          configurations.runtimeOnly.extendsFrom(configurations.soapProvider)
          dependencies {
              implementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              soapProvider 'org.apache.cxf:cxf-rt-frontend-jaxws:3.5.5'
          }
          """));
    }

    @Test
    void sharedApiBucketDoesNotPolluteCoveredMainRuntime() {
        rewriteRun(buildGradle("""
          apply plugin: 'java'
          repositories { mavenCentral() }
          sourceSets { integrationTest {} }
          configurations { soapApi }
          configurations.implementation.extendsFrom(configurations.soapApi)
          configurations.integrationTestImplementation.extendsFrom(configurations.soapApi)
          dependencies {
              soapApi 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
              implementation 'org.apache.cxf:cxf-rt-frontend-jaxws:3.5.5'
          }
          """, spec -> spec.after(after -> {
            assertThat(after).containsPattern("(?m)^\\s*integrationTestImplementation ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]");
            assertThat(after).doesNotContain("soapApi \"com.sun.xml.ws", "soapApi 'com.sun.xml.ws", "implementation \"com.sun.xml.ws", "testImplementation \"com.sun.xml.ws");
            return after;
        })));
    }

    @Test
    void customApiInAppliedScript() {
        rewriteRun(
          buildGradle("""
            apply plugin: 'java'
            repositories { mavenCentral() }
            sourceSets { integrationTest {} }
            dependencies {
                compileOnly 'com.sun.xml.ws:jaxws-rt:2.3.7'
            }
            apply from: 'dependencies.gradle'
            """, spec -> spec.after(after -> {
                assertThat(after).containsPattern("(?m)^\\s*integrationTestImplementation ['\"]com\\.sun\\.xml\\.ws:jaxws-rt:2\\.3\\.\\d+['\"]");
                return after;
            })),
          buildGradle("""
            dependencies {
                integrationTestImplementation 'jakarta.xml.ws:jakarta.xml.ws-api:2.3.3'
            }
            """, spec -> spec.path("dependencies.gradle"))
        );
    }

}
