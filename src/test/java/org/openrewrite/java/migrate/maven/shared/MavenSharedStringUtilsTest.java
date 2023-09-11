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
package org.openrewrite.java.migrate.maven.shared;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"Deprecation", "UnusedAssignment", "DataFlowIssue", "StringOperationCanBeSimplified"})
class MavenSharedStringUtilsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("maven-shared-utils"))
          .recipe(new MavenSharedStringUtilsRecipes());
    }

    @Test
    @DocumentExample
    void ubertest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.maven.shared.utils.StringUtils;

              class Foo {
                  void bar(String in, CharSequence cs) {
                      // Reuse output variables for readability
                      String[] array;
                      boolean bool;
                      String string;

                      // Test all methods in alphabetical order to only execute the slow recipes once
                      string = StringUtils.abbreviate(in, 10);
                      string = StringUtils.capitalise(in);
                      string = StringUtils.defaultString(in);
                      string = StringUtils.defaultString(in, "nil");
                      string = StringUtils.deleteWhitespace(in);

                      bool = StringUtils.equalsIgnoreCase(in, "other");
                      bool = StringUtils.equals(in, "other");
                      //bool = StringUtils.equals(cs, "other");

                      bool = StringUtils.isAlphanumeric(in);
                      bool = StringUtils.isAlpha(in);
                      bool = StringUtils.isEmpty(in);

                      string = StringUtils.lowerCase(in);
                      string = StringUtils.replace(in, "search", "replacement");
                      string = StringUtils.reverse(in);
                      array = StringUtils.split(in);
                      string = StringUtils.strip(in);
                      string = StringUtils.trim(in);
                      string = StringUtils.upperCase(in);
                  }
              }
              """,
            """
              import org.apache.maven.shared.utils.StringUtils;

              import java.util.Objects;

              class Foo {
                  void bar(String in, CharSequence cs) {
                      // Reuse output variables for readability
                      String[] array;
                      boolean bool;
                      String string;
                           
                      // Test all methods in alphabetical order to only execute the slow recipes once
                      string = in == null || in.length() <= 10 ? in : in.substring(0, 10 - 3) + "...";
                      string = in == null || in.isEmpty() || Character.isTitleCase(in.charAt(0)) ? in : Character.toTitleCase(in.charAt(0)) + in.substring(1);
                      string = Objects.toString(in, "");
                      string = Objects.toString(in, "nil");
                      string = in == null ? null : in.replaceAll("\\s+", "");
                           
                      bool = in != null && in.equalsIgnoreCase("other");
                      bool = Objects.equals(in, "other");
                      //bool = StringUtils.equals(cs, "other");
                           
                      bool = in != null && !in.isEmpty() && in.chars().allMatch(Character::isLetterOrDigit);
                      bool = in != null && !in.isEmpty() && in.chars().allMatch(Character::isLetter);
                      bool = StringUtils.isEmpty(in);
                           
                      string = in == null ? null : in.toLowerCase();
                      string = in == null || in.isEmpty() ? in : in.replace("search", "replacement");
                      string = in == null ? null : new StringBuilder(in).reverse().toString();
                      array = in == null ? null : in.split("\\s+");
                      string = in == null ? null : in.trim();
                      string = in == null ? null : in.trim();
                      string = in == null ? null : in.toUpperCase();
                  }
              }
              """
          )
        );
    }

}
