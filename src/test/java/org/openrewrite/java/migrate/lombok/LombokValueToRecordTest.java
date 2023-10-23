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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.*;

class LombokValueToRecordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new LombokValueToRecord(false))
          .allSources(s -> s.markers(javaVersion(17)))
          .parser(JavaParser.fromJavaVersion()
            .classpath(
              "lombok",
              "jackson-core"
            )
          );
    }

    @Test
    void valueAnnotatedClassWithUseExactOptionKeepsLombokToString() {
        //language=java
        rewriteRun(
          s -> s.recipe(new LombokValueToRecord(true)),
          java(
            """
              import lombok.Value;
                              
              @Value
              public class Test {
                  String field1;
                  
                  String field2;
              }
              """,
            """
              public record Test(
                  String field1,
                  
                  String field2) {
                  @Override
                  public String toString() {
                      return "Test(" +
                              "field1=" + field1 + ", " +
                              "field2=" + field2 +
                              ")";
                  }
              }
              """)
        );
    }

    @Test
    void convertOnlyValueAnnotatedClassWithoutDefaultValuesToRecord() {
        //language=java
        rewriteRun(
          // TODO: find a way to please type validation so this workaround is not required anymore
          s -> s.typeValidationOptions(TypeValidation.none()),
          java(
            """
              package example;
                              
              import lombok.Value;
                              
              @Value
              public class A {
                 String test;
              }
              """,
            """
              package example;
                              
              public record A(
                 String test) {
              }
              """
          ),
          java(
            """
              package example;
                              
              public class UserOfA {
                  
                  private final A record;
                  
                  public UserOfA() {
                      this.record = new A("some value");
                  }
                  
                  public String getRecordValue() {
                      return record.getTest();
                  }
              }
              """,
            """
              package example;
                              
              public class UserOfA {
                  
                  private final A record;
                  
                  public UserOfA() {
                      this.record = new A("some value");
                  }
                  
                  public String getRecordValue() {
                      return record.test();
                  }
              }
              """
          )
        );
    }

    @Nested
    class Unchanged {
        @Test
        void classWithExplicitConstructor() {
            //language=java
            rewriteRun(
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
              )
            );
        }

        @Test
        void classWithFieldAnnotations() {
            //language=java
            rewriteRun(
              java(
                """
                  import com.fasterxml.jackson.annotation.JsonProperty;
                  import lombok.Value;
                                  
                  @Value
                  public class A {
                                
                     @JsonProperty
                     String test;
                  }
                  """
              )
            );
        }

        @Test
        void classWithExplicitMethods() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;
                                  
                  @Value
                  public class A {
                     String test;
                     
                     public String getTest() {
                         return test;
                     }
                  }
                  """
              )
            );
        }

        @Test
        void genericClass() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;
                                  
                  @Value
                  public class A<T extends Object> {
                     T test;
                  }
                  """
              )
            );
        }

        @Test
        void nonJava17Class() {
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
        void classWithMultipleLombokAnnotations() {
            //language=java
            rewriteRun(
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
              )
            );
        }

        @Test
        void existingRecordsAreUnchanged() {
            //language=java
            rewriteRun(
              java(
                """
                  public record A(String test) {
                  }
                  """
              )
            );
        }

        @Test
        void classWithStaticField() {
            //language=java
            rewriteRun(
              java(
                """
                  import lombok.Value;
                                  
                  @Value
                  public class A {
                      static String disqualifyingField;
                      String test;
                  }
                  """
              )
            );
        }
    }
}