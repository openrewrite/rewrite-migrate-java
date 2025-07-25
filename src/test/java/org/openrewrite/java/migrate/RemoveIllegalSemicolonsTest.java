/*
 * Copyright 2024 the original author or authors.
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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class RemoveIllegalSemicolonsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveIllegalSemicolons())
          .allSources(src -> src.markers(javaVersion(21)));
    }

    @ExpectedToFail("Fails after adding whitespace validation in RewriteTest: " +
                    "https://github.com/openrewrite/rewrite/commit/33d8f7d42f9bd6749ab93171fad31289b0e2bc97")
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/396")
    @Test
    void importSemicolon() {
        //language=java
        rewriteRun(
          java(
            """
              package p; ;
              ;; import java.util.List;
              class AfterPackage { }
              """,
            """
              package p;
              import java.util.List;
              class AfterPackage { }
              """
          ),
          java(
            """
              package p;
              import java.util.List; ;
              ;; import java.util.Set;
              class BetweenImport { }
              """,
            """
              package p;
              import java.util.List;
              import java.util.Set;
              class BetweenImport { }
              """
          )
        );
    }
}
