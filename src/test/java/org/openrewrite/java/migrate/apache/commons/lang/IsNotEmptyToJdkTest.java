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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class IsNotEmptyToJdkTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3", "plexus-utils", "maven-shared-utils"))
          .recipe(new IsNotEmptyToJdk());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '#', textBlock = """
      org.apache.commons.lang3.StringUtils # StringUtils.isEmpty(first) # first == null || first.isEmpty()
      org.apache.commons.lang3.StringUtils # StringUtils.isNotEmpty(first) # first != null && !first.isEmpty()
      org.apache.maven.shared.utils.StringUtils # StringUtils.isEmpty(first) # first == null || first.isEmpty()
      org.apache.maven.shared.utils.StringUtils # StringUtils.isNotEmpty(first) # first != null && !first.isEmpty()
      org.codehaus.plexus.util.StringUtils # StringUtils.isEmpty(first) # first == null || !first.isEmpty()
      org.codehaus.plexus.util.StringUtils # StringUtils.isNotEmpty(first) # first != null && !first.isEmpty()
      """)
    void replaceNow(String classname, String beforeLine, String afterLine) {
        // language=java
        rewriteRun(
          java(
            """
              import %s;

              class A {
                  boolean test(String first) {
                      return %s;
                  }
              }
              """.formatted(classname, beforeLine),
            """
              class A {
                  boolean test(String first) {
                      return %s;
                  }
              }
              """.formatted(afterLine)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '#', textBlock = """
      org.apache.commons.lang3.StringUtils # !StringUtils.isEmpty(first) # first != null && !first.isEmpty()
      org.apache.commons.lang3.StringUtils # !StringUtils.isNotEmpty(first) # first == null || first.isEmpty()
      org.apache.maven.shared.utils.StringUtils # !StringUtils.isEmpty(first) # first != null && !first.isEmpty()
      org.apache.maven.shared.utils.StringUtils # !StringUtils.isNotEmpty(first) # first == null || first.isEmpty()
      org.codehaus.plexus.util.StringUtils # !StringUtils.isEmpty(first) # first != null && !first.isEmpty()
      org.codehaus.plexus.util.StringUtils # !StringUtils.isNotEmpty(first) # first == null || !first.isEmpty()
      """)
    void replaceSoon(String classname, String beforeLine, String afterLine) {
        // language=java
        rewriteRun(
          java(
            """
              import %s;

              class A {
                  boolean test(String first) {
                      return %s;
                  }
              }
              """.formatted(classname, beforeLine),
            """
              class A {
                  boolean test(String first) {
                      return %s;
                  }
              }
              """.formatted(afterLine)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '#', textBlock = """
      org.apache.commons.lang3.StringUtils # StringUtils.isEmpty(foo())
      org.apache.commons.lang3.StringUtils # StringUtils.isEmpty(first + second)
      org.apache.commons.lang3.StringUtils # StringUtils.isNotEmpty(foo())
      org.apache.commons.lang3.StringUtils # StringUtils.isNotEmpty(first + second)
      org.apache.maven.shared.utils.StringUtils # StringUtils.isEmpty(foo())
      org.codehaus.plexus.util.StringUtils # StringUtils.isEmpty(foo())
      """)
    void retain(String classname, String beforeLine) {
        // language=java
        rewriteRun(
          java(
            """
              %s;

              class A {
                  boolean test(String first, String second) {
                      return %s;
                  }
                  private String foo() {
                      return "foo";
                  }
              }
              """.formatted(classname, beforeLine)));
    }
}