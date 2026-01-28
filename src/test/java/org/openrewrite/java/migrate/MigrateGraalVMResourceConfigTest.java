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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.json.Assertions.json;

class MigrateGraalVMResourceConfigTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateGraalVMResourceConfig());
    }

    @DocumentExample
    @Test
    void migrateSimpleResourceConfig() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": ".*\\\\.txt"},
                    {"pattern": "META-INF/.*"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "**/*.txt"},
                    {"glob": "META-INF/**"}
                  ]
              }
              """,
            spec -> spec.path("META-INF/native-image/resource-config.json")
          )
        );
    }

    @Test
    void migrateWithExcludes() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": ".*\\\\.properties"}
                  ],
                  "excludes": [
                    {"pattern": ".*\\\\.bak"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "**/*.properties"}
                  ]
              }
              """,
            spec -> spec.path("META-INF/native-image/com.example/myapp/resource-config.json")
          )
        );
    }

    @Test
    void migrateSingleLevelWildcard() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": "config/[^/]*\\\\.properties"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "config/*.properties"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void migrateLiteralPath() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": "exact/path/to/file\\\\.txt"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "exact/path/to/file.txt"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void migrateNestedPathWildcard() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": "[^/]*/[^/]*\\\\.xml"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "*/*.xml"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void noChangeForNonResourceConfigFiles() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": ".*\\\\.txt"}
                  ]
                }
              }
              """,
            spec -> spec.path("some/other/config.json")
          )
        );
    }

    @Test
    void noChangeForAlreadyNewFormat() {
        rewriteRun(
          json(
            """
              {
                "resources": [
                  {"glob": "**/*.txt"}
                ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void handleEmptyIncludes() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": []
                }
              }
              """,
            """
              {
                "resources": []
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void preserveModuleField() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"module": "myModule", "pattern": ".*\\\\.txt"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"module": "myModule", "glob": "**/*.txt"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void migrateMultiplePatterns() {
        rewriteRun(
          json(
            """
              {
                "resources": {
                  "includes": [
                    {"pattern": ".*\\\\.json"},
                    {"pattern": ".*\\\\.xml"},
                    {"pattern": "static/.*"},
                    {"pattern": "templates/[^/]*\\\\.html"}
                  ]
                }
              }
              """,
            """
              {
                "resources": [
                    {"glob": "**/*.json"},
                    {"glob": "**/*.xml"},
                    {"glob": "static/**"},
                    {"glob": "templates/*.html"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }

    @Test
    void preserveOtherTopLevelFields() {
        rewriteRun(
          json(
            """
              {
                "bundles": [
                  {"name": "com.example.Messages"}
                ],
                "resources": {
                  "includes": [
                    {"pattern": ".*\\\\.properties"}
                  ]
                }
              }
              """,
            """
              {
                "bundles": [
                  {"name": "com.example.Messages"}
                ],
                "resources": [
                    {"glob": "**/*.properties"}
                  ]
              }
              """,
            spec -> spec.path("resource-config.json")
          )
        );
    }
}
