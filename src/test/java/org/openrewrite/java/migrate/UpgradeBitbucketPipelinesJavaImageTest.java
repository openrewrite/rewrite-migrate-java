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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class UpgradeBitbucketPipelinesJavaImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeBitbucketPipelinesJavaImage(25));
    }

    @DocumentExample
    @Test
    void upgradeDefaultAndStepImages() {
        rewriteRun(
          yaml(
            """
              image: maven:3.9-eclipse-temurin-17
              pipelines:
                default:
                  - step:
                      name: Build
                      script:
                        - mvn verify
                  - step:
                      name: Package
                      image: openjdk:8
                      script:
                        - ./package.sh
              """,
            """
              image: maven:3.9-eclipse-temurin-25
              pipelines:
                default:
                  - step:
                      name: Build
                      script:
                        - mvn verify
                  - step:
                      name: Package
                      image: eclipse-temurin:25
                      script:
                        - ./package.sh
              """,
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @CsvSource({
      "maven:3.9-eclipse-temurin-17, maven:3.9-eclipse-temurin-25",
      "maven:3.9.9-eclipse-temurin-21-alpine, maven:3.9-eclipse-temurin-25-alpine",
      "openjdk:8, eclipse-temurin:25",
      "openjdk:11-jdk, eclipse-temurin:25-jdk",
      // The OpenJDK based Maven images are no longer published, so move to the eclipse-temurin based ones
      "maven:3-jdk-8, maven:3.9-eclipse-temurin-25",
      "maven:3.6.3-jdk-11, maven:3.9-eclipse-temurin-25",
      // Gradle 8 can not run on Java 25, and no gradle:8-jdk25 image is published
      "gradle:8-jdk17, gradle:9-jdk25",
      "gradle:8.5-jdk21-alpine, gradle:9.1-jdk25-alpine",
      "gradle:9-jdk21, gradle:9-jdk25",
      "gradle:jdk17, gradle:jdk25",
      "docker.io/eclipse-temurin:17-jdk@sha256:1234567890abcdef, docker.io/eclipse-temurin:25-jdk",
    })
    @ParameterizedTest
    void upgradeImage(String before, String after) {
        rewriteRun(
          yaml(
            "image: %s".formatted(before),
            "image: %s".formatted(after),
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @CsvSource({
      "gradle:8-jdk17, 21, gradle:8-jdk21",
      "gradle:7-jdk11, 21, gradle:8-jdk21",
      "gradle:8.4.1-jdk17, 21, gradle:8.5.0-jdk21",
      "gradle:8.10-jdk17, 21, gradle:8.10-jdk21",
    })
    @ParameterizedTest
    void upgradeGradleOnlyWhereItCanNotRunOnTheNewJavaVersion(String before, int version, String after) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeBitbucketPipelinesJavaImage(version)),
          yaml(
            "image: %s".formatted(before),
            "image: %s".formatted(after),
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @Test
    void upgradeNameOfImageInObjectForm() {
        rewriteRun(
          yaml(
            """
              image:
                name: openjdk:17-jdk
              pipelines:
                default:
                  - step:
                      image:
                        name: eclipse-temurin:21
                        run-as-user: 1000
                      script:
                        - ./mvnw verify
              """,
            """
              image:
                name: eclipse-temurin:25-jdk
              pipelines:
                default:
                  - step:
                      image:
                        name: eclipse-temurin:25
                        run-as-user: 1000
                      script:
                        - ./mvnw verify
              """,
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @Test
    void upgradeNestedStepImages() {
        rewriteRun(
          yaml(
            """
              pipelines:
                branches:
                  main:
                    - parallel:
                        steps:
                          - step:
                              image: eclipse-temurin:17
                              script:
                                - ./gradlew test
                          - step:
                              image: node:20
                              script:
                                - npm test
                    - stage:
                        name: Deploy
                        steps:
                          - step:
                              image: amazoncorretto:17-al2023
                              script:
                                - ./deploy.sh
                    - final:
                        image: eclipse-temurin:17-jre
                        script:
                          - ./notify.sh
                pull-requests:
                  '**':
                    - parallel:
                        - step:
                            image: maven:3.9-eclipse-temurin-21
                            script:
                              - mvn verify
                custom:
                  release:
                    - variables:
                        - name: VERSION
                    - step:
                        image: gradle:8-jdk17
                        script:
                          - gradle publish
              """,
            """
              pipelines:
                branches:
                  main:
                    - parallel:
                        steps:
                          - step:
                              image: eclipse-temurin:25
                              script:
                                - ./gradlew test
                          - step:
                              image: node:20
                              script:
                                - npm test
                    - stage:
                        name: Deploy
                        steps:
                          - step:
                              image: amazoncorretto:25-al2023
                              script:
                                - ./deploy.sh
                    - final:
                        image: eclipse-temurin:25-jre
                        script:
                          - ./notify.sh
                pull-requests:
                  '**':
                    - parallel:
                        - step:
                            image: maven:3.9-eclipse-temurin-25
                            script:
                              - mvn verify
                custom:
                  release:
                    - variables:
                        - name: VERSION
                    - step:
                        image: gradle:9-jdk25
                        script:
                          - gradle publish
              """,
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @Test
    void upgradeStepDefinedAsAnchor() {
        rewriteRun(
          yaml(
            """
              definitions:
                steps:
                  - step: &build
                      name: Build
                      image: maven:3.9-eclipse-temurin-17
                      script:
                        - mvn verify
              pipelines:
                default:
                  - step: *build
              """,
            """
              definitions:
                steps:
                  - step: &build
                      name: Build
                      image: maven:3.9-eclipse-temurin-25
                      script:
                        - mvn verify
              pipelines:
                default:
                  - step: *build
              """,
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @Test
    void upgradeJavaServiceImageOnly() {
        rewriteRun(
          yaml(
            """
              definitions:
                services:
                  postgres:
                    image: postgres:16
                    variables:
                      POSTGRES_PASSWORD: secret
                  wiremock:
                    image: eclipse-temurin:17-jre
                  docker:
                    memory: 2048
              """,
            """
              definitions:
                services:
                  postgres:
                    image: postgres:16
                    variables:
                      POSTGRES_PASSWORD: secret
                  wiremock:
                    image: eclipse-temurin:25-jre
                  docker:
                    memory: 2048
              """,
            spec -> spec.path("bitbucket-pipelines.yml")
          )
        );
    }

    @Nested
    class NoChange {

        @ParameterizedTest
        @ValueSource(strings = {
          // Images already at or beyond the target version
          "eclipse-temurin:25-jdk",
          "eclipse-temurin:26",
          "maven:3.9-eclipse-temurin-25",
          "gradle:9-jdk25",
          // Images that are not Java images, or without a Java version in their tag
          "atlassian/default-image:4",
          "node:20",
          "eclipse-temurin:latest",
          "openjdk",
          // Tags a newer Java version is not published for
          "maven:3-jdk-8-slim",
          "maven:3.8-openjdk-17",
        })
        void unrelatedImages(String image) {
            rewriteRun(
              yaml(
                "image: %s".formatted(image),
                spec -> spec.path("bitbucket-pipelines.yml")
              )
            );
        }

        @Test
        void pipes() {
            rewriteRun(
              yaml(
                """
                  pipelines:
                    default:
                      - step:
                          script:
                            - pipe: atlassian/maven-deploy:1.0.0
                            - pipe: docker://openjdk:17
                              variables:
                                image: openjdk:17
                  """,
                spec -> spec.path("bitbucket-pipelines.yml")
              )
            );
        }

        @Test
        void otherYamlFiles() {
            rewriteRun(
              yaml(
                """
                  image: maven:3.9-eclipse-temurin-17
                  pipelines:
                    default:
                      - step:
                          image: openjdk:8
                  """,
                spec -> spec.path("ci/bitbucket-pipelines.yml")
              ),
              yaml(
                """
                  image: maven:3.9-eclipse-temurin-17
                  """,
                spec -> spec.path("pipelines.yml")
              )
            );
        }
    }
}
