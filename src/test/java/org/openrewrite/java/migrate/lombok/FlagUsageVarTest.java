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

import static org.openrewrite.test.SourceSpecs.text;

class FlagUsageVarTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FlagUsageVar());
    }

    @DocumentExample
    @Test
    void flagUsageError() {
        rewriteRun(
          text(
            """
              aaa=true
              zzz=true
              """,
            """
              aaa=true
              lombok.var.flagUsage = error
              zzz=true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void noChanges() {
        rewriteRun(
          text(
            """
              aaa=true
              lombok.var.flagUsage = error
              zzz=true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void allow_to_error() {
        rewriteRun(
          text(
            """
              aaa=true
              lombok.var.flagUsage = allow
              zzz=true
              """,
            """
              aaa=true
              lombok.var.flagUsage = error
              zzz=true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void sortsAfterVal() {
        rewriteRun(
          text(
            """
              lombok.val.flagUsage = error
              zzz=true
              """,
            """
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              zzz=true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }
}
