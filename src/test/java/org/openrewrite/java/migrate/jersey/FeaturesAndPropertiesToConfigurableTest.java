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

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class FeaturesAndPropertiesToConfigurableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FeaturesAndPropertiesToConfigurable())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "jersey-server-1", "jersey-json-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void clientConfig() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.api.json.JSONConfiguration;

              class Test {
                  Client client() {
                      ClientConfig config = new DefaultClientConfig();
                      config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
                      config.getClasses().add(Provider.class);
                      config.getSingletons().add(new Provider());
                      config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 1000);
                      return Client.create(config);
                  }

                  static class Provider {
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

              class Test {
                  Client client() {
                      ClientConfig config = new DefaultClientConfig();
                      config.register(JacksonJaxbJsonProvider.class);
                      config.register(Provider.class);
                      config.register(new Provider());
                      config.property(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 1000);
                      return Client.create(config);
                  }

                  static class Provider {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.client.ClientConfig register(..)")).hasSize(3);
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.client.ClientConfig property(String, Object)")).hasSize(1);
            })
          )
        );
    }

    @Test
    void implicitReceiverInResourceConfigSubclass() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class AppConfig extends DefaultResourceConfig {
                  AppConfig() {
                      getClasses().add(HelloResource.class);
                      getSingletons().add(new HelloResource());
                      getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, true);
                      getProperties().put("custom", "value");
                      this.getClasses().add(HelloResource.class);
                  }

                  static class HelloResource {
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class AppConfig extends DefaultResourceConfig {
                  AppConfig() {
                      register(HelloResource.class);
                      register(new HelloResource());
                      property(ResourceConfig.FEATURE_DISABLE_WADL, true);
                      property("custom", "value");
                      this.register(HelloResource.class);
                  }

                  static class HelloResource {
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.server.ResourceConfig register(Class)")).hasSize(2);
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.server.ResourceConfig register(Object)")).hasSize(1);
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.server.ResourceConfig property(String, Object)")).hasSize(2);
            })
          )
        );
    }

    @Test
    void anonymousResourceConfig() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class Test {
                  ResourceConfig config() {
                      return new DefaultResourceConfig() {{
                          getClasses().add(Test.class);
                      }};
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class Test {
                  ResourceConfig config() {
                      return new DefaultResourceConfig() {{
                          register(Test.class);
                      }};
                  }
              }
              """
          )
        );
    }

    @Test
    void addAllAndPutAllOnResourceConfig() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;
              import java.util.List;
              import java.util.Map;
              import java.util.Set;

              class Test {
                  void configure(ResourceConfig rc, Set<Class<?>> classes, Set<Object> singletons,
                                 Map<String, Object> properties, List<Class<?>> classList) {
                      rc.getClasses().addAll(classes);
                      rc.getSingletons().addAll(singletons);
                      rc.getProperties().putAll(properties);
                      rc.getClasses().addAll(classList);
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;
              import java.util.List;
              import java.util.Map;
              import java.util.Set;

              class Test {
                  void configure(ResourceConfig rc, Set<Class<?>> classes, Set<Object> singletons,
                                 Map<String, Object> properties, List<Class<?>> classList) {
                      rc.registerClasses(classes);
                      rc.registerInstances(singletons);
                      rc.addProperties(properties);
                      rc.getClasses().addAll(classList);
                  }
              }
              """
          )
        );
    }

    @Test
    void clientProperties() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;

              class Test {
                  void configure(Client client) {
                      client.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 5000);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;

              class Test {
                  void configure(Client client) {
                      client.property(ClientConfig.PROPERTY_READ_TIMEOUT, 5000);
                  }
              }
              """
          )
        );
    }

    @Test
    void pojoMappingSpelledAsStringsAndStaticImport() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;

              import static com.sun.jersey.api.json.JSONConfiguration.FEATURE_POJO_MAPPING;

              class Test {
                  void configure(DefaultResourceConfig rc, DefaultResourceConfig other) {
                      rc.getFeatures().put(FEATURE_POJO_MAPPING, true);
                      other.getProperties().put("com.sun.jersey.api.json.POJOMappingFeature", "true");
                  }
              }
              """,
            """
              import com.sun.jersey.api.core.DefaultResourceConfig;
              import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

              class Test {
                  void configure(DefaultResourceConfig rc, DefaultResourceConfig other) {
                      rc.register(JacksonJaxbJsonProvider.class);
                      other.register(JacksonJaxbJsonProvider.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void pojoMappingSwitchedOffIsDropped() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.api.json.JSONConfiguration;

              class Test {
                  void configure(DefaultClientConfig config) {
                      config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.FALSE);
                      config.getClasses().add(Test.class);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              class Test {
                  void configure(DefaultClientConfig config) {
                      config.register(Test.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesUsedResultsVariablesAndOtherCollectionsAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.api.json.JSONConfiguration;
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  boolean configure(ClientConfig config, boolean pojo, Set<Class<?>> classes) {
                      config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, pojo);
                      config.getClasses().addAll(classes);
                      config.getClasses().remove(Test.class);
                      Set<Class<?>> mine = new HashSet<>();
                      mine.add(Test.class);
                      return config.getClasses().add(Test.class);
                  }
              }
              """
          )
        );
    }
}
