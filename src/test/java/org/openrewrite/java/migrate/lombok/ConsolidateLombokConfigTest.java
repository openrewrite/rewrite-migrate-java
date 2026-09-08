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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.SourceSpecs.text;

class ConsolidateLombokConfigTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConsolidateLombokConfig());
    }

    @DocumentExample
    @Test
    void mergeNestedConfigsIntoRootConfig() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              lombok.anyConstructor.addConstructorProperties = true
              lombok.extern.findbugs.addSuppressFBWarnings = true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              lombok.anyConstructor.addConstructorProperties = true
              lombok.extern.findbugs.addSuppressFBWarnings = true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              a1=a1value
              a2=a2value
              other=foobar
              b1=b1value
              b2=b2value
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              a1=a1value
              a2=a2value
              other=foobar
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              b1=b1value
              b2=b2value
              other=foobar
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
              """,
            doesNotExist(),
            spec -> spec.path("b/lombok.config")
          )
        );
    }

    @Test
    void conflictingDirectiveInNestedConfig() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = false
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void conflictingDirectivesInNestedConfigs() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = false
              """,
            spec -> spec.path("b/lombok.config")
          )
        );
    }

    @Test
    void listDirectivesWithDifferentValuesDoNotConflict() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
              """,
            doesNotExist(),
            spec -> spec.path("b/lombok.config")
          )
        );
    }

    @Test
    void spacingAroundTheOperatorDoesNotPreventDeduplication() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation=true
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void indentedDirectivesAreNormalized() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              lombok.copyableAnnotations += com.example.Ann
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          // The first line is left unindented so that `trimIndent` does not strip the indentation under test.
          text(
            """
              lombok.copyableAnnotations += com.example.Ann
                  lombok.addLombokGeneratedAnnotation = true
                  lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void singleConfigIsNotChanged() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              lombok.anyConstructor.addConstructorProperties = true
              lombok.extern.findbugs.addSuppressFBWarnings = true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void ignoreUnrelatedConfigFile() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              whatever=true
              """,
            spec -> spec.path("unrelated/unrelated.config")
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
    void rootConfigIsNotReformatted() {
        rewriteRun(
          text(
            """
              # Lombok configuration for this project.

              # val is fine, var is not.
              lombok.val.flagUsage   = allow
              lombok.var.flagUsage=error

              config.stopBubbling = true
              """,
            """
              # Lombok configuration for this project.

              # val is fine, var is not.
              lombok.val.flagUsage   = allow
              lombok.var.flagUsage=error

              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = true
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void rootConfigImportingAFileThatIsNotAmongTheSourcesIsLeftAlone() {
        rewriteRun(
          text(
            """
              import shared/base.config
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
    void importInRootConfigIsPreservedAtTheTopOfTheFile() {
        rewriteRun(
          text(
            """
              import shared/base.config
              config.stopBubbling = true
              """,
            """
              import shared/base.config
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("shared/base.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void directiveTheRootImportsAlreadyDeclaresIsNotRepeated() {
        rewriteRun(
          text(
            """
              import shared/base.config
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("shared/base.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void directiveConflictingWithWhatTheRootImportsAbortsTheRecipe() {
        rewriteRun(
          text(
            """
              import shared/base.config
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = warning
              """,
            spec -> spec.path("shared/base.config")
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
    void importsAreFollowedThroughTheFilesTheyName() {
        rewriteRun(
          text(
            """
              import shared/base.config
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import more.config
              """,
            spec -> spec.path("shared/base.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("shared/more.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void importsThatLeadBackAroundAbortTheRecipe() {
        rewriteRun(
          text(
            """
              import shared/base.config
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import ../lombok.config
              """,
            spec -> spec.path("shared/base.config")
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
    void configImportedByAnotherConfigIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import ../b/lombok.config
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("b/lombok.config")
          ),
          text(
            """
              lombok.var.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("c/lombok.config")
          )
        );
    }

    @Test
    void clearInRootConfigIsPreserved() {
        rewriteRun(
          text(
            """
              clear lombok.copyableAnnotations
              config.stopBubbling = true
              """,
            """
              clear lombok.copyableAnnotations
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void linesLombokCannotReadArePreserved() {
        rewriteRun(
          text(
            """
              this is not a directive
              config.stopBubbling = true
              """,
            """
              this is not a directive
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigDeclaringStopBubblingIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("generated/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigTurningOffStopBubblingIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = false
              a=aValue
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedImportIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import base.config
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedClearIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              clear lombok.copyableAnnotations
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedRemoveIsLeftInPlaceBecauseItsOrderMatters() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations -= com.example.Ann
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedLinesLombokCannotReadAreLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              this is not a directive
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigWithNothingToHoistIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              # Nothing to see here.
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedCommentsAreHoistedWithTheirDirectives() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              # Generated sources need the annotation.
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              # Generated sources need the annotation.
              lombok.addLombokGeneratedAnnotation = true
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void commentAtTheEndOfANestedConfigIsCarriedOver() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              # TODO revisit once module a no longer uses val.
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              # TODO revisit once module a no longer uses val.
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void commentAtTheEndOfARedundantNestedConfigIsCarriedOver() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              # TODO revisit once module a no longer uses val.
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              # TODO revisit once module a no longer uses val.
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void additionsOfADirectoryAboveAnotherAreAppendedFirstToKeepTheirOrderInTheList() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.copyableAnnotations += com.example.A
              lombok.copyableAnnotations += com.example.B
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += com.example.A
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += com.example.B
              """,
            doesNotExist(),
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void rootClearOfAKeyBlocksHoistingThatKey() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              clear lombok.copyableAnnotations
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void rootRemovalOfAValueBlocksHoistingThatKey() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.copyableAnnotations -= com.example.Ann
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void redundantNestedConfigIsDeletedWithoutChangingTheRoot() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void deeplyNestedConfigsAreMergedInAPredictableOrder() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              b=bValue
              a=aValue
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              a=aValue
              """,
            doesNotExist(),
            spec -> spec.path("modules/z/deep/lombok.config")
          ),
          text(
            """
              b=bValue
              """,
            doesNotExist(),
            spec -> spec.path("modules/a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigsAreLeftInPlaceWithoutARootConfig() {
        rewriteRun(
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("module/lombok.config")
          ),
          text(
            """
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("module/nested/lombok.config")
          )
        );
    }

    @Test
    void nothingIsChangedWhenNothingCanBeHoisted() {
        rewriteRun(
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import base.config
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void hoistableAndUnhoistableNestedConfigsAreHandledIndependently() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              b=bValue
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import base.config
              a=aValue
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              b=bValue
              """,
            doesNotExist(),
            spec -> spec.path("b/lombok.config")
          )
        );
    }

    @Test
    void commentSyntaxLombokDoesNotSupportIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              // Lombok comments start with a hash, so Lombok cannot read this line.
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigWithOnlyBlankLinesIsLeftInPlace() {
        rewriteRun(
          text(
            """


              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          )
        );
    }

    @Test
    void blankLinesInNestedConfigsAreNotHoisted() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error

              lombok.var.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void spacingAroundTheListOperatorDoesNotPreventDeduplication() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations+=com.example.Ann
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void valueContainingAnEqualsSignIsReadAsPartOfTheValue() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.nonNull.exceptionType = a=b
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.nonNull.exceptionType=a=b
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void emptyRootConfig() {
        rewriteRun(
          text(
            "",
            """
              lombok.val.flagUsage = error

              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void rootConfigWithoutATrailingNewLine() {
        rewriteRun(
          text(
            "config.stopBubbling = true",
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error""",
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void rootConfigWithWindowsLineEndingsIsAppendedToWithWindowsLineEndings() {
        rewriteRun(
          text(
            "config.stopBubbling = true\r\nlombok.var.flagUsage = error",
            "config.stopBubbling = true\r\nlombok.var.flagUsage = error\r\nlombok.val.flagUsage = error",
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void rootConfigEndingWithABlankLineKeepsItsTrailingNewLine() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true

              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error

              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigUnderAStopBubblingAncestorIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("generated/lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("generated/nested/lombok.config")
          )
        );
    }

    @Test
    void commentIsNotHoistedWhenItsDirectiveIsAlreadyDeclaredByTheRoot() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              # val is banned here too.
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void keyAssignedTwiceInOneFileIsNotAConflict() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = allow
              lombok.val.flagUsage = error
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = allow
              lombok.val.flagUsage = error
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addLombokGeneratedAnnotation = true
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void hoistingContinuesBelowADirectoryThatTurnsOffStopBubbling() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopBubbling = false
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.var.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/nested/lombok.config")
          )
        );
    }

    @Test
    void onlyCommentsWhoseDirectiveIsHoistedAreCarriedOver() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              # var is banned here as well.
              lombok.var.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              # val is banned everywhere.
              lombok.val.flagUsage = error
              # var is banned here as well.
              lombok.var.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void keyAssignedTwiceInANestedConfigKeepsOnlyTheAssignmentThatWins() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = allow
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void assignmentSupersededInItsOwnFileDoesNotOutrankTheRoot() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = warning
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void assignmentSupersededInItsOwnFileDoesNotOutrankAnotherNestedConfig() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = warning
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("b/lombok.config")
          )
        );
    }

    @Test
    void nestedStopBubblingSpelledWithADifferentCaseIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              config.stopbubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void conflictingValuesForAKeySpelledWithADifferentCaseAbortTheRecipe() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.addLombokGeneratedAnnotation = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.addlombokgeneratedannotation = false
              """,
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void keySpelledWithADifferentCaseIsRecognizedAsAlreadyDeclaredByTheRoot() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              Lombok.Val.FlagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigShadowedByAnAncestorAssignmentIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              clear lombok.copyableAnnotations
              lombok.fieldDefaults.defaultFinal = false
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.fieldDefaults.defaultFinal = true
              """,
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigWhoseAdditionAnAncestorClearsIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              clear lombok.copyableAnnotations
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.copyableAnnotations += com.example.Ann
              """,
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigBelowAnAncestorThatImportsIsLeftInPlace() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              import base.config
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void nestedConfigIsHoistedWhenAnAncestorLeftInPlaceSaysTheSameThing() {
        rewriteRun(
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              clear lombok.copyableAnnotations
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("a/lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("a/b/lombok.config")
          )
        );
    }

    @Test
    void configWithNoJavaSourceBelowItIsLeftInPlace() {
        rewriteRun(
          java(
            """
              class Foo {
              }
              """,
            spec -> spec.path("src/main/java/Foo.java")
          ),
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
            spec -> spec.path("src/test/resources/fixtures/lombok.config")
          )
        );
    }

    @Test
    void configGoverningTestSourcesIsHoisted() {
        rewriteRun(
          java(
            """
              package com.foo;

              class Foo {
              }
              """,
            spec -> spec.path("src/test/java/com/foo/Foo.java")
          ),
          text(
            """
              config.stopBubbling = true
              """,
            """
              config.stopBubbling = true
              lombok.val.flagUsage = error
              """,
            spec -> spec.path("lombok.config")
          ),
          text(
            """
              lombok.val.flagUsage = error
              """,
            doesNotExist(),
            spec -> spec.path("src/test/java/com/foo/lombok.config")
          )
        );
    }
}
