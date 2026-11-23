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
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class PackagesResourceConfigToResourceConfigTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PackagesResourceConfigToResourceConfig())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-core-1", "jersey-server-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void constructor() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;

              class Test {
                  Object config(String[] more) {
                      Object other = new PackagesResourceConfig(more);
                      return new PackagesResourceConfig("com.example.api", "com.example.providers");
                  }
              }
              """,
            """
              import org.glassfish.jersey.server.ResourceConfig;

              class Test {
                  Object config(String[] more) {
                      Object other = new ResourceConfig().packages(more);
                      return new ResourceConfig().packages("com.example.api", "com.example.providers");
                  }
              }
              """,
            spec -> spec.afterRecipe(cu ->
              assertThat(FindMethods.find(cu, "org.glassfish.jersey.server.ResourceConfig packages(String[])")).hasSize(2))
          )
        );
    }

    @Test
    void togetherWithTheRenameOfJersey1ResourceConfig() {
        rewriteRun(
          spec -> spec.recipes(
            new PackagesResourceConfigToResourceConfig(),
            new ChangeType("com.sun.jersey.api.core.ResourceConfig", "org.glassfish.jersey.server.ResourceConfig", null)
          ),
          java(
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class Test {
                  ResourceConfig config() {
                      ResourceConfig rc = new PackagesResourceConfig("com.example.api");
                      rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, true);
                      return rc;
                  }
              }
              """,
            """
              import org.glassfish.jersey.server.ResourceConfig;

              class Test {
                  ResourceConfig config() {
                      ResourceConfig rc = new ResourceConfig().packages("com.example.api");
                      rc.getFeatures().put(ResourceConfig.FEATURE_DISABLE_WADL, true);
                      return rc;
                  }
              }
              """
          )
        );
    }

    @Test
    void subclass() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;

              public class AppConfig extends PackagesResourceConfig {
                  public AppConfig() {
                      super("com.example.api", "com.example.providers");
                      getProperties().put("custom", "value");
                  }

                  public AppConfig(String extra) {
                      this();
                  }
              }
              """,
            """
              import org.glassfish.jersey.server.ResourceConfig;

              public class AppConfig extends ResourceConfig {
                  public AppConfig() {
                      packages("com.example.api", "com.example.providers");
                      getProperties().put("custom", "value");
                  }

                  public AppConfig(String extra) {
                      this();
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(FindMethods.find(cu, "org.glassfish.jersey.server.ResourceConfig packages(String[])")).hasSize(1);
                J.ClassDeclaration appConfig = cu.getClasses().get(0);
                assertThat(TypeUtils.isOfClassType(appConfig.getExtends().getType(), "org.glassfish.jersey.server.ResourceConfig")).isTrue();
                assertThat(TypeUtils.isOfClassType(appConfig.getType().getSupertype(), "org.glassfish.jersey.server.ResourceConfig")).isTrue();
            })
          )
        );
    }

    @Test
    void leavesMapConstructorAnonymousAndImplicitConstructorsAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.core.PackagesResourceConfig;
              import java.util.Map;

              class Test {
                  Object fromMap(Map<String, Object> properties) {
                      return new PackagesResourceConfig(properties);
                  }

                  Object anonymous() {
                      return new PackagesResourceConfig("com.example") {
                      };
                  }
              }

              class FromMap extends PackagesResourceConfig {
                  FromMap(Map<String, Object> properties) {
                      super(properties);
                  }
              }

              class Mixed extends PackagesResourceConfig {
                  Mixed() {
                      super("com.example");
                  }

                  Mixed(Map<String, Object> properties) {
                      super(properties);
                  }
              }

              class CallsSuperMethods extends PackagesResourceConfig {
                  CallsSuperMethods() {
                      super("com.example");
                      super.getFeatures().put("feature", true);
                  }
              }
              """
          )
        );
    }
}
