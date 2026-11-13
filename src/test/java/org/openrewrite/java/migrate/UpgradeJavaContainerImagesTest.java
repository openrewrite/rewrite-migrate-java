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

class UpgradeJavaContainerImagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeJavaContainerImages(25));
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
          spec -> spec.recipe(new UpgradeJavaContainerImages(version)),
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

    @Nested
    class GitLab {

        @Test
        void upgradeGlobalDefaultJobAndServiceImages() {
            rewriteRun(
              yaml(
                """
                  image: openjdk:17
                  default:
                    image: eclipse-temurin:17-jdk
                  build:
                    image:
                      name: gradle:8-jdk17
                      entrypoint: [""]
                    services:
                      - postgres:16
                      - name: eclipse-temurin:17-jre
                        alias: stub
                    script:
                      - gradle build
                  """,
                """
                  image: eclipse-temurin:25
                  default:
                    image: eclipse-temurin:25-jdk
                  build:
                    image:
                      name: gradle:9-jdk25
                      entrypoint: [""]
                    services:
                      - postgres:16
                      - name: eclipse-temurin:25-jre
                        alias: stub
                    script:
                      - gradle build
                  """,
                spec -> spec.path(".gitlab-ci.yml")
              )
            );
        }
        @Test
        void upgradeImageFamilies() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaContainerImages(21)),
              yaml(
                """
                  image: eclipse-temurin:17
                  default:
                    image: registry.example.com/library/eclipse-temurin:17-jdk-alpine
                  corretto:
                    image: amazoncorretto:17-alpine
                    script: java -version
                  maven:
                    image: maven:3.9.6-eclipse-temurin-17
                    script: mvn verify
                  maven-corretto:
                    image:
                      name: maven:3-amazoncorretto-17-debian
                    script: mvn verify
                  gradle:
                    image: gradle:8.10-jdk17
                    script: gradle build
                  gradle-alpine:
                    image: gradle:jdk11-alpine
                    script: gradle build
                  """,
                """
                  image: eclipse-temurin:21
                  default:
                    image: registry.example.com/library/eclipse-temurin:21-jdk-alpine
                  corretto:
                    image: amazoncorretto:21-alpine
                    script: java -version
                  maven:
                    image: maven:3.9-eclipse-temurin-21
                    script: mvn verify
                  maven-corretto:
                    image:
                      name: maven:3-amazoncorretto-21-debian
                    script: mvn verify
                  gradle:
                    image: gradle:8.10-jdk21
                    script: gradle build
                  gradle-alpine:
                    image: gradle:jdk21-alpine
                    script: gradle build
                  """,
                spec -> spec.path(".gitlab-ci.yml")
              )
            );
        }

        @Test
        void upgradeAnchoredImageOnce() {
            rewriteRun(
              yaml(
                """
                  .jdk: &jdk maven:3-eclipse-temurin-17
                  .runtime: &runtime
                    name: eclipse-temurin:17-jre
                  build:
                    image: *jdk
                    script: mvn verify
                  test:
                    image: *jdk
                    services:
                      - *runtime
                    script: mvn test
                  """,
                """
                  .jdk: &jdk maven:3-eclipse-temurin-25
                  .runtime: &runtime
                    name: eclipse-temurin:25-jre
                  build:
                    image: *jdk
                    script: mvn verify
                  test:
                    image: *jdk
                    services:
                      - *runtime
                    script: mvn test
                  """,
                spec -> spec.path(".gitlab-ci.yml")
              )
            );
        }

        @Test
        void leaveImagesWithoutAJavaVersionAlone() {
            rewriteRun(
              yaml(
                """
                  .unrelated: &unrelated eclipse-temurin:17
                  build:
                    image: maven:3.9.6
                    script: mvn verify
                  test:
                    image: eclipse-temurin
                    script: ./mvnw test
                  lint:
                    image: node:18
                    script: npm run lint
                  """,
                spec -> spec.path(".gitlab-ci.yml")
              )
            );
        }
    }

    @Nested
    class GitHubActions {

        @Test
        void upgradeJobContainersButNotServices() {
            rewriteRun(
              yaml(
                """
                  jobs:
                    build:
                      container: maven:3.9-eclipse-temurin-17
                    test:
                      container:
                        image: eclipse-temurin:21-jdk
                      services:
                        stub:
                          image: eclipse-temurin:17-jre
                  """,
                """
                  jobs:
                    build:
                      container: maven:3.9-eclipse-temurin-25
                    test:
                      container:
                        image: eclipse-temurin:25-jdk
                      services:
                        stub:
                          image: eclipse-temurin:17-jre
                  """,
                spec -> spec.path(".github/workflows/ci.yaml")
              )
            );
        }
    }

    @Nested
    class CircleCi {

        @Test
        void upgradePrimaryImageOfJobsAndExecutors() {
            rewriteRun(
              yaml(
                """
                  version: 2.1
                  executors:
                    jdk:
                      docker:
                        - image: cimg/openjdk:17.0-browsers
                  jobs:
                    test:
                      docker:
                        - image: eclipse-temurin:17
                        - image: eclipse-temurin:17
                  """,
                """
                  version: 2.1
                  executors:
                    jdk:
                      docker:
                        - image: cimg/openjdk:25.0-browsers
                  jobs:
                    test:
                      docker:
                        - image: eclipse-temurin:25
                        - image: eclipse-temurin:17
                  """,
                spec -> spec.path(".circleci/config.yml")
              )
            );
        }
    }

    @Nested
    class AzurePipelines {

        @Test
        void upgradePipelineJobAndResourceContainers() {
            rewriteRun(
              yaml(
                """
                  resources:
                    containers:
                      - container: jdk
                        image: openjdk:17-jdk
                      - container: db
                        image: postgres:16
                  container: maven:3.9-eclipse-temurin-17
                  stages:
                    - stage: Build
                      jobs:
                        - job: Compile
                          container:
                            image: eclipse-temurin:21
                        - job: Test
                          container: jdk
                  """,
                """
                  resources:
                    containers:
                      - container: jdk
                        image: eclipse-temurin:25-jdk
                      - container: db
                        image: postgres:16
                  container: maven:3.9-eclipse-temurin-25
                  stages:
                    - stage: Build
                      jobs:
                        - job: Compile
                          container:
                            image: eclipse-temurin:25
                        - job: Test
                          container: jdk
                  """,
                spec -> spec.path("azure-pipelines.yml")
              )
            );
        }
    }

    @Nested
    class Kubernetes {

        @Test
        void upgradeContainersAndInitContainers() {
            rewriteRun(
              yaml(
                """
                  apiVersion: batch/v1
                  kind: CronJob
                  metadata:
                    name: report
                  spec:
                    schedule: "0 * * * *"
                    jobTemplate:
                      spec:
                        template:
                          spec:
                            initContainers:
                              - name: migrate
                                image: registry.example.com/openjdk:17-jre
                            containers:
                              - name: report
                                image: eclipse-temurin:17-jre-alpine
                              - name: proxy
                                image: envoyproxy/envoy:v1.31
                  """,
                """
                  apiVersion: batch/v1
                  kind: CronJob
                  metadata:
                    name: report
                  spec:
                    schedule: "0 * * * *"
                    jobTemplate:
                      spec:
                        template:
                          spec:
                            initContainers:
                              - name: migrate
                                image: registry.example.com/eclipse-temurin:25-jre
                            containers:
                              - name: report
                                image: eclipse-temurin:25-jre-alpine
                              - name: proxy
                                image: envoyproxy/envoy:v1.31
                  """,
                spec -> spec.path("deploy/k8s/cronjob.yaml")
              ),
              yaml(
                """
                  apiVersion: v1
                  kind: Pod
                  metadata:
                    name: app
                  spec:
                    containers:
                      - name: app
                        image: amazoncorretto:17
                  """,
                """
                  apiVersion: v1
                  kind: Pod
                  metadata:
                    name: app
                  spec:
                    containers:
                      - name: app
                        image: amazoncorretto:25
                  """,
                spec -> spec.path("pod.yml")
              )
            );
        }
    }

    @Test
    void upgradeImagesAtACustomLocation() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaContainerImage(25, "**/compose.yaml", "$.services.*.image")),
          yaml(
            """
              services:
                app:
                  image: eclipse-temurin:17-jre
                db:
                  image: postgres:16
              """,
            """
              services:
                app:
                  image: eclipse-temurin:25-jre
                db:
                  image: postgres:16
              """,
            spec -> spec.path("compose.yaml")
          )
        );
    }
}
