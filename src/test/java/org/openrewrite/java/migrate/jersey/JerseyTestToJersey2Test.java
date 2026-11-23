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

import static org.openrewrite.java.Assertions.java;

class JerseyTestToJersey2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jersey.Jersey1TestFrameworkToJersey2")
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1.19", "jersey-core-1.19",
              "jersey-server-1.19", "jersey-test-framework-core-1.19", "jersey-test-framework-grizzly2-1.19",
              "javax.servlet-api-4"));
    }

    @DocumentExample
    @Test
    void servletDeploymentMovesToConfigureDeployment() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.test.framework.JerseyTest;
              import com.sun.jersey.test.framework.WebAppDescriptor;

              import javax.servlet.ServletContextListener;

              public class ItemsTest extends JerseyTest {

                  public ItemsTest() throws Exception {
                      super(new WebAppDescriptor.Builder("com.example.items")
                              .contextPath("app")
                              .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
                              .contextListenerClass(ServletContextListener.class)
                              .build());
                  }

                  WebResource items() {
                      return resource().path("items");
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.WebResource;
              import org.glassfish.jersey.test.DeploymentContext;
              import org.glassfish.jersey.test.JerseyTest;
              import org.glassfish.jersey.test.ServletDeploymentContext;
              import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
              import org.glassfish.jersey.test.spi.TestContainerFactory;

              import javax.servlet.ServletContextListener;

              public class ItemsTest extends JerseyTest {

                  @Override
                  protected DeploymentContext configureDeployment() {
                      return ServletDeploymentContext.forPackages("com.example.items")
                              .contextPath("app")
                              .initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
                              .addListener(ServletContextListener.class)
                              .build();
                  }

                  WebResource items() {
                      return target().path("items");
                  }

                  @Override
                  protected TestContainerFactory getTestContainerFactory() {
                      return new GrizzlyWebTestContainerFactory();
                  }
              }
              """
          )
        );
    }

    @Test
    void packagesBecomeResourceConfig() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.test.framework.JerseyTest;

              import java.net.URI;

              public class ItemsTest extends JerseyTest {
                  static final String RESOURCES = "com.example.items";

                  public ItemsTest() {
                      super(RESOURCES, "com.example.users");
                  }

                  URI base() {
                      return getBaseURI();
                  }
              }
              """,
            """
              import org.glassfish.jersey.server.ResourceConfig;
              import org.glassfish.jersey.test.JerseyTest;

              import java.net.URI;

              public class ItemsTest extends JerseyTest {
                  static final String RESOURCES = "com.example.items";

                  public ItemsTest() {
                      super(new ResourceConfig().packages(RESOURCES, "com.example.users"));
                  }

                  URI base() {
                      return getBaseUri();
                  }
              }
              """
          )
        );
    }

    @Test
    void configureReturnsDeploymentContext() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.test.framework.AppDescriptor;
              import com.sun.jersey.test.framework.JerseyTest;
              import com.sun.jersey.test.framework.LowLevelAppDescriptor;

              public class ItemsTest extends JerseyTest {

                  @Override
                  protected AppDescriptor configure() {
                      return new LowLevelAppDescriptor.Builder(ItemResource.class).contextPath("app").build();
                  }

                  public static class ItemResource {
                  }
              }
              """,
            """
              import org.glassfish.jersey.server.ResourceConfig;
              import org.glassfish.jersey.test.DeploymentContext;
              import org.glassfish.jersey.test.JerseyTest;

              public class ItemsTest extends JerseyTest {

                  @Override
                  protected DeploymentContext configureDeployment() {
                      return DeploymentContext.builder(new ResourceConfig(ItemResource.class)).contextPath("app").build();
                  }

                  public static class ItemResource {
                  }
              }
              """
          )
        );
    }

    @Test
    void descriptorFromFactoryAndOwnContainer() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.test.framework.WebAppDescriptor;

              class Descriptors {
                  static WebAppDescriptor items() {
                      return new WebAppDescriptor.Builder().contextPath("app").build();
                  }
              }
              """,
            """
              import org.glassfish.jersey.servlet.ServletContainer;
              import org.glassfish.jersey.test.ServletDeploymentContext;

              class Descriptors {
                  static ServletDeploymentContext items() {
                      return ServletDeploymentContext.forServlet(ServletContainer.class).contextPath("app").build();
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import com.sun.jersey.test.framework.JerseyTest;
              import com.sun.jersey.test.framework.spi.container.TestContainerFactory;
              import com.sun.jersey.test.framework.spi.container.grizzly2.web.GrizzlyWebTestContainerFactory;

              public class ItemsTest extends JerseyTest {

                  public ItemsTest() {
                      super(Descriptors.items());
                      System.setProperty("test", "true");
                  }

                  @Override
                  protected TestContainerFactory getTestContainerFactory() {
                      return new GrizzlyWebTestContainerFactory();
                  }
              }
              """,
            """
              import org.glassfish.jersey.test.DeploymentContext;
              import org.glassfish.jersey.test.JerseyTest;
              import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
              import org.glassfish.jersey.test.spi.TestContainerFactory;

              public class ItemsTest extends JerseyTest {

                  public ItemsTest() {
                      System.setProperty("test", "true");
                  }

                  @Override
                  protected DeploymentContext configureDeployment() {
                      return Descriptors.items();
                  }

                  @Override
                  protected TestContainerFactory getTestContainerFactory() {
                      return new GrizzlyWebTestContainerFactory();
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveDescriptorBuiltFromConstructorParameters() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.test.framework.AppDescriptor;
              import com.sun.jersey.test.framework.JerseyTest;

              public abstract class BaseTest extends JerseyTest {

                  protected BaseTest(AppDescriptor descriptor) {
                      super(descriptor);
                  }
              }
              """,
            """
              import org.glassfish.jersey.test.DeploymentContext;
              import org.glassfish.jersey.test.JerseyTest;

              public abstract class BaseTest extends JerseyTest {

                  protected BaseTest(DeploymentContext descriptor) {
                      super(descriptor);
                  }
              }
              """
          )
        );
    }
}
