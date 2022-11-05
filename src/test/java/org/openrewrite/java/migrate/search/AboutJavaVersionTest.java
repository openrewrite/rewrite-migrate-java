/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.java.Assertions.java;

public class AboutJavaVersionTest implements RewriteTest {

    @Test
    void aboutJavaVersion() {
        rewriteRun(
          spec -> spec.recipe(new AboutJavaVersion(null)),
          java(
            //language=java
            """
              class Test {
              }
              """,
            //language=java
            """
              /*~~(Java version: 11)~~>*/class Test {
              }
              """,
            spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "me", "me", "11.0.15+10", "11.0.15+10"))
          )
        );
    }
}
