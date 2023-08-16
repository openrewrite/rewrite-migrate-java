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

@SuppressWarnings({"UnnecessaryCallToStringValueOf", "ConstantValue", "UnusedAssignment", "DataFlowIssue", "StringOperationCanBeSimplified"})
class ApacheCommonsStringUtilsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"))
          .recipe(new ApacheCommonsStringUtilsRecipes());
    }

    // TODO: Test for putting parentheses around replacements

    @Test
    @DocumentExample
    void ubertest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  void test() {
                      String in = "foo";
                      String outString = StringUtils.defaultString(in);
                      boolean outBool = StringUtils.isEmpty(in);
                      String[] outArray = StringUtils.split(in);
                      outArray = StringUtils.split(in, ", ");
                      outBool = StringUtils.equals(in, "string");
                      outString = StringUtils.chop(in);
                      outString = StringUtils.replace(in, "o", "z");
                      outString = StringUtils.strip(in);
                      outString = StringUtils.join(in);
                      outString = StringUtils.deleteWhitespace(in);
                      outString = StringUtils.abbreviate(in, 5);
                      outString = StringUtils.trimToEmpty(in);
                      outString = StringUtils.substringAfter(in, ",");
                      outString = StringUtils.right(in, 5);
                      outString = StringUtils.mid(in, 2, 4);
                      outString = StringUtils.stripStart(in, "chars");
                      outString = StringUtils.uncapitalize(in);
                      outString = StringUtils.capitalize(in);
                      outString = StringUtils.removeEnd(in, "remove");
                      int outInt = StringUtils.countMatches(in, "pattern");
                      outString = StringUtils.trimToNull(in);
                      outInt = StringUtils.indexOfAny(in, "search");
                      outString = StringUtils.swapCase(in);
                      outBool = StringUtils.isAlpha(in);
                      outBool = StringUtils.isAlphaSpace(in);
                  }
              }
              """,
            """
              import java.util.Objects;
              import java.util.stream.IntStream;
                            
              class Foo {
                  void test() {
                      String in = "foo";
                      String outString = Objects.toString(in);
                      boolean outBool = in == null || in.isEmpty();
                      String[] outArray = in.split(" ");
                      outArray = in == null ? null : in.split(", ");
                      outBool = Objects.equals(in, "string");
                      outString = in.substring(0, in.length() - 1);
                      outString = in.replaceAll("o", "z");
                      outString = in.trim();
                      outString = String.join(in);
                      outString = in.replaceAll("\\s+", "");
                      outString = in.substring(0, Math.min(in.length(), 5));
                      outString = in != null ? in.trim() : "";
                      outString = in.substring(in.indexOf(",") + 1, in.length());
                      outString = in.substring(in.length() - 5, in.length() - 1);
                      outString = 2 + 4 < in.length() ? in.substring(2, 2 + 4) : in.substring(2, in.length() - 1);                 
                      outString = in.startsWith("chars") ? in.substring("chars".length()) : in;
                      outString = Character.toLowerCase(in.charAt(0)) + in.substring(1);
                      outString = in == null ? null : in.substring(0, 1).toUpperCase() + in.substring(1);
                      outString = in.endsWith("remove") ? in.substring(0, in.length() - "remove".length()) : in;
                      int outInt = in.chars().filter(c -> c == "pattern").count();
                      outString = in == null || in.trim() == null ? null : in.trim();
                      outInt = IntStream.range(0, in.length()).filter((i) -> "search".indexOf(in.charAt(i)) >= 0).findFirst().orElse(-1);
                      outString = in.chars().map((c) -> Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                      outBool = in.chars().allMatch(Character::isLetter);
                      outBool = in.matches("[a-zA-Z\\s]+");
                  }
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

    @Test
    void chompTest() { // TODO: Fix this test
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
              
              class Foo {
                  void test(String s) {
                      String test = StringUtils.chomp(s);
                  }
              }
              """,
            """
              class Foo {
                  void test(String s) {
                      String test = s.endsWith("\\n") ? s.substring(0, s.length() - 1) : s;
                  }
              }
              """
          )
        );
    }
}
