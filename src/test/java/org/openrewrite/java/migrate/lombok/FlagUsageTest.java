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

class FlagUsageTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FlagUsage("val", null));
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
              lombok.val.flagUsage = error
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
              lombok.val.flagUsage = error
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
              lombok.val.flagUsage = allow
              zzz=true
              """,
            """
              aaa=true
              lombok.val.flagUsage = error
              zzz=true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void unsortedConfigIsAppendedTo() {
        rewriteRun(
          text(
            """
              zzz=true
              aaa=true
              """,
            """
              zzz=true
              aaa=true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void emptyConfig() {
        rewriteRun(
          text(
            "",
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void nestedConfigIsWrittenToAsWell() {
        rewriteRun(
          text(
            "",
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            "",
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void spacingAndCaseOfTheDeclarationAreKept() {
        rewriteRun(
          text(
            """
              Lombok.Val.FlagUsage=warning
              """,
            """
              Lombok.Val.FlagUsage=error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void carriageReturnsAreKept() {
        rewriteRun(
          text(
            "aaa=true\r\nzzz=true",
            "aaa=true\r\nlombok.val.flagUsage = error\r\nzzz=true",
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void clearIsLeftAlone() {
        rewriteRun(
          text(
            """
              clear lombok.val.flagUsage
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void notALombokConfig() {
        rewriteRun(
          text(
            """
              aaa=true
              """,
            spec -> spec.path("application.properties")
          )
        );
    }

    @Test
    void sortsBeforeVar() {
        rewriteRun(
          text(
            """
              aaa=true
              lombok.var.flagUsage = error
              """,
            """
              aaa=true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void sortsAfterVal() {
        rewriteRun(
          spec -> spec.recipe(new FlagUsage("var", null)),
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

    @Test
    void warningInsteadOfError() {
        rewriteRun(
          spec -> spec.recipe(new FlagUsage("val", "warning")),
          text(
            """
              aaa=true
              """,
            """
              aaa=true
              lombok.val.flagUsage = warning
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void errorDowngradedToWarning() {
        rewriteRun(
          spec -> spec.recipe(new FlagUsage("val", "warning")),
          text(
            """
              lombok.val.flagUsage = error
              """,
            """
              lombok.val.flagUsage = warning
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }
}
