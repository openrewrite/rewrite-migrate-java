/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateTomcatServletMappingTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateTomcatServletMapping())
          .parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package org.apache.catalina;
              public interface Context {
                  void addServletMapping(String pattern, String name);
                  void addServletMapping(String pattern, String name, boolean jspWildCard);
                  void addServletMappingDecoded(String pattern, String name);
                  void addServletMappingDecoded(String pattern, String name, boolean jspWildCard);
              }
              """,
            """
              package org.apache.tomcat.util.buf;
              public class UDecoder {
                  public static String URLDecode(String pattern, java.nio.charset.Charset charset) {
                      return pattern;
                  }
              }
              """));
    }

    @Test
    void retainsPatternDecodingAndOverloads() {
        rewriteRun(
          java(
            """
              import org.apache.catalina.Context;
              class WebApp {
                  void mapping(Context context, String pattern) {
                      context.addServletMapping("/*", "default");
                      context.addServletMapping(pattern, "servlet");
                      context.addServletMapping("/with%20space", "jsp", true);
                  }
              }
              """,
            """
              import org.apache.catalina.Context;
              import org.apache.tomcat.util.buf.UDecoder;

              import java.nio.charset.StandardCharsets;

              class WebApp {
                  void mapping(Context context, String pattern) {
                      context.addServletMappingDecoded("/*", "default");
                      context.addServletMappingDecoded(UDecoder.URLDecode(pattern, StandardCharsets.ISO_8859_1), "servlet");
                      context.addServletMappingDecoded(UDecoder.URLDecode("/with%20space", StandardCharsets.ISO_8859_1), "jsp", true);
                  }
              }
              """
          )
        );
    }
}
