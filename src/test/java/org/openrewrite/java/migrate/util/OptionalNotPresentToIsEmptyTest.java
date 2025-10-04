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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "SimplifyOptionalCallChains"})
class OptionalNotPresentToIsEmptyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new OptionalNotPresentToIsEmpty())
          .parser(JavaParser.fromJavaVersion());
    }

    @DocumentExample
    @Test
    void replaceNotIsPresentWithIsEmpty() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                package com.example.app;
                import java.util.Optional;
                class App {
                    boolean notPresent(Optional<String> bar){
                        return !bar.isPresent();
                    }
                }
                """,
              """
                package com.example.app;
                import java.util.Optional;
                class App {
                    boolean notPresent(Optional<String> bar){
                        return bar.isEmpty();
                    }
                }
                """
            ),
            11
          )
        );
    }
}
