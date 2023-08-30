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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"Deprecation", "UnusedAssignment", "DataFlowIssue", "StringOperationCanBeSimplified"})
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
                  void bar(String in, CharSequence cs) {
                      // Reuse output variables for readability
                      String[] array;
                      boolean bool;
                      int integer;
                      String string;

                      // Test all methods in alphabetical order to only execute the slow recipes once
                      string = StringUtils.abbreviate(in, 10);
                      string = StringUtils.capitalize(in);
                      string = StringUtils.center(in, 10);
                      string = StringUtils.center(in, 10, ' ');
                      string = StringUtils.center(in, 10, " ");
                      string = StringUtils.chomp(in);
                      string = StringUtils.chop(in);

                      bool = StringUtils.contains(in, "search");

                      integer = StringUtils.countMatches(in, '|');
                      integer = StringUtils.countMatches(in, "|");

                      string = StringUtils.defaultString(in);
                      string = StringUtils.defaultString(in, "nil");
                      string = StringUtils.deleteWhitespace(in);

                      //bool = StringUtils.endsWithIgnoreCase(in, "suffix");
                      bool = StringUtils.equalsIgnoreCase(in, "other");
                      bool = StringUtils.equals(in, "other");
                      bool = StringUtils.equals(cs, "other");
                      bool = StringUtils.equals(cs, cs);

                      //integer = StringUtils.indexOfAny(in, "search");

                      bool = StringUtils.isAlphanumericSpace(in);
                      bool = StringUtils.isAlphanumeric(in);
                      bool = StringUtils.isAlphaSpace(in);
                      bool = StringUtils.isAlpha(in);
                      bool = StringUtils.isEmpty(in);

                      string = StringUtils.join(in);
                      string = StringUtils.joinWith(",", in);
                      string = StringUtils.left(in, 4);
                      string = StringUtils.leftPad(in, 4);
                      string = StringUtils.leftPad(in, 4, ' ');
                      string = StringUtils.leftPad(in, 4, " ");
                      string = StringUtils.lowerCase(in);
                      string = StringUtils.mid(in, 3, 4);
                      string = StringUtils.overlay(in, "overlay", 3, 5);

                      string = StringUtils.remove(in, "r");
                      string = StringUtils.removeEnd(in, "suffix");
                      string = StringUtils.repeat(in, 4);
                      string = StringUtils.repeat(in, ",", 4);
                      string = StringUtils.replace(in, "search", "replacement");
                      //string = StringUtils.replaceOnce(in, "search", "replacement");
                      string = StringUtils.reverse(in);
                      string = StringUtils.right(in, 5);
                      string = StringUtils.rightPad(in, 5);
                      string = StringUtils.rightPad(in, 5, ' ');
                      string = StringUtils.rightPad(in, 5, " ");

                      array = StringUtils.split(in);
                      //array = StringUtils.split(in, "*");
                      bool = StringUtils.startsWith(in, "prefix");
                      bool = StringUtils.startsWithAny(in, "prefix");
                      bool = StringUtils.startsWithIgnoreCase(in, "prefix");
                      array = StringUtils.stripAll(in);

                      string = StringUtils.strip(in);
                      string = StringUtils.stripEnd(in, "suffix");
                      string = StringUtils.stripStart(in, "chars");

                      bool = StringUtils.startsWith(in, "prefix");

                      string = StringUtils.substringAfter(in, "|");
                      string = StringUtils.substring(in, 2, 4);
                      string = StringUtils.swapCase(in);
                      string = StringUtils.trimToEmpty(in);
                      string = StringUtils.trimToNull(in);
                      string = StringUtils.trim(in);
                      string = StringUtils.upperCase(in);
                      string = StringUtils.uncapitalize(in);
                  }
              }
              """,
            """
              import org.apache.commons.lang3.StringUtils;

              import java.util.Objects;

              class Foo {
                  void bar(String in, CharSequence cs) {
                      // Reuse output variables for readability
                      String[] array;
                      boolean bool;
                      int integer;
                      String string;

                      // Test all methods in alphabetical order to only execute the slow recipes once
                      string = in == null || in.length() <= 10 ? in : in.substring(0, 10 - 3) + "...";
                      string = in == null || in.isEmpty() || Character.isTitleCase(in.charAt(0)) ? in : Character.toTitleCase(in.charAt(0)) + in.substring(1);
                      string = StringUtils.center(in, 10);
                      string = StringUtils.center(in, 10, ' ');
                      string = StringUtils.center(in, 10, " ");
                      string = StringUtils.chomp(in);
                      string = StringUtils.chop(in);

                      bool = StringUtils.contains(in, "search");

                      integer = StringUtils.countMatches(in, '|');
                      integer = StringUtils.countMatches(in, "|");

                      string = Objects.toString(in, "");
                      string = Objects.toString(in, "nil");
                      string = in == null ? null : in.replaceAll("\\s+", "");

                      //bool = StringUtils.endsWithIgnoreCase(in, "suffix");
                      bool = in != null && in.equalsIgnoreCase("other");
                      bool = Objects.equals(in, "other");
                      bool = StringUtils.equals(cs, "other");
                      bool = StringUtils.equals(cs, cs);

                      //integer = StringUtils.indexOfAny(in, "search");

                      bool = StringUtils.isAlphanumericSpace(in);
                      bool = in != null && !in.isEmpty() && in.chars().allMatch(Character::isLetterOrDigit);
                      bool = StringUtils.isAlphaSpace(in);
                      bool = in != null && !in.isEmpty() && in.chars().allMatch(Character::isLetter);
                      bool = StringUtils.isEmpty(in);

                      string = StringUtils.join(in);
                      string = StringUtils.joinWith(",", in);
                      string = StringUtils.left(in, 4);
                      string = StringUtils.leftPad(in, 4);
                      string = StringUtils.leftPad(in, 4, ' ');
                      string = StringUtils.leftPad(in, 4, " ");
                      string = in == null ? null : in.toLowerCase();
                      string = StringUtils.mid(in, 3, 4);
                      string = StringUtils.overlay(in, "overlay", 3, 5);

                      string = StringUtils.remove(in, "r");
                      string = in == null || in.isEmpty() || !in.endsWith("suffix") ? in : in.substring(0, in.length() - "suffix".length());
                      string = StringUtils.repeat(in, 4);
                      string = StringUtils.repeat(in, ",", 4);
                      string = in == null || in.isEmpty() ? in : in.replace("search", "replacement");
                      //string = StringUtils.replaceOnce(in, "search", "replacement");
                      string = in == null ? null : new StringBuilder(in).reverse().toString();
                      string = StringUtils.right(in, 5);
                      string = StringUtils.rightPad(in, 5);
                      string = StringUtils.rightPad(in, 5, ' ');
                      string = StringUtils.rightPad(in, 5, " ");

                      array = in == null ? null : in.split("\\s+");
                      //array = StringUtils.split(in, "*");
                      bool = StringUtils.startsWith(in, "prefix");
                      bool = StringUtils.startsWithAny(in, "prefix");
                      bool = StringUtils.startsWithIgnoreCase(in, "prefix");
                      array = StringUtils.stripAll(in);

                      string = in == null ? null : in.trim();
                      string = StringUtils.stripEnd(in, "suffix");
                      string = StringUtils.stripStart(in, "chars");

                      bool = StringUtils.startsWith(in, "prefix");

                      string = StringUtils.substringAfter(in, "|");
                      string = StringUtils.substring(in, 2, 4);
                      string = StringUtils.swapCase(in);
                      string = in == null ? "" : in;
                      string = in == null || in.trim().isEmpty() ? null : in.trim();
                      string = in == null ? null : in.trim();
                      string = in == null ? null : in.toUpperCase();
                      string = StringUtils.uncapitalize(in);
                  }
              }
              """
          )
        );
    }

    @Test
    void canCallMethodOnResult() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  void test(String s) {
                      String test = StringUtils.strip(s).toString();
                  }
              }
              """,
            """
              class Foo {
                  void test(String s) {
                      String test = (s == null ? null : s.trim()).toString();
                  }
              }
              """
          )
        );
    }

    @Test
    void invertedBooleanHandled() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  void test(String s, String other) {
                      String test = !StringUtils.equalsIgnoreCase(s, other);
                  }
              }
              """,
            """
              class Foo {
                  void test(String s, String other) {
                      String test = !(s == null && other == null || s != null && s.equalsIgnoreCase(other));
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-templating/issues/27")
    void inputMethodsNotCalledTwice() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  void test(String s) {
                      String test = StringUtils.strip(bar).toString();
                  }
                  String bar() {
                      return "bar";
                  }
              }
              """
          )
        );
    }
}
