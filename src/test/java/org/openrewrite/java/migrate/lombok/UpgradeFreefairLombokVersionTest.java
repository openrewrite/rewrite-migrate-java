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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UpgradeFreefairLombokVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeFreefairLombokVersion("1.18.42"));
    }

    @DocumentExample
    @Test
    void upgradesVersionInLombokBlock() {
        rewriteRun(
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '6.3.0'
              }

              lombok {
                  version = "1.18.22"
              }
              """,
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '6.3.0'
              }

              lombok {
                  version = "1.18.42"
              }
              """
          )
        );
    }

    @Test
    void upgradesDottedVersion() {
        rewriteRun(
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '6.3.0'
              }

              lombok.version = '1.18.22'
              """,
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '6.3.0'
              }

              lombok.version = '1.18.42'
              """
          )
        );
    }

    @Test
    void upgradesVersionSetInKotlinDsl() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  java
                  id("io.freefair.lombok") version "8.14"
              }

              lombok {
                  version.set("1.18.30")
              }
              """,
            """
              plugins {
                  java
                  id("io.freefair.lombok") version "8.14"
              }

              lombok {
                  version.set("1.18.42")
              }
              """
          )
        );
    }

    @Test
    void upgradesVersionAssignmentInKotlinDsl() {
        rewriteRun(
          //language=kotlin
          buildGradleKts(
            """
              plugins {
                  java
                  id("io.freefair.lombok") version "8.14"
              }

              lombok {
                  version = "1.18.30"
              }
              """,
            """
              plugins {
                  java
                  id("io.freefair.lombok") version "8.14"
              }

              lombok {
                  version = "1.18.42"
              }
              """
          )
        );
    }

    @Test
    void noChangeWithoutFreefairPlugin() {
        rewriteRun(
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
              }

              lombok {
                  version = "1.18.22"
              }
              """
          )
        );
    }

    @Test
    void noDowngrade() {
        rewriteRun(
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '9.1.0'
              }

              lombok {
                  version = "1.18.44"
              }
              """
          )
        );
    }

    @Test
    void noChangeToProjectVersionOrVariable() {
        rewriteRun(
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'io.freefair.lombok' version '6.3.0'
              }

              version = "1.0.0"
              def lombokVersion = "1.18.22"

              lombok {
                  version = lombokVersion
              }
              """
          )
        );
    }
}
