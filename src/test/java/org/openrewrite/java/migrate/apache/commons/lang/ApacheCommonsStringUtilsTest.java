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
package org.openrewrite.java.migrate.apache.commons.lang;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"UnnecessaryCallToStringValueOf", "ConstantValue"})
class ApacheCommonsStringUtilsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"))
          .recipe(new ApacheCommonsStringUtilsRecipes());
    }

    @Test
    @DocumentExample
    void defaultString() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  String in = "foo";
                  String out = StringUtils.defaultString(in);
              }
              """,
            """
              import java.util.Objects;
                            
              class Foo {
                  String in = "foo";
                  String out = Objects.toString(in);
              }
              """
          )
        );
    }

    @Test
    void defaultStringStatic() {
        rewriteRun(
          //language=java
          java(
            """
              import static org.apache.commons.lang3.StringUtils.defaultString;
                            
              class Foo {
                  String in = "foo";
                  String out = defaultString(in);
              }
              """,
            """
              import java.util.Objects;
                            
              class Foo {
                  String in = "foo";
                  String out = Objects.toString(in);
              }
              """
          )
        );
    }

    @Test
    void isEmpty() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                  
              class Foo {
                  String in = "foo";
                  boolean out = StringUtils.isEmpty(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  boolean out = in == null || in.isEmpty();
              }
              """
          )
        );
    }

    @Test
    void splitTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                          
              class Foo {
                  String in = "foo";
                  String[] out = StringUtils.split(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String[] out = in.split(" ");
              }
              """
          )
        );
    }

    @Test
    @Disabled("Argument variant not covered yet")
    void splitWithArg() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                          
              class Foo {
                  String in = "foo";
                  String[] out = StringUtils.split(in, ", ");
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String[] out = in == null ? null : in.split(", ");
              }
              """
          )
        );
    }

    @Test
    void equalsTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                          
              class Foo {
                  String in = "foo";
                  boolean out = StringUtils.equals(in, "string");
              }
              """,
            """
              import java.util.Objects;
                          
              class Foo {
                  String in = "foo";
                  boolean out = Objects.equals(in, "string");
              }
              """
          )
        );
    }

    @Test
    void chopTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                  
              class Foo {
                  String in = "foo";
                  String out = StringUtils.chop(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = in.substring(0, in.length() - 1);
              }
              """
          )
        );
    }

    @Test
    void replaceTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                  
              class Foo {
                  String in = "foo";
                  String out = StringUtils.replace(in, "o", "z");
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = in.replaceAll("o", "z");
              }
              """
          )
        );
    }

    @Test
    void stripTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  String in = "foo";
                  String out = StringUtils.strip(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = in.trim();
              }
              """
          )
        );
    }

    @Test
    void joinTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  String in = "foo";
                  String out = StringUtils.join(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = String.join(in);
              }
              """
          )
        );
    }

    @Test
    void deleteWhitespaceTest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  String in = "foo";
                  String out = StringUtils.deleteWhitespace(in);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = in.replaceAll("\\s+", "");
              }
              """
          )
        );
    }
}
