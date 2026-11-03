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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class HttpSessionInvalidateTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.servlet-3.0"))
          .recipe(new HttpSessionInvalidate());
    }

    @DocumentExample
    @Test
    void addLogoutBeforeInvalidate() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.servlet.ServletException;
              import javax.servlet.http.HttpServletResponse;
              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpSession;

              import java.io.IOException;

              class Foo {
                  void logOut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                      HttpSession session = req.getSession(false);
                      session.invalidate();
                      res.sendRedirect("login.html");
                  }
              }
              """,
            """
              import javax.servlet.ServletException;
              import javax.servlet.http.HttpServletResponse;
              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpSession;

              import java.io.IOException;

              class Foo {
                  void logOut(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                      HttpSession session = req.getSession(false);
                      req.logout();
                      session.invalidate();
                      res.sendRedirect("login.html");
                  }
              }
              """
          )
        );
    }

    @Test
    void addLogoutWhenSupertypeOfServletExceptionIsCaught() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.servlet.http.HttpServletRequest;

              class Foo {
                  void logOut(HttpServletRequest req) {
                      try {
                          req.getSession().invalidate();
                      } catch (Exception e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """,
            """
              import javax.servlet.http.HttpServletRequest;

              class Foo {
                  void logOut(HttpServletRequest req) {
                      try {
                          req.logout();
                          req.getSession().invalidate();
                      } catch (Exception e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void addLogoutWhenServletExceptionIsMultiCaught() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.servlet.ServletException;
              import javax.servlet.http.HttpServletResponse;
              import javax.servlet.http.HttpServletRequest;

              import java.io.IOException;

              class Foo {
                  void reauthenticate(HttpServletRequest req, HttpServletResponse res) {
                      try {
                          req.getSession().invalidate();
                          req.authenticate(res);
                      } catch (IOException | ServletException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """,
            """
              import javax.servlet.ServletException;
              import javax.servlet.http.HttpServletResponse;
              import javax.servlet.http.HttpServletRequest;

              import java.io.IOException;

              class Foo {
                  void reauthenticate(HttpServletRequest req, HttpServletResponse res) {
                      try {
                          req.logout();
                          req.getSession().invalidate();
                          req.authenticate(res);
                      } catch (IOException | ServletException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void servletExceptionNotHandled() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.http.HttpServletResponse;
                  import javax.servlet.http.HttpServletRequest;
                  import javax.servlet.http.HttpSession;

                  import java.io.IOException;

                  class Foo {
                      void logOut(HttpServletRequest req, HttpServletResponse res) throws IOException {
                          HttpSession session = req.getSession(false);
                          session.invalidate();
                          res.sendRedirect("login.html");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void alreadyLoggedOut() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.ServletException;
                  import javax.servlet.http.HttpServletRequest;

                  class Foo {
                      void logOut(HttpServletRequest req) throws ServletException {
                          req.logout();
                          req.getSession().invalidate();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void cannotFindServletRequest() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.ServletException;
                  import javax.servlet.http.HttpSession;

                  class Foo {
                      void logOut(HttpSession session) throws ServletException {
                          session.invalidate();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void notAStandaloneStatement() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.ServletException;
                  import javax.servlet.http.HttpServletRequest;
                  import javax.servlet.http.HttpSession;

                  class Foo {
                      void logOut(HttpServletRequest req) throws ServletException {
                          HttpSession session = req.getSession(false);
                          if (session != null) session.invalidate();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void lambdaInFieldInitializer() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.http.HttpSession;

                  import java.util.function.Consumer;

                  class Foo {
                      Consumer<HttpSession> invalidate = session -> {
                          session.invalidate();
                      };
                  }
                  """
              )
            );
        }

        @Test
        void lambdaInsideMethodThatThrowsServletException() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.servlet.ServletException;
                  import javax.servlet.http.HttpServletRequest;

                  class Foo {
                      Runnable logOut(HttpServletRequest req) throws ServletException {
                          return () -> {
                              req.getSession().invalidate();
                          };
                      }
                  }
                  """
              )
            );
        }
    }
}
