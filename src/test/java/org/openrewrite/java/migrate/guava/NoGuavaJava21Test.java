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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class NoGuavaJava21Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.guava.NoGuavaJava21")
          )
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void preferMathClampForDouble() {
        //language=java
        rewriteRun(
          version(
            java(
              """   
                import com.google.common.primitives.Doubles;
  
                class Test {
                    public double testMethod() {
                        return Doubles.constrainToRange(20D, 10D, 100D);
                    }
                }
                """,
              """ 
                class Test {
                    public double testMethod() {
                        return Math.clamp(20D, 10D, 100D);
                    }
                }
                """
            ),
            21)
        );
    }

    @Test
    void preferMathClampForLongs() {
        //language=java
        rewriteRun(
          version(
            java(
              """   
                import com.google.common.primitives.Longs;
  
                class Test {
                    public long testMethod() {
                        return Longs.constrainToRange(20L, 10L, 100L);
                    }
                }
                """,
              """ 
                class Test {
                    public long testMethod() {
                        return Math.clamp(20L, 10L, 100L);
                    }
                }
                """
            ),
            21)
        );
    }

    @Test
    void preferMathClampForFloats() {
        //language=java
        rewriteRun(
          version(
            java(
              """   
                import com.google.common.primitives.Floats;
  
                class Test {
                    public float testMethod() {
                        return Floats.constrainToRange(20F, 10F, 100F);
                    }
                }
                """,
              """ 
                class Test {
                    public float testMethod() {
                        return Math.clamp(20F, 10F, 100F);
                    }
                }
                """
            ),
            21)
        );
    }
}
