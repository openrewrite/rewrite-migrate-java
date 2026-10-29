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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class ModuleHasKotlinSourceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasKotlinSource(null));
    }

    @DocumentExample
    @Test
    void kotlinSourceWithoutModuleMarkerBelongsToEnclosingModule() {
        // Android builds attach a `JavaProject` marker to the build file, but not to the Kotlin sources.
        var app = new JavaProject(randomId(), "app", null);
        rewriteRun(
          text(
            "plugins { id 'kotlin-android' }",
            "~~(Module has Kotlin source)~~>plugins { id 'kotlin-android' }",
            spec -> spec.path("app/build.gradle").markers(app)
          ),
          other("class App", spec -> spec.path("app/src/main/java/com/example/App.kt"))
        );
    }

    @Test
    void kotlinSourceWithoutModuleMarkerBelongsToClosestModuleOnly() {
        var root = new JavaProject(randomId(), "root", null);
        var app = new JavaProject(randomId(), "app", null);
        var lib = new JavaProject(randomId(), "lib", null);
        rewriteRun(
          text("root", spec -> spec.path("build.gradle").markers(root)),
          text(
            "app",
            "~~(Module has Kotlin source)~~>app",
            spec -> spec.path("app/build.gradle").markers(app)
          ),
          text("lib", spec -> spec.path("lib/build.gradle").markers(lib)),
          other("class App", spec -> spec.path("app/src/main/java/com/example/App.kt"))
        );
    }

    @Test
    void kotlinSourceOutsideAnyModuleMarksNothing() {
        var app = new JavaProject(randomId(), "app", null);
        rewriteRun(
          text("app", spec -> spec.path("app/build.gradle").markers(app)),
          other("class Tool", spec -> spec.path("tools/Tool.kt"))
        );
    }
}
