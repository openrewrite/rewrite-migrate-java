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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

class MigrateToJersey2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jersey.MigrateToJersey2")
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1.19", "jersey-core-1.19", "javax.ws.rs-api-2.1"));
    }

    private static String jersey2Version(String pom) {
        Matcher matcher = Pattern.compile("<artifactId>jersey-client</artifactId>\\s*<version>(2\\.\\d+)</version>").matcher(pom);
        assertThat(matcher.find()).as("jersey-client 2.x in %s", pom).isTrue();
        return matcher.group(1);
    }

    @DocumentExample
    @Test
    void migrateClient() {
        rewriteRun(
          mavenProject("client",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>client</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-client</artifactId>
                            <version>1.19.4</version>
                        </dependency>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-json</artifactId>
                            <version>1.19.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  String version = jersey2Version(pom);
                  assertThat(pom).isEqualTo(
                    //language=xml
                    """
                      <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>com.example</groupId>
                          <artifactId>client</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                              <dependency>
                                  <groupId>org.glassfish.jersey.core</groupId>
                                  <artifactId>jersey-client</artifactId>
                                  <version>%1$s</version>
                              </dependency>
                              <dependency>
                                  <groupId>org.codehaus.jackson</groupId>
                                  <artifactId>jackson-jaxrs</artifactId>
                                  <version>1.9.13</version>
                              </dependency>
                              <dependency>
                                  <groupId>org.codehaus.jackson</groupId>
                                  <artifactId>jackson-xc</artifactId>
                                  <version>1.9.13</version>
                              </dependency>
                              <dependency>
                                  <groupId>org.glassfish.jersey.inject</groupId>
                                  <artifactId>jersey-hk2</artifactId>
                                  <version>%1$s</version>
                                  <scope>runtime</scope>
                              </dependency>
                          </dependencies>
                      </project>""".formatted(version));
                  return pom;
              })
            ),
            srcMainJava(
              //language=java
              java(
                """
                  import com.sun.jersey.api.client.Client;
                  import com.sun.jersey.api.client.ClientResponse;
                  import com.sun.jersey.api.client.UniformInterfaceException;
                  import com.sun.jersey.api.client.WebResource;

                  import javax.ws.rs.core.MediaType;

                  class ItemClient {
                      private final WebResource items;

                      ItemClient(Client client, String url) {
                          items = client.resource(url).path("items");
                      }

                      String find(String id) {
                          try {
                              return items.path(id).accept(MediaType.APPLICATION_JSON).get(String.class);
                          } catch (UniformInterfaceException e) {
                              return null;
                          }
                      }

                      int create(String json) {
                          ClientResponse response = items.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, json);
                          if (response.getStatus() != ClientResponse.Status.CREATED.getStatusCode()) {
                              throw new IllegalStateException(response.getEntity(String.class));
                          }
                          return response.getStatus();
                      }
                  }
                  """,
                """
                  import javax.ws.rs.WebApplicationException;
                  import javax.ws.rs.client.Client;
                  import javax.ws.rs.client.Entity;
                  import javax.ws.rs.client.WebTarget;
                  import javax.ws.rs.core.MediaType;
                  import javax.ws.rs.core.Response;

                  class ItemClient {
                      private final WebTarget items;

                      ItemClient(Client client, String url) {
                          items = client.target(url).path("items");
                      }

                      String find(String id) {
                          try {
                              return items.path(id).request(MediaType.APPLICATION_JSON).get(String.class);
                          } catch (WebApplicationException e) {
                              return null;
                          }
                      }

                      int create(String json) {
                          Response response = items.request().post(Entity.entity(json, MediaType.APPLICATION_JSON), Response.class);
                          if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                              throw new IllegalStateException(response.readEntity(String.class));
                          }
                          return response.getStatus();
                      }
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void leaveRepositoryWhereAnotherLibraryBringsJersey1() {
        rewriteRun(
          mavenProject("service",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>service</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>io.dropwizard</groupId>
                            <artifactId>dropwizard-core</artifactId>
                            <version>0.7.1</version>
                        </dependency>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-client</artifactId>
                            <version>1.18.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            srcMainJava(
              //language=java
              java(
                """
                  import com.sun.jersey.api.client.Client;

                  class Caller {
                      String call(Client client) {
                          return client.resource("http://localhost").get(String.class);
                      }
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void leaveRepositoryThatExtendsJersey1() {
        rewriteRun(
          mavenProject("instrumentation",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>instrumentation</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-client</artifactId>
                            <version>1.19.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            srcMainJava(
              //language=java
              java(
                """
                  package com.sun.jersey.api.client;

                  class TracingHandler {
                      String call(Client client) {
                          return client.resource("http://localhost").get(String.class);
                      }
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void leaveRepositoryWithScalaClients() {
        rewriteRun(
          mavenProject("scala-client",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>scala-client</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-client</artifactId>
                            <version>1.19.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            text(
              """
                import com.sun.jersey.api.client.Client

                object Clients {
                  val client = Client.create()
                }
                """,
              spec -> spec.path("src/main/scala/Clients.scala")
            )
          )
        );
    }

    @Test
    void leaveContainerProvidedJersey1() {
        rewriteRun(
          mavenProject("webapp",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>webapp</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.sun.jersey</groupId>
                            <artifactId>jersey-client</artifactId>
                            <version>1.19.4</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
