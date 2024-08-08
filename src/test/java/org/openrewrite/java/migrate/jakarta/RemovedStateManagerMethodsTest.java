/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class RemovedStateManagerMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.faces-2.3.19", "jakarta.faces-3.0.3", "jakarta.faces-api-4.0.1"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedStateManagerMethods"));
    }

    @DocumentExample
    @Test
    void stateManagerReplacement() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.faces.application.StateManager;
              import jakarta.faces.component.UIViewRoot;
              import jakarta.faces.context.FacesContext;

              class StateManagerParent extends StateManager {

                  @Override
                  public UIViewRoot restoreView(FacesContext arg0, String arg1, String arg2) {
                      UIViewRoot uv = null;
                      super.getComponentStateToSave(arg0);
                      super.getTreeStructureToSave(arg0);
                      super.restoreComponentState(arg0, uv, arg2);
                      super.restoreTreeStructure(arg0, arg1, arg2);
                      return null;
                  }
              }
              """,
            """
              import jakarta.faces.component.UIViewRoot;
              import jakarta.faces.context.FacesContext;
              import jakarta.faces.view.StateManagementStrategy;

              class StateManagerParent extends StateManagementStrategy {

                  @Override
                  public UIViewRoot restoreView(FacesContext arg0, String arg1, String arg2) {
                      UIViewRoot uv = null;
                      super.saveView(arg0);
                      super.saveView(arg0);
                      super.restoreView(arg0, uv, arg2);
                      super.restoreView(arg0, arg1, arg2);
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void stateManagerRemove() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.faces.application.StateManager;
              import jakarta.faces.context.FacesContext;
              import java.io.IOException;

               class StateMgrTest {

                   public void test() throws IOException {

                       StateManager st = null;
                       FacesContext fc = null;
                       String var1 = null;
                       String var2 = null;
                       st.restoreView(fc,var1,var2);
                       st.saveSerializedView(fc);
                       st.saveView(fc);
                       StateManager.SerializedView sv = null;
                       st.writeState(fc,sv);
                   }
               }
              """,
            """
              import jakarta.faces.context.FacesContext;
              import jakarta.faces.view.StateManagementStrategy;

              import java.io.IOException;

              class StateMgrTest {

                   public void test() throws IOException {

                       StateManagementStrategy st = null;
                       FacesContext fc = null;
                       String var1 = null;
                       String var2 = null;
                       st.restoreView(fc,var1,var2);
                       st.saveView(fc);
                       st.saveView(fc);
                       StateManagementStrategy.SerializedView sv = null;
                       st.writeState(fc,sv);
                   }
               }
              """
          )
        );
    }

}
