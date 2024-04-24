/*
 * Copyright 2024 the original author or authors.
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
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true") // Unexplained failure only on GitHub Actions
class UpdateBeanManagerMethodsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.enterprise.cdi-api-3.0.0-M4", "jakarta.enterprise.cdi-api-4.0.1"))
          .recipe(new UpdateBeanManagerMethods());
    }

    @DocumentExample
    @Test
    void fireEvent() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;
              
              class Foo {
                  void bar(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery) {
                      beanManager.fireEvent(beforeBeanDiscovery);
                  }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
              import java.util.Set;
              
              class Foo {
                  void bar(BeanManager beanManager, BeforeBeanDiscovery beforeBeanDiscovery) {
                      beanManager.getEvent().fire(beforeBeanDiscovery);
                  }
              }
              """
          )
        );
    }

    @Test
    void createInjectionTarget() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;
              
              class Foo {
                  void bar(BeanManager beanManager) {
                      AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                      beanManager.createInjectionTarget(producerType);
                  }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;
              
              class Foo {
                  void bar(BeanManager beanManager) {
                      AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                      beanManager.getInjectionTargetFactory(producerType).createInjectionTarget(null);
                  }
              }
              """
          )
        );
    }
}
