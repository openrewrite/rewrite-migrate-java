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

class AddStopBubblingToLombokConfigTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStopBubblingToLombokConfig());
    }

    @DocumentExample
    @Test
    void addStopBubblingToRootConfig() {
        rewriteRun(
          text(
            """
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            """
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void rootConfigThatAlreadyStopsBubblingIsNotChanged() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void rootConfigThatTurnsOffStopBubblingIsLeftAlone() {
        rewriteRun(
          text(
            """
              config.stopBubbling = false
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void stopBubblingSpelledWithADifferentCaseIsNotAddedAgain() {
        rewriteRun(
          text(
            """
              config.stopbubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void stopBubblingDeclaredByAnImportedFileIsNotAddedAgain() {
        rewriteRun(
          text(
            """
              import shared/base.config
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("shared/base.config")
          )
        );
    }

    @Test
    void rootConfigImportingAFileThatIsNotAmongTheSourcesIsLeftAlone() {
        rewriteRun(
          text(
            """
              import shared/base.config
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void importsAreFollowedThroughTheFilesTheyName() {
        rewriteRun(
          text(
            """
              import shared/base.config
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import deeper.config
              """,
            spec -> spec.path("shared/base.config")
          ),
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("shared/deeper.config")
          )
        );
    }

    @Test
    void importsThatLeadBackAroundLeaveTheRootAlone() {
        rewriteRun(
          text(
            """
              import shared/base.config
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import ../lombok.config
              """,
            spec -> spec.path("shared/base.config")
          )
        );
    }

    @Test
    void nestedConfigsAreNotChanged() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigIsNotTreatedAsTheRootConfig() {
        rewriteRun(
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void doNothingWhenLombokConfigIsAbsent() {
        rewriteRun(
          text(
            """
              This is a README file.
              """,
            spec -> spec.path("README.md")
          )
        );
    }

    @Test
    void unrelatedConfigFileIsNotChanged() {
        rewriteRun(
          text(
            """
              whatever=true
              """,
            spec -> spec.path("unrelated.config")
          )
        );
    }

    @Test
    void emptyRootConfig() {
        rewriteRun(
          text(
            "",
            """
              config.stopBubbling = true

              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void rootConfigWithoutATrailingNewLine() {
        rewriteRun(
          text(
            "lombok.val.flagUsage = error",
            """
              lombok.val.flagUsage = error
              config.stopBubbling = true""",
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void rootConfigWithWindowsLineEndingsIsAppendedToWithWindowsLineEndings() {
        rewriteRun(
          text(
            "lombok.val.flagUsage = error\r\nlombok.var.flagUsage = error",
            "lombok.val.flagUsage = error\r\nlombok.var.flagUsage = error\r\nconfig.stopBubbling = true",
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void rootConfigIsNotReformatted() {
        rewriteRun(
          text(
            """
              # Keep the comments and the odd spacing exactly as they are.
              lombok.val.flagUsage=error

                  lombok.var.flagUsage   =   error
              """,
            """
              # Keep the comments and the odd spacing exactly as they are.
              lombok.val.flagUsage=error

                  lombok.var.flagUsage   =   error
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }
}
