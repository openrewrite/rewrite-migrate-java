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
                      
                      String str = StringUtils.abbreviate(in, 10);
                      str = StringUtils.capitalize(in);
                      str = StringUtils.chop(in);
                      str = StringUtils.contains(in, "search");
                      str = StringUtils.countMatches(in, "|");
                      str = StringUtils.defaultString(in);
                      str = StringUtils.deleteWhitespace(in);
                      boolean bool = StringUtils.endsWithIgnoreCase(in, "suffix");
                      bool = StringUtils.equalsIgnoreCase(in);
                      bool = StringUtils.equals(in, "other");
                      str = StringUtils.indexOfAny(in, "search"); // 
                      bool = StringUtils.isAlphanumericSpace(in);
                      bool = StringUtils.isAlphanumeric(in);
                      bool = StringUtils.isAlphaSpace(in);
                      bool = StringUtils.isAlpha(in);
                      bool = StringUtils.isEmpty(in);
                      str = StringUtils.join(in);
                      str = StringUtils.left(in, 4);
                      str = StringUtils.lowerCase(in);
                      str = StringUtils.mid(in, 3, 4);
                      str = StringUtils.overlay(in, "overlay", 3, 5);
                      str = StringUtils.remove(in, "r");
                      str = StringUtils.repeat(in, 4);
                      str = StringUtils.replaceOnce(in, "search", "replacement");
                      str = StringUtils.reverse(in);
                      str = StringUtils.right(in, 5);
                      str = StringUtils.split(in, ", ");
                      str = StringUtils.stripEnd(in, "suffix");
                      str = StringUtils.stripStart(in, "chars");
                      str = StringUtils.startsWith(in, "prefix");
                      str = StringUtils.split(in);
                      str = StringUtils.strip(in);
                      str = StringUtils.substringAfter(in, "|");
                      str = StringUtils.substring(in, 2, 4);
                      str = StringUtils.swapCase(in); //
                      str = StringUtils.trimToEmpty(in);
                      str = StringUtils.trimToNull(in);
                      str = StringUtils.trim(in);
                      str = StringUtils.upperCase(in);
                  }
              }
              """,
            """
              import java.util.Objects;
              import java.util.stream.IntStream;
                            
              class Foo {
                  void test() {
                      String in = "foo";
                      
                      String str = in == null || in.length() <= 10 ? in : in.substring(0, 10 - 3) + "...";
                      str = in == null ? null : in.substring(0, 1).toUpperCase() + in.substring(1);
                      str = in == null ? null : in.substring(0, in.length() - 1);
                      str = in == null || "search" == null ? null : in.contains("search");
                      str = (int) (in == null ? 0 : in.chars().filter(c -> c == "pattern").count());
                      str = in == null ? null : in.replaceAll("\\s+", "");
                      boolean bool = in == null ? null : in.replaceAll("\\s+", "");
                      bool = in.equalsIgnoreCase("other");
                      bool = Objects.equals(in, "other");
                      str = Objects.equals(in, "other");
                      bool = in == null ? false : in.matches("^[a-zA-Z0-9\\s]*$");
                      bool = in == null ? false : in.chars().allMatch(Character::isAlphabetic);
                      bool = in == null ? false : in.matches("[a-zA-Z\\s]+");
                      bool = in == null ? false : in.chars().allMatch(Character::isLetter);
                      bool = in == null || in.isEmpty();
                      str = in == null ? null : String.join("", in);
                      str = in == null ? null : in.substring(0, 4);
                      str = in == null ? null : in.toLowerCase();
                      str = in == null ? null : (3 + 3 < in.length() ? in.substring(3, 3 + 3) : in.substring(3, in.length() - 1));
                      str = in == null ? null : in.substring(0, 3) + "overlay" + in.substring(3);
                      str = in == null ? null : (in.endsWith("remove") ? in.substring(0, in.length() - "remove".length()) : in);
                      str = in == null ? null : new String(new char[3]).replace("\\0", in);
                      str = in == null ? null : in.replaceFirst(Pattern.quote("search"), "replacement");
                      str = in == null ? null : in.replaceAll("target", "replacement");
                      str = in == null ? null : new StringBuilder(in).reverse().toString();
                      str = in == null ? null : in.substring(in.length() - l, in.length() - 1);
                      str = in == null ? null : in.split(", ");
                      str = in == null ? null : (in.endsWith("suffix") ? in.substring(0, in.lastIndexOf("suffix")) : in);
                      str = in == null ? null : (in.startsWith("chars") ? in.substring("chars".length()) : in);
                      str = in == null || "prefix" == null ? null : in.startsWith("prefix");
                      str = in == null ? null : in.split(" ");
                      str = in == null ? null : in.trim();
                      str = in == null ? null : in.substring(in.indexOf("|") + 1, in.length());
                      str = in == null ? null : in.substring(3, 3);
                      str = in != null ? in.trim() : "";
                      str = in == null ? null : (in.trim() == null ? null : in.trim());
                      str = in == null ? null : in.trim();
                      str = in == null ? null : in.toUpperCase();
                      str = in == null ? null : Character.toLowerCase(in.charAt(0)) + in.substring(1);
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

    //TODO
    @Test
    void replacementWorksWithDotNotation() {
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
                      String test = (s == null ? null : in.trim()).toString();
                  }
              }
              """
          )
        );
    }
}
