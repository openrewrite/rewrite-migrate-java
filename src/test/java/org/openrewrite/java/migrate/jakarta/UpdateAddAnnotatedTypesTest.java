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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpdateAddAnnotatedTypesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.enterprise.cdi-api-3.0.0-M4"))
          .recipe(new UpdateAddAnnotatedTypes());
    }

    @DocumentExample
    @Test
    void addAnnotatedType() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;

              class Foo {
                void bar(BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
                  AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                  beforeBeanDiscovery.addAnnotatedType(producerType); // Flag this one
                  beforeBeanDiscovery.addAnnotatedType(producerType, "my unique id"); // Not this one
                  beforeBeanDiscovery.addAnnotatedType(String.class, "my other unique id"); // Not this one
                }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.AnnotatedType;
              import jakarta.enterprise.inject.spi.BeanManager;
              import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;

              class Foo {
                void bar(BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
                  AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
                  beforeBeanDiscovery.addAnnotatedType(producerType, null); // Flag this one
                  beforeBeanDiscovery.addAnnotatedType(producerType, "my unique id"); // Not this one
                  beforeBeanDiscovery.addAnnotatedType(String.class, "my other unique id"); // Not this one
                }
              }
              """
          )
        );
    }

}
