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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("ConstantValue")
class StringUtilsMethodToJdkTemplateTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    void defaultStringObjectsToString() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils defaultString(java.lang.String)",
            "Objects.toString(#{any()})",
            new String[]{"java.util.Objects"},
            null)),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;

            class Test {
               String s = StringUtils.defaultString("foo");
            }
            """, """
            import java.util.Objects;

            class Test {
               String s = Objects.toString("foo");
            }
            """)
        );
    }

    @Test
    void isBlankToStringIsBlank() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils isBlank(java.lang.CharSequence)",
            "#{any()}.isBlank()",
            null,
            null
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class Test {
                  void method() {
                      boolean b = StringUtils.isBlank("hello world");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      boolean b = "hello world".isBlank();
                  }
              }
              """
          )
        );
    }

    @Test
    void methodHasTwoArguments() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils split(java.lang.String, java.lang.String)",
            "#{any()}.split(#{any()})",
            null,
            null
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class Test {
                  void method() {
                      String[] split = StringUtils.split("hello, world", ", ");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String[] split = "hello, world".split(", ");
                  }
              }
              """
          )
        );
    }

    @Test
    void methodHasThreeArguments() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils replace(java.lang.String, java.lang.String, java.lang.String)",
            "#{any()}.replaceAll(#{any()}, #{any()})",
            null,
            null
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class Test {
                  void method() {
                      String rep = StringUtils.replace("hello world", "world", "rewrite");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String rep = "hello world".replaceAll("world", "rewrite");
                  }
              }
              """
          )
        );
    }

    @Test
    void oneArgAndTemplateHasTwoParameters() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils chop(java.lang.String)",
            "#{any()}.substring(0, #{any()}.length() - 1)",
            null,
            new Integer[]{0, 0}
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;

              class Test {
                  void method() {
                      String chop = StringUtils.chop("hello world");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String chop = "hello world".substring(0, "hello world".length() - 1);
                  }
              }
              """
          )
        );
    }

    @Test
    void parametersNotInSameOrderAsArgs() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils repeat(java.lang.String, int)",
            "new String(new char[#{any(int)}]).replace(\"\0\", #{any(java.lang.String)})",
            null,
            new Integer[]{1, 0}
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
              
              class Test {
                  void method() {
                      String repeat = StringUtils.repeat("string", 5);
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String repeat = new String(new char[5]).replace("\0", "string");
                  }
              }
              """
          )
        );
    }

    @Test
    void templateHasALotOfParametersOutOfOrder() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils stripEnd(java.lang.String, java.lang.String)",
            "(#{any()}.endsWith(#{any()}) ? #{any()}.substring(0, #{any()}.lastIndexOf(#{any()})) : #{any()})",
            null,
            new Integer[]{0, 1, 0, 0, 1, 0}
          )),
          //language=java
          java(
            """
              import org.apache.commons.lang3.StringUtils;
    
              class Test {
                  void method() {
                      String x = StringUtils.stripEnd("hello world", "ld");
                  }
              }
              """,
            """
              class Test {
                  void method() {
                      String x = ("hello world".endsWith("ld") ? "hello world".substring(0, "hello world".lastIndexOf("ld")) : "hello world");
                  }
              }
              """
          )
        );
    }

}