/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceLocalizedStreamMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().dependsOn(
            //language=java
            """
              package com.test;
              import java.io.InputStream;
              import java.io.OutputStream;
              public class Runtime1 {
                  public InputStream getLocalizedInputStream1(InputStream in) {
                      return in;
                  }
                  public OutputStream getLocalizedOutputStream1(OutputStream out) {
                      return out;
                  }
              }
              """
          ))
          .recipe(new ReplaceLocalizedStreamMethods(
            "com.test.Runtime1 getLocalizedInputStream1(java.io.InputStream)",
            "com.test.Runtime1 getLocalizedOutputStream1(java.io.OutputStream)"));
    }

    @Test
    @DocumentExample
    void replaceGetLocalizedInputStream() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import java.io.InputStream;

              class Test {
                  void exampleMethod(InputStream in) {
                      Runtime1 rt = null;
                      InputStream newStream = rt.getLocalizedInputStream1(in);
                  }
              }
              """,
            """
              package com.test;
              import java.io.InputStream;

              class Test {
                  void exampleMethod(InputStream in) {
                      Runtime1 rt = null;
                      InputStream newStream = in;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceGetLocalizedOutputStream() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import java.io.OutputStream;

              class Test {
                  void exampleMethod(OutputStream out) {
                      Runtime1 rt = null;
                      OutputStream newStream = rt.getLocalizedOutputStream1(out);
                  }
              }
              """,
            """
              package com.test;
              import java.io.OutputStream;

              class Test {
                  void exampleMethod(OutputStream out) {
                      Runtime1 rt = null;
                      OutputStream newStream = out;
                  }
              }
              """
          )
        );
    }
}
