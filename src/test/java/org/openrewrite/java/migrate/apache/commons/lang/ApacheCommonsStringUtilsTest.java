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
    void ubertest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  String in = "foo";
                  String out = StringUtils.defaultString(in);
                  boolean out = StringUtils.isEmpty(in);
                  String[] out = StringUtils.split(in);
                  String[] out = StringUtils.split(in, ", ");
                  boolean out = StringUtils.equals(in, "string");
                  String out = StringUtils.chop(in);
                  String out = StringUtils.replace(in, "o", "z");
                  String out = StringUtils.strip(in);
                  String out = StringUtils.join(in);
                  String out = StringUtils.deleteWhitespace(in);
                  String out = StringUtils.abbreviate(in, 5);
                  String out = StringUtils.trimToEmpty(in);
                  String out = StringUtils.substringAfter(in, ",");
                  String out = StringUtils.right(in, 5);
                  String out = StringUtils.mid(in, 2, 4);
              }
              """,
            """
              import java.util.Objects;
                            
              class Foo {
                  String in = "foo";
                  String out = Objects.toString(in);
                  boolean out = in == null || in.isEmpty();
                  String[] out = in.split(" ");
                  String[] out = in == null ? null : in.split(", ");
                  boolean out = Objects.equals(in, "string");
                  String out = in.substring(0, in.length() - 1);
                  String out = in.replaceAll("o", "z");
                  String out = in.trim();
                  String out = String.join(in);
                  String out = in.replaceAll("\\s+", "");
                  String out = in.substring(0, Math.min(in.length(), 5));
                  String out = in != null ? in.trim() : "";
                  String out = in.substring(in.indexOf(",") + 1, in.length());
                  String out = in.substring(in.length() - 5, in.length() - 1);
                  String out = 2 + 4 < in.length() ? in.substring(2, 2 + 4) : in.substring(2, in.length() - 1);
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
    @Disabled
    void leftPad() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
              
              class Foo {
                  String in = "foo";
                  String out = StringUtils.leftPad(in, 4);
              }
              """,
            """
              class Foo {
                  String in = "foo";
                  String out = String.format("%" + 4 + "s", in);
              }
              """
          )
        );
    }
}
