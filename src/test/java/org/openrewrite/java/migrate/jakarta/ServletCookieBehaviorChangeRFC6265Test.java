/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ServletCookieBehaviorChangeRFC6265Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().
            classpathFromResources(new InMemoryExecutionContext(), "jakarta.servlet-api-4.0.2", "jakarta.servlet-api-6.0.0")).
          recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.ServletCookieBehaviorChangeRFC6265"));
    }

    @DocumentExample
    @Test
    void removeMethodsJakarta() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.servlet.ServletContext;
              import jakarta.servlet.SessionCookieConfig;
              import jakarta.servlet.http.HttpServlet;

              import jakarta.servlet.http.Cookie;

              public class TestJakarta extends HttpServlet {

                  public void test() {
                        Cookie cookie = new Cookie("test", "cookie");
                        cookie.setComment("comment");
                        cookie.getComment();
                        cookie.setVersion(1);
                        cookie.getVersion();

                        ServletContext servletContext = getServletContext();
                        SessionCookieConfig config = servletContext.getSessionCookieConfig();
                        config.getComment();
                        config.setComment("comment");
                  }
              }
              """,
                """
              package com.test;
              import jakarta.servlet.ServletContext;
              import jakarta.servlet.SessionCookieConfig;
              import jakarta.servlet.http.HttpServlet;

              import jakarta.servlet.http.Cookie;

              public class TestJakarta extends HttpServlet {

                  public void test() {
                        Cookie cookie = new Cookie("test", "cookie");

                        ServletContext servletContext = getServletContext();
                        SessionCookieConfig config = servletContext.getSessionCookieConfig();
                  }
              }
              """
          ));
    }

}
