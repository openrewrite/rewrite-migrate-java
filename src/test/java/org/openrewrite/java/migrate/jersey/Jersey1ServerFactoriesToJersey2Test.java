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
package org.openrewrite.java.migrate.jersey;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class Jersey1ServerFactoriesToJersey2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ServerFactoriesToJersey2())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(),
              "jersey-core-1", "jersey-server-1", "jersey-grizzly2-1", "javax.ws.rs-api-2",
              "grizzly-framework-2.2", "grizzly-http-server-2.2"));
    }

    @DocumentExample
    @Test
    void jdkHttpServerStaysUnstarted() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.container.httpserver.HttpServerFactory;
              import com.sun.jersey.api.core.ResourceConfig;
              import com.sun.net.httpserver.HttpServer;

              import java.io.IOException;
              import java.net.URI;

              class Main {
                  static HttpServer start(ResourceConfig rc, URI uri) throws IOException {
                      HttpServer server = HttpServerFactory.create("http://localhost:9998/", rc);
                      server.start();
                      HttpServer other = HttpServerFactory.create(uri, rc);
                      return other;
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.ResourceConfig;
              import com.sun.net.httpserver.HttpServer;
              import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

              import java.io.IOException;
              import java.net.URI;

              class Main {
                  static HttpServer start(ResourceConfig rc, URI uri) throws IOException {
                      HttpServer server = JdkHttpServerFactory.createHttpServer(URI.create("http://localhost:9998/"), rc, false);
                      server.start();
                      HttpServer other = JdkHttpServerFactory.createHttpServer(uri, rc, false);
                      return other;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu,
              "org.glassfish.jersey.jdkhttp.JdkHttpServerFactory createHttpServer(java.net.URI, org.glassfish.jersey.server.ResourceConfig, boolean)"))
              .hasSize(2))
          )
        );
    }

    @Test
    void grizzlyServerStaysStarted() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
              import com.sun.jersey.api.core.ResourceConfig;
              import org.glassfish.grizzly.http.server.HttpServer;

              import java.io.IOException;

              class Main {
                  static HttpServer start(ResourceConfig rc) throws IOException {
                      return GrizzlyServerFactory.createHttpServer("http://localhost:9998/", rc);
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.ResourceConfig;
              import org.glassfish.grizzly.http.server.HttpServer;
              import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

              import java.io.IOException;
              import java.net.URI;

              class Main {
                  static HttpServer start(ResourceConfig rc) throws IOException {
                      return GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:9998/"), rc);
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu,
              "org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory createHttpServer(java.net.URI, org.glassfish.jersey.server.ResourceConfig)"))
              .singleElement()
              .satisfies(m -> assertThat(TypeUtils.isOfClassType(((J.MethodInvocation) m).getType(), "org.glassfish.grizzly.http.server.HttpServer")).isTrue()))
          )
        );
    }

    @Test
    void catchOfIOExceptionCatchesProcessingException() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
              import com.sun.jersey.api.container.httpserver.HttpServerFactory;
              import com.sun.jersey.api.core.ResourceConfig;
              import com.sun.net.httpserver.HttpServer;

              import java.io.IOException;
              import java.io.InputStream;
              import java.net.URI;

              class Main {
                  static void start(ResourceConfig rc, URI uri, InputStream in) {
                      try {
                          HttpServer server = HttpServerFactory.create(uri, rc);
                          server.start();
                      } catch (IOException e) {
                          throw new IllegalStateException(e);
                      }
                      try {
                          GrizzlyServerFactory.createHttpServer(uri, rc);
                          in.read();
                      } catch (IOException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.ResourceConfig;
              import com.sun.net.httpserver.HttpServer;
              import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
              import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;

              import java.io.IOException;

              import javax.ws.rs.ProcessingException;
              import java.io.InputStream;
              import java.net.URI;

              class Main {
                  static void start(ResourceConfig rc, URI uri, InputStream in) {
                      try {
                          HttpServer server = JdkHttpServerFactory.createHttpServer(uri, rc, false);
                          server.start();
                      } catch (ProcessingException e) {
                          throw new IllegalStateException(e);
                      }
                      try {
                          GrizzlyHttpServerFactory.createHttpServer(uri, rc);
                          in.read();
                      } catch (IOException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void ioExceptionInMultiCatch() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
              import com.sun.jersey.api.core.ResourceConfig;

              import java.io.IOException;
              import java.net.URI;

              class Main {
                  static void start(ResourceConfig rc, URI uri) {
                      try {
                          GrizzlyServerFactory.createHttpServer(uri, rc);
                      } catch (IOException | IllegalArgumentException e) {
                          throw new IllegalStateException(e);
                      }
                      try {
                          GrizzlyServerFactory.createHttpServer(uri, rc);
                      } catch (IOException | RuntimeException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.ResourceConfig;
              import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

              import java.net.URI;

              import javax.ws.rs.ProcessingException;

              class Main {
                  static void start(ResourceConfig rc, URI uri) {
                      try {
                          GrizzlyHttpServerFactory.createHttpServer(uri, rc);
                      } catch (ProcessingException | IllegalArgumentException e) {
                          throw new IllegalStateException(e);
                      }
                      try {
                          GrizzlyHttpServerFactory.createHttpServer(uri, rc);
                      } catch (RuntimeException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesClasspathScanningOverloadsAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
              import com.sun.jersey.api.container.httpserver.HttpServerFactory;
              import com.sun.net.httpserver.HttpServer;

              import java.io.IOException;

              class Main {
                  static Object start() throws IOException {
                      HttpServer server = HttpServerFactory.create("http://localhost:9998/");
                      return GrizzlyServerFactory.createHttpServer("http://localhost:9999/");
                  }
              }
              """
          )
        );
    }
}
