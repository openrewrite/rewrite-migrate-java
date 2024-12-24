/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseDataTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.lombok.UseData")
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true)
            .classpath("lombok"));
    }

    @DocumentExample
    @Test
    void replaceOneFieldData() {
        rewriteRun(// language=java
          java(
            """
              import lombok.ToString;
              import lombok.EqualsAndHashCode;
              import lombok.Getter;
              import lombok.Setter;
              import lombok.RequiredArgsConstructor;

              @ToString
              @EqualsAndHashCode
              @Getter
              @Setter
              @RequiredArgsConstructor
              class A {}
              """,
            """
              import lombok.Data;

              @Data
              class A {}
              """
          )
        );
    }

    @Test
    void otherAnnotationAbove() {
        rewriteRun(// language=java
          java(
            """
              import lombok.ToString;
              import lombok.EqualsAndHashCode;
              import lombok.Getter;
              import lombok.Setter;
              import lombok.RequiredArgsConstructor;
              import lombok.extern.java.Log;

              @Log
              @ToString
              @EqualsAndHashCode
              @Getter
              @Setter
              @RequiredArgsConstructor
              class A {}
              """,
            """
              import lombok.Data;
              import lombok.extern.java.Log;

              @Data
              @Log
              class A {}
              """
          )
        );
    }

    @Test
    void otherAnnotationBelow() {
        rewriteRun(// language=java
          java(
            """
              import lombok.ToString;
              import lombok.EqualsAndHashCode;
              import lombok.Getter;
              import lombok.Setter;
              import lombok.RequiredArgsConstructor;
              import lombok.extern.java.Log;

              @ToString
              @EqualsAndHashCode
              @Getter
              @Setter
              @RequiredArgsConstructor
              @Log
              class A {}
              """,
            """
              import lombok.Data;
              import lombok.extern.java.Log;

              @Data
              @Log
              class A {}
              """
          )
        );
    }

    @Test
    void otherAnnotationsAround() {
        rewriteRun(// language=java
          java(
            """
              import lombok.*;
              import lombok.extern.java.Log;

              @NoArgsConstructor
              @ToString
              @EqualsAndHashCode
              @Getter
              @Setter
              @RequiredArgsConstructor
              @Log
              class A {}
              """,
            """
              import lombok.Data;
              import lombok.NoArgsConstructor;
              import lombok.extern.java.Log;

              @Data
              @NoArgsConstructor
              @Log
              class A {}
              """
          )
        );
    }

    @Test
    void oneAnnotationMissing() {
        rewriteRun(// language=java
          java(
            """
              import lombok.*;
              import lombok.extern.java.Log;

              @NoArgsConstructor
              @ToString
              @EqualsAndHashCode
              @Getter
              @RequiredArgsConstructor
              @Log
              class A {}
              """
          )
        );
    }

}
