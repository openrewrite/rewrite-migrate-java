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

class RemovedJakartaFacesResourceResolverTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().
            classpathFromResources(new InMemoryExecutionContext(), "jakarta.faces-api-4.0.0-M6", "jakarta.faces-2.3.19", "jakarta.faces-3.0.3")).
          recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedJakartaFacesResourceResolver"));
    }

    @DocumentExample
    @Test
    void removedJakartaFacesResourceResolver_1() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import java.net.URL;

              import jakarta.faces.application.StateManager;
              import jakarta.faces.component.UIViewRoot;
              import jakarta.faces.context.FacesContext;
              import jakarta.faces.view.facelets.ResourceResolver;

              public class ResourceResolverParent extends ResourceResolver {

                  @Override
                  public URL resolveUrl(String arg0) {
                      // TODO Auto-generated method stub
                      return null;
                  }
              }
              """,
                """
              package com.test;
              import java.net.URL;

              import jakarta.faces.application.ResourceHandler;
              import jakarta.faces.application.StateManager;
              import jakarta.faces.component.UIViewRoot;
              import jakarta.faces.context.FacesContext;

              public class ResourceResolverParent extends ResourceHandler {

                  @Override
                  public URL resolveUrl(String arg0) {
                      // TODO Auto-generated method stub
                      return null;
                  }
              }
              """
          ));
    }

    void removedJavaxFacesResourceResolver_1() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import java.net.URL;

              import javax.faces.application.StateManager;
              import javax.faces.component.UIViewRoot;
              import javax.faces.context.FacesContext;
              import javax.faces.view.facelets.ResourceResolver;

              public class ResourceResolverParent extends ResourceResolver {

                  @Override
                  public URL resolveUrl(String arg0) {
                      // TODO Auto-generated method stub
                      return null;
                  }
              }
              """,
                """
              package com.test;
              import java.net.URL;

              import jakarta.faces.application.ResourceHandler;
              import jakarta.faces.application.StateManager;
              import jakarta.faces.component.UIViewRoot;
              import jakarta.faces.context.FacesContext;

              public class ResourceResolverParent extends ResourceHandler {

                  @Override
                  public URL resolveUrl(String arg0) {
                      // TODO Auto-generated method stub
                      return null;
                  }
              }
              """
          ));
    }

}
