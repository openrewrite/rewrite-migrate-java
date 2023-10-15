/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

public class LombokValueToRecordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValueToRecord())
          .parser(JavaParser.fromJavaVersion().classpath("lombok"));
    }

    @Test
    void convertOnlyValueAnnotatedClassWithoutDefaultValuesToRecord() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                }
                """,
              """
                public record A(
                   String test) {
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void classWithExplicitConstructorIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                   
                   public A() {
                       this.test = "test";
                   }
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void nonJava17ClassIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                
                @Value
                public class A {
                   String test;
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void classWithMultipleLombokAnnotationsIsUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import lombok.Value;
                import lombok.experimental.Accessors;
                
                @Value
                @Accessors(fluent = true)
                public class A {
                    String test;
                }
                """
            ),
            17
          )
        );
    }

    @Test
    void existingRecordsAreUnchanged() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                public record A(String test) {
                }
                """
            ),
            17
          )
        );
    }
}
