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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.docker.Assertions.docker;

class UpgradeDockerImageVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeDockerImageVersion(25));
    }

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
      // The argument holds the bare version
      "java_version=17, eclipse-temurin:${java_version}, java_version=25, eclipse-temurin:${java_version}",
      "java_version=17, eclipse-temurin:$java_version, java_version=25, eclipse-temurin:$java_version",
      "JAVA_VERSION=11, eclipse-temurin:${JAVA_VERSION}-jre, JAVA_VERSION=25, eclipse-temurin:${JAVA_VERSION}-jre",
      "JAVA_VERSION=8, amazoncorretto:${JAVA_VERSION}-alpine, JAVA_VERSION=25, amazoncorretto:${JAVA_VERSION}-alpine",
      // The argument holds the version and a suffix
      "IMAGE_TAG=11-jre-alpine, eclipse-temurin:${IMAGE_TAG}, IMAGE_TAG=25-jre-alpine, eclipse-temurin:${IMAGE_TAG}",
      // The argument holds the whole image reference
      "BASE_IMAGE=eclipse-temurin:11-jre, ${BASE_IMAGE}, BASE_IMAGE=eclipse-temurin:25-jre, ${BASE_IMAGE}",
      "BASE_IMAGE=openjdk:11-jre, ${BASE_IMAGE}, BASE_IMAGE=eclipse-temurin:25-jre, ${BASE_IMAGE}",
      "BASE_IMAGE=eclipse-temurin:11-jre@sha256:1234567890abcdef, ${BASE_IMAGE}, BASE_IMAGE=eclipse-temurin:25-jre, ${BASE_IMAGE}",
      // The argument holds the image name only
      "BASE_IMAGE=eclipse-temurin, ${BASE_IMAGE}:11-jre, BASE_IMAGE=eclipse-temurin, ${BASE_IMAGE}:25-jre",
      "BASE_IMAGE=openjdk, ${BASE_IMAGE}:11-jre, BASE_IMAGE=eclipse-temurin, ${BASE_IMAGE}:25-jre",
      // A quoted default value keeps its quotes
      "JAVA_VERSION=\"11\", eclipse-temurin:${JAVA_VERSION}, JAVA_VERSION=\"25\", eclipse-temurin:${JAVA_VERSION}",
      "IMAGE_TAG=\"11-jre\", eclipse-temurin:${IMAGE_TAG}, IMAGE_TAG=\"25-jre\", eclipse-temurin:${IMAGE_TAG}",
      "BASE_IMAGE=\"openjdk:11-jre\", ${BASE_IMAGE}, BASE_IMAGE=\"eclipse-temurin:25-jre\", ${BASE_IMAGE}",
    })
    @ParameterizedTest
    void upgradeArgumentDefaultValue(String beforeArg, String beforeFrom, String afterArg, String afterFrom) {
        rewriteRun(
          docker(
            """
              ARG %s
              FROM %s
              """.formatted(beforeArg, beforeFrom),
            """
              ARG %s
              FROM %s
              """.formatted(afterArg, afterFrom)
          )
        );
    }

    @Test
    void upgradeSingleQuotedArgumentDefaultValue() {
        rewriteRun(
          docker(
            """
              ARG IMAGE_TAG='11-jre'
              FROM eclipse-temurin:${IMAGE_TAG}
              """,
            """
              ARG IMAGE_TAG='25-jre'
              FROM eclipse-temurin:${IMAGE_TAG}
              """
          )
        );
    }

    @Test
    void upgradeDeprecatedImageNameAlongsideArgumentDefaultValue() {
        rewriteRun(
          docker(
            """
              ARG JAVA_VERSION=11
              FROM openjdk:${JAVA_VERSION}-jre
              """,
            """
              ARG JAVA_VERSION=25
              FROM eclipse-temurin:${JAVA_VERSION}-jre
              """
          )
        );
    }

    @Test
    void upgradeArgumentDefaultValueSharedByStages() {
        rewriteRun(
          docker(
            """
              ARG JAVA_VERSION=11
              FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder
              FROM eclipse-temurin:${JAVA_VERSION}-jre
              """,
            """
              ARG JAVA_VERSION=25
              FROM eclipse-temurin:${JAVA_VERSION}-jdk AS builder
              FROM eclipse-temurin:${JAVA_VERSION}-jre
              """
          )
        );
    }

    @Test
    void dropDigestPinWhenUpgradingArgumentDefaultValue() {
        rewriteRun(
          docker(
            """
              ARG JAVA_VERSION=11
              FROM eclipse-temurin:${JAVA_VERSION}-jre@sha256:1234567890abcdef
              """,
            """
              ARG JAVA_VERSION=25
              FROM eclipse-temurin:${JAVA_VERSION}-jre
              """
          )
        );
    }

    @Test
    void dropDigestPinWhenUpgradingArgumentHoldingWholeReference() {
        rewriteRun(
          docker(
            """
              ARG BASE_IMAGE=eclipse-temurin:11-jre
              FROM ${BASE_IMAGE}@sha256:1234567890abcdef
              """,
            """
              ARG BASE_IMAGE=eclipse-temurin:25-jre
              FROM ${BASE_IMAGE}
              """
          )
        );
    }

    @Test
    void upgradeArgumentSharedWithAnImageWeDoNotUpgrade() {
        rewriteRun(
          docker(
            """
              ARG VERSION=11
              FROM eclipse-temurin:${VERSION} AS build
              FROM node:${VERSION}
              """,
            """
              ARG VERSION=25
              FROM eclipse-temurin:${VERSION} AS build
              FROM node:${VERSION}
              """
          )
        );
    }

    @Test
    void dropDigestPinWhenUpgradingASharedArgument() {
        rewriteRun(
          docker(
            """
              ARG VERSION=11
              FROM eclipse-temurin:${VERSION}@sha256:1234567890abcdef AS build
              FROM node:${VERSION}
              """,
            """
              ARG VERSION=25
              FROM eclipse-temurin:${VERSION} AS build
              FROM node:${VERSION}
              """
          )
        );
    }

    @Test
    void upgradeDeprecatedImageAlongsideASharedArgument() {
        rewriteRun(
          docker(
            """
              ARG VERSION=11
              FROM openjdk:${VERSION} AS build
              FROM node:${VERSION}
              """,
            """
              ARG VERSION=25
              FROM eclipse-temurin:${VERSION} AS build
              FROM node:${VERSION}
              """
          )
        );
    }

    @Test
    void upgradeImageAndVersionArgumentsSharedWithAnotherImage() {
        rewriteRun(
          docker(
            """
              ARG IMAGE=openjdk
              ARG VERSION=11
              FROM ${IMAGE}:${VERSION} AS build
              FROM node:${VERSION}
              """,
            """
              ARG IMAGE=eclipse-temurin
              ARG VERSION=25
              FROM ${IMAGE}:${VERSION} AS build
              FROM node:${VERSION}
              """
          )
        );
    }

    @Test
    void upgradeImageArgumentSharedWithAnUntaggedImage() {
        rewriteRun(
          docker(
            """
              ARG BASE=openjdk
              FROM ${BASE}:11-jre AS build
              FROM ${BASE}
              """,
            """
              ARG BASE=eclipse-temurin
              FROM ${BASE}:25-jre AS build
              FROM ${BASE}
              """
          )
        );
    }

    @Test
    void upgradeImageArgumentSharedWithAnImageStuckOnItsTag() {
        rewriteRun(
          docker(
            """
              ARG BASE=openjdk
              FROM ${BASE}:11-jre AS build
              FROM ${BASE}:latest
              """,
            """
              ARG BASE=eclipse-temurin
              FROM ${BASE}:25-jre AS build
              FROM ${BASE}:latest
              """
          )
        );
    }

    @CsvSource({
      "FROM openjdk:11-jre@sha256:1234567890abcdef, FROM eclipse-temurin:25-jre",
      "FROM eclipse-temurin:11-jre@sha256:1234567890abcdef, FROM eclipse-temurin:25-jre",
    })
    @ParameterizedTest
    void dropStaleDigestPin(String before, String after) {
        rewriteRun(
          docker(before, after)
        );
    }

    @Test
    void changeLiteralImageAlongsideVariableImage() {
        rewriteRun(
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

    @Test
    void upgradeOnlyTheArgumentDeclarationHoldingTheDefault() {
        rewriteRun(
          docker(
            """
              ARG JAVA_VERSION
              ARG JAVA_VERSION=11
              FROM eclipse-temurin:${JAVA_VERSION}
              """,
            """
              ARG JAVA_VERSION
              ARG JAVA_VERSION=25
              FROM eclipse-temurin:${JAVA_VERSION}
              """
          )
        );
    }

    @CsvSource({
      // A registry the FROM spells out
      "docker.io/eclipse-temurin:11-jre, docker.io/eclipse-temurin:25-jre",
      "docker.io/openjdk:11-jre, docker.io/eclipse-temurin:25-jre",
      "myregistry:5000/openjdk:11-jre, myregistry:5000/eclipse-temurin:25-jre",
      "localhost/eclipse-temurin:11-jre, localhost/eclipse-temurin:25-jre",
      // A registry the FROM builds from an argument
      "${REGISTRY}/eclipse-temurin:11-jre, ${REGISTRY}/eclipse-temurin:25-jre",
      "${REGISTRY}/openjdk:11-jre, ${REGISTRY}/eclipse-temurin:25-jre",
      "${REGISTRY}/azul/zulu-openjdk:11-jdk, ${REGISTRY}/azul/zulu-openjdk:25-jdk",
    })
    @ParameterizedTest
    void upgradeAnImageBehindARegistry(String before, String after) {
        rewriteRun(
          docker(
            """
              ARG REGISTRY
              FROM %s
              """.formatted(before),
            """
              ARG REGISTRY
              FROM %s
              """.formatted(after)
          )
        );
    }

    @Test
    void upgradeArgumentDefaultValueBehindAVariableRegistry() {
        rewriteRun(
          docker(
            """
              ARG REGISTRY
              ARG JAVA_VERSION=11
              FROM ${REGISTRY}/eclipse-temurin:${JAVA_VERSION}-jre
              """,
            """
              ARG REGISTRY
              ARG JAVA_VERSION=25
              FROM ${REGISTRY}/eclipse-temurin:${JAVA_VERSION}-jre
              """
          )
        );
    }

    @Nested
    class NoChange {

        @CsvSource({
          "FROM ${IMAGE_NAME}:${IMAGE_TAG}",
          "FROM $IMAGE_NAME:$IMAGE_TAG",
          "FROM ${IMAGE_NAME}:11",
          "FROM eclipse-temurin:${IMAGE_TAG}",
        })
        @ParameterizedTest
        void variableImageReferences(String from) {
            rewriteRun(
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

        @Test
        void anArgumentDefaultingToAnotherVariable() {
            rewriteRun(
              docker(
                """
                  ARG DEFAULT_VERSION
                  ARG JAVA_VERSION=${DEFAULT_VERSION}
                  FROM eclipse-temurin:${JAVA_VERSION}
                  """
              )
            );
        }

        @CsvSource({
          // Arguments that are not used in a FROM are left alone
          "JAVA_VERSION=11, eclipse-temurin:25-jre",
          // Arguments for unrelated images are left alone
          "NODE_VERSION=20, node:${NODE_VERSION}-alpine",
          // Arguments already at or beyond the target version are left alone
          "JAVA_VERSION=25, eclipse-temurin:${JAVA_VERSION}-jre",
          "JAVA_VERSION=26, eclipse-temurin:${JAVA_VERSION}-jre",
          // Arguments not holding a leading version are left alone
          "JAVA_VERSION=latest, eclipse-temurin:${JAVA_VERSION}",
          "JAVA_VERSION=\"latest\", eclipse-temurin:${JAVA_VERSION}",
          "SUFFIX=-jre, eclipse-temurin:11${SUFFIX}",
          // A leading segment that is not a registry host belongs to the repository name
          "JAVA_VERSION=11, mycompany/eclipse-temurin:${JAVA_VERSION}-jre",
        })
        @ParameterizedTest
        void unrelatedArgumentDefaultValues(String arg, String from) {
            rewriteRun(
              docker(
                """
                  ARG %s
                  FROM %s
                  """.formatted(arg, from)
              )
            );
        }

        @Test
        void anArgumentDeclaredAfterTheFirstFrom() {
            rewriteRun(
              docker(
                """
                  FROM eclipse-temurin:25-jre
                  ARG JAVA_VERSION=11
                  RUN echo "${JAVA_VERSION}"
                  """
              )
            );
        }

        @CsvSource({
          // Unrelated images are left alone
          "FROM ubuntu:22.04",
          "FROM node:20-alpine",
          // Tags without a leading Java version are left alone
          "FROM eclipse-temurin:latest",
          "FROM eclipse-temurin:",
          // Already at or beyond the target version
          "FROM eclipse-temurin:25-jre",
          "FROM eclipse-temurin:26-jre",
        })
        @ParameterizedTest
        void unrelatedImages(String from) {
            rewriteRun(
              docker(from)
            );
        }
    }
}
