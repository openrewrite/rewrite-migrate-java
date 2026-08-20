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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class UpgradeDockerImageVersionTest implements RewriteTest {

    @CsvSource({
      // Deprecated images migrate to eclipse-temurin
      "openjdk, 8, eclipse-temurin, 17, 17",
      "openjdk, 11, eclipse-temurin, 17, 17",
      "adoptopenjdk, 8, eclipse-temurin, 17, 17",
      "adoptopenjdk, 11, eclipse-temurin, 17, 17",
      // Deprecated images preserve common suffixes when migrating
      "openjdk, 11-jdk, eclipse-temurin, 17-jdk, 17",
      "openjdk, 11-jdk-alpine, eclipse-temurin, 17-jdk-alpine, 17",
      "adoptopenjdk, 8-jre, eclipse-temurin, 17-jre, 17",
      // Current images update tag only
      "eclipse-temurin, 8, eclipse-temurin, 17, 17",
      "eclipse-temurin, 11, eclipse-temurin, 17, 17",
      "amazoncorretto, 8, amazoncorretto, 17, 17",
      "amazoncorretto, 11, amazoncorretto, 17, 17",
      // Current images preserve common suffixes
      "eclipse-temurin, 11-jdk, eclipse-temurin, 17-jdk, 17",
      "eclipse-temurin, 11-jre, eclipse-temurin, 17-jre, 17",
      "eclipse-temurin, 11-jdk-alpine, eclipse-temurin, 17-jdk-alpine, 17",
      "eclipse-temurin, 11-jre-alpine, eclipse-temurin, 17-jre-alpine, 17",
      "eclipse-temurin, 11-jdk-jammy, eclipse-temurin, 17-jdk-jammy, 17",
      "eclipse-temurin, 11-jdk-focal, eclipse-temurin, 17-jdk-focal, 17",
      "amazoncorretto, 11-alpine, amazoncorretto, 17-alpine, 17",
      "azul/zulu-openjdk, 11-jdk, azul/zulu-openjdk, 17-jdk, 17",
      // Arbitrary suffix preserved via $1 capture (previously lost through wildcard fallback)
      "openjdk, 11-jdk-custom-suffix, eclipse-temurin, 17-jdk-custom-suffix, 17",
      "eclipse-temurin, 11-jdk-custom-suffix, eclipse-temurin, 17-jdk-custom-suffix, 17",
    })
    @ParameterizedTest
    void upgradeDockerImage(String fromImage, String fromTag, String toImage, String toTag, int targetVersion) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(targetVersion)),
          docker(
            "FROM %s:%s".formatted(fromImage, fromTag),
            "FROM %s:%s".formatted(toImage, toTag)
          )
        );
    }

    @CsvSource({
      "FROM ${IMAGE_NAME}:${IMAGE_TAG}",
      "FROM $IMAGE_NAME:$IMAGE_TAG",
      "FROM ${IMAGE_NAME}:11",
      "FROM eclipse-temurin:${IMAGE_TAG}",
      "FROM ${REGISTRY}/eclipse-temurin:11-jre",
    })
    @ParameterizedTest
    void doNotChangeVariableImageReferences(String from) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(25)),
          docker(
            """
              ARG IMAGE_NAME
              ARG IMAGE_TAG
              ARG REGISTRY
              %s
              """.formatted(from)
          )
        );
    }

    @CsvSource({
      // Unrelated images are left alone
      "FROM ubuntu:22.04",
      "FROM node:20-alpine",
      // Tags without a leading Java version are left alone
      "FROM eclipse-temurin:latest",
      // Already at or beyond the target version
      "FROM eclipse-temurin:25-jre",
      "FROM eclipse-temurin:26-jre",
    })
    @ParameterizedTest
    void doNotChangeUnrelatedImages(String from) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(25)),
          docker(from)
        );
    }

    @Test
    void preserveDigestWhenMigratingDeprecatedImage() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(25)),
          docker(
            "FROM openjdk:11-jre@sha256:1234567890abcdef",
            "FROM eclipse-temurin:25-jre@sha256:1234567890abcdef"
          )
        );
    }

    @Test
    void changeLiteralImageAlongsideVariableImage() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(25)),
          docker(
            """
              ARG IMAGE_TAG
              FROM eclipse-temurin:${IMAGE_TAG} AS builder
              FROM eclipse-temurin:11-jre
              """,
            """
              ARG IMAGE_TAG
              FROM eclipse-temurin:${IMAGE_TAG} AS builder
              FROM eclipse-temurin:25-jre
              """
          )
        );
    }
}
