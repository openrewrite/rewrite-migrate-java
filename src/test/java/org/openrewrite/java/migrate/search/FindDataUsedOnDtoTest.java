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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.migrate.table.DtoDataUses;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FindDataUsedOnDtoTest implements RewriteTest {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @ParameterizedTest
    @ValueSource(strings = {
      "java.time.LocalDate", "java.time.*"
    })
    void findDataUsedOnDto(String dtoType) {
        rewriteRun(
          spec -> spec.recipe(new FindDataUsedOnDto(dtoType))
            .dataTable(DtoDataUses.Row.class, rows -> {
                for (DtoDataUses.Row row : rows) {
                    assertThat(row.getSourcePath()).isEqualTo("Test.java");
                    assertThat(row.getMethodName()).isEqualTo("test");
                    assertThat(row.getField()).isIn("dayOfMonth", "dayOfYear");
                }
            }),
          //language=java
          java(
            """
              import java.time.LocalDate;

              class Test {
                  void test(LocalDate date) {
                        date.getDayOfMonth();
                        date.getDayOfYear();
                  }
              }
              """,
            """
              import java.time.LocalDate;

              class Test {
                  void test(LocalDate date) {
                        /*~~>*/date.getDayOfMonth();
                        /*~~>*/date.getDayOfYear();
                  }
              }
              """
          )
        );
    }
}
