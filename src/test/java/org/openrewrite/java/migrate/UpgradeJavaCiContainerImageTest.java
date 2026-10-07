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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

class UpgradeJavaCiContainerImageTest implements RewriteTest {
    @Test
    void upgradeGitHubJobContainer() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(25)),
          yaml(
            """
              jobs:
                test:
                  container: cimg/openjdk:22.0
                  services:
                    database:
                      image: mongo:5
              """,
            """
              jobs:
                test:
                  container: cimg/openjdk:25.0
                  services:
                    database:
                      image: mongo:5
              """,
            spec -> spec.path(".github/workflows/test.yaml")
          )
        );
    }

    @Test
    void upgradeCircleCiJobContainer() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(25)),
          yaml(
            """
              version: 2.1
              jobs:
                test:
                  docker:
                    - image: cimg/openjdk:17.0
                    - image: cimg/openjdk:11.0
              """,
            """
              version: 2.1
              jobs:
                test:
                  docker:
                    - image: cimg/openjdk:25.0
                    - image: cimg/openjdk:11.0
              """,
            spec -> spec.path(".circleci/config.yml")
          )
        );
    }
    @Test
    void preserveNewerVersionsServicesAndUnrelatedYaml() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaCiContainerImage(25)),
          yaml(
            """
              jobs:
                test:
                  container:
                    image: cimg/openjdk:26.0
                  services:
                    other:
                      image: cimg/openjdk:17.0
              """,
            spec -> spec.path(".github/workflows/test.yml")
          ),
          yaml(
            """
              jobs:
                test:
                  container: cimg/openjdk:17.0
              """,
            spec -> spec.path("example.yml")
          )
        );
    }
}

