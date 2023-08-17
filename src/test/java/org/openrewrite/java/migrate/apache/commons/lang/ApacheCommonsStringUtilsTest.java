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

    @Test
    @DocumentExample
    void ubertest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class Foo {
                  void bar() {
                      String in = "foo";

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

                      integer = StringUtils.countMatches(in, "|");

                      string = StringUtils.defaultString(in);
                      string = StringUtils.deleteWhitespace(in);

                      bool = StringUtils.endsWithIgnoreCase(in, "suffix");
                      bool = StringUtils.equalsIgnoreCase(in, "other");
                      bool = StringUtils.equals(in, "other");

                      integer = StringUtils.indexOfAny(in, "search");

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
                      string = StringUtils.repeat(in, 4);
                      string = StringUtils.repeat(in, ",", 4);
                      string = StringUtils.replaceOnce(in, "search", "replacement");
                      string = StringUtils.reverse(in);
                      string = StringUtils.right(in, 5);
                      string = StringUtils.rightPad(in, 5);
                      string = StringUtils.rightPad(in, 5, ' ');
                      string = StringUtils.rightPad(in, 5, " ");

                      array = StringUtils.split(in, ", ");
                      bool = StringUtils.startsWith(in, "prefix");
                      bool = StringUtils.startsWithAny(in, "prefix");
                      bool = StringUtils.startsWithIgnoreCase(in, "prefix");
                      array = StringUtils.stripAll(in);

                      string = StringUtils.stripEnd(in, "suffix");
                      string = StringUtils.stripStart(in, "chars");

                      bool = StringUtils.startsWith(in, "prefix");

                      array = StringUtils.split(in);

                      string = StringUtils.strip(in);
                      string = StringUtils.substringAfter(in, "|");
                      string = StringUtils.substring(in, 2, 4);
                      string = StringUtils.swapCase(in);
                      string = StringUtils.trimToEmpty(in);
                      string = StringUtils.trimToNull(in);
                      string = StringUtils.trim(in);
                      string = StringUtils.upperCase(in);
                  }
              }
              """,
            """
              import org.apache.commons.lang3.StringUtils;

              import java.util.Objects;
              import java.util.regex.Pattern;
              import java.util.stream.IntStream;

              class Foo {
                  void bar() {
                      String in = "foo";

                      // Reuse output variables for readability
                      String[] array;
                      boolean bool;
                      int integer;
                      String string;

                      // Test all methods in alphabetical order to only execute the slow recipes once
                      string = in == null || in.length() <= 10 ? in : in.substring(0, 10 - 3) + "...";
                      string = in == null ? null : in.substring(0, 1).toUpperCase() + in.substring(1);
                      string = StringUtils.center(in, 10);
                      string = StringUtils.center(in, 10, ' ');
                      string = StringUtils.center(in, 10, " ");
                      string = StringUtils.chomp(in);
                      string = in == null ? null : in.substring(0, in.length() - 1);

                      bool = StringUtils.contains(in, "search");

                      integer = StringUtils.countMatches(in, "|");

                      string = Objects.toString(in, "");
                      string = in == null ? null : in.replaceAll("\\s+", "");

                      bool = in != null && in.regionMatches(true, in.length() - "suffix".length(), "suffix", 0, "suffix".length());
                      bool = in != null && in.equalsIgnoreCase("other");
                      bool = Objects.equals(in, "other");

                      integer = IntStream.range(0, in.length()).filter(i -> "search".indexOf(in.charAt(i)) >= 0).min().orElse(-1);

                      bool = StringUtils.isAlphanumericSpace(in);
                      bool = in != null && in.chars().allMatch(Character::isAlphabetic);
                      bool = StringUtils.isAlphaSpace(in);
                      bool = in != null && !in.isEmpty() && in.chars().allMatch(Character::isLetter);
                      bool = in == null || in.isEmpty();

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
                      string = StringUtils.repeat(in, 4);
                      string = StringUtils.repeat(in, ",", 4);
                      string = in == null ? null : in.replaceFirst(Pattern.quote("search"), "replacement");
                      string = in == null ? null : new StringBuilder(in).reverse().toString();
                      string = StringUtils.right(in, 5);
                      string = StringUtils.rightPad(in, 5);
                      string = StringUtils.rightPad(in, 5, ' ');
                      string = StringUtils.rightPad(in, 5, " ");

                      array = in == null ? null : in.split(", ");
                      bool = StringUtils.startsWith(in, "prefix");
                      bool = StringUtils.startsWithAny(in, "prefix");
                      bool = StringUtils.startsWithIgnoreCase(in, "prefix");
                      array = StringUtils.stripAll(in);

                      string = in == null ? null : (in.endsWith("suffix") ? in.substring(0, in.lastIndexOf("suffix")) : in);
                      string = in == null ? null : (in.startsWith("chars") ? in.substring("chars".length()) : in);

                      bool = StringUtils.startsWith(in, "prefix");

                      array = in == null ? null : in.split(" ");

                      string = in == null ? null : in.trim();
                      string = in == null ? null : in.substring(in.indexOf("|") + 1, in.length());
                      string = in == null ? null : in.substring(2, 4);
                      string = in == null ? null : in.chars().map(c -> Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c)).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                      string = in != null ? in.trim() : "";
                      string = in == null ? null : (in.trim() == null ? null : in.trim());
                      string = in == null ? null : in.trim();
                      string = in == null ? null : in.toUpperCase();
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
                  void test(String s) {
                      String test = !StringUtils.equalsIgnoreCase(s, "other");
                  }
              }
              """,
            """
              class Foo {
                  void test(String s) {
                      String test = !(s != null && s.equalsIgnoreCase("other"));
                  }
              }
              """
          )
        );
    }

    @Test
    void inputMethodsNotCalledTwice() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
                            
              class Foo {
                  void test(String s) {
                      String test = StringUtils.strip(bar()).toString();
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
