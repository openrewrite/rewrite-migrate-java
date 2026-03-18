/*
 * Copyright 2025 the original author or authors.
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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UpgradeToJava21Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava21");
    }

    @DocumentExample
    @Test
    void ifElseIfAssignmentToSwitchExpressionInOnePass() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                class Test {
                    private static double convertToDouble(Object value) {
                        double dValue;
                        if (value instanceof String string) {
                            dValue = Double.parseDouble(string);
                        } else if (value instanceof Integer integer) {
                            dValue = integer.doubleValue();
                        } else if (value instanceof Long long1) {
                            dValue = long1.doubleValue();
                        } else {
                            dValue = (double) value;
                        }
                        return dValue;
                    }
                }
                """,
              """
                class Test {
                    private static double convertToDouble(Object value) {
                        return switch (value) {
                            case String string -> Double.parseDouble(string);
                            case Integer integer -> integer.doubleValue();
                            case Long long1 -> long1.doubleValue();
                            default -> (double) value;
                        };
                    }
                }
                """
            ), 21)
        );
    }
}
