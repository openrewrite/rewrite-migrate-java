/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class WasDevMvnChangeParentArtifactIdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.WasDevMvnChangeParentArtifactId");
    }

    @DocumentExample
    @Test
    void mvnChangeParentArtifactId() {
        rewriteRun(
          //language=XML
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                   <parent>
                        <groupId>net.wasdev.maven.parent</groupId>
                         <artifactId>java8-parent</artifactId>
                         <version>1.4</version>
                   </parent>
                   <artifactId>my-artifact</artifactId>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                   <parent>
                        <groupId>net.wasdev.maven.parent</groupId>
                         <artifactId>parent</artifactId>
                         <version>1.4</version>
                   </parent>
                   <artifactId>my-artifact</artifactId>
              </project>
              """
          )
        );
    }

    @Test
    void noChangeParentArtifactId() {
        rewriteRun(
          //language=XML
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                   <parent>
                        <groupId>net.wasdev.maven.parent</groupId>
                         <artifactId>parent</artifactId>
                         <version>1.4</version>
                   </parent>
                   <artifactId>my-artifact</artifactId>
              </project>
              """
          )
        );
    }
}
