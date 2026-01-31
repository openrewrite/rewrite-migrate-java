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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

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
    })
    @ParameterizedTest
    void upgradeDockerImage(String fromImage, String fromTag, String toImage, String toTag, int targetVersion) {
        rewriteRun(
          spec -> spec.recipe(new UpgradeDockerImageVersion(targetVersion)),
          text(
            "FROM %s:%s".formatted(fromImage, fromTag),
            "FROM %s:%s".formatted(toImage, toTag),
            spec -> spec.path("Dockerfile")
          )
        );
    }
}
