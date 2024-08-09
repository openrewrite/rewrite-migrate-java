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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FacesManagedBeansRemovedTest implements RewriteTest {

    @DocumentExample
    @Test
    void updateFacesManagedBeanFromEE8() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
              .classpathFromResources(new InMemoryExecutionContext(), "cdi-api-2.0.SP1", "jsf-api-2.1.29-11", "jakarta.enterprise.cdi-api-4.0.1"))
            .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.jakarta.FacesManagedBeansRemoved"))
          ,
          //language=java
          java(
            """
              import javax.faces.bean.ApplicationScoped;
              import javax.faces.bean.RequestScoped;
              import javax.faces.bean.SessionScoped;
              import javax.faces.bean.ManagedProperty;
              import javax.faces.bean.NoneScoped;
              import javax.faces.bean.ViewScoped;

              @ApplicationScoped
              @RequestScoped
              @SessionScoped
              @ManagedProperty
              @NoneScoped
              @ViewScoped
              public class ApplicationBean2 {
              }
              """,
            """
              import jakarta.enterprise.context.ApplicationScoped;
              import jakarta.enterprise.context.Dependent;
              import jakarta.enterprise.context.RequestScoped;
              import jakarta.enterprise.context.SessionScoped;
              import jakarta.faces.annotation.ManagedProperty;
              import jakarta.faces.view.ViewScoped;

              @ApplicationScoped
              @RequestScoped
              @SessionScoped
              @ManagedProperty
              @Dependent
              @ViewScoped
              public class ApplicationBean2 {
              }
              """
          )
        );
    }

    @Test
    void updateFacesManagedBeanFromEE9() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
              .classpathFromResources(new InMemoryExecutionContext(), "cdi-api-2.0.SP1", "jakarta.faces-api-3.0.0", "jakarta.enterprise.cdi-api-4.0.1"))
            .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.jakarta.FacesManagedBeansRemoved"))
          ,
          //language=java
          java(
            """
              import jakarta.faces.bean.ApplicationScoped;
              import jakarta.faces.bean.RequestScoped;
              import jakarta.faces.bean.SessionScoped;
              import jakarta.faces.bean.ManagedProperty;
              import jakarta.faces.bean.NoneScoped;
              import jakarta.faces.bean.ViewScoped;

              @ApplicationScoped
              @RequestScoped
              @SessionScoped
              @ManagedProperty
              @NoneScoped
              @ViewScoped
              public class ApplicationBean2 {
              }
              """,
            """
              import jakarta.enterprise.context.ApplicationScoped;
              import jakarta.enterprise.context.Dependent;
              import jakarta.enterprise.context.RequestScoped;
              import jakarta.enterprise.context.SessionScoped;
              import jakarta.faces.annotation.ManagedProperty;
              import jakarta.faces.view.ViewScoped;

              @ApplicationScoped
              @RequestScoped
              @SessionScoped
              @ManagedProperty
              @Dependent
              @ViewScoped
              public class ApplicationBean2 {
              }
              """
          )
        );
    }

}
