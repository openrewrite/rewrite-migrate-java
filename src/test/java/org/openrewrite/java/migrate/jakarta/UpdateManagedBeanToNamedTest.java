/*
 * Copyright 2025 the original author or authors.
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
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@ParameterizedClass
@ValueSource(strings = {"javax", "jakarta"})
record UpdateManagedBeanToNamedTest(String pkg) implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(),
              "jsf-api-2.1.29-11",
              "jakarta.faces-api-3.0.0"))
          .recipe(new UpdateManagedBeanToNamed());
    }

    @Test
    void updateManagedBeanToNamed() {
        rewriteRun(
          //language=java
          java(
            """
              import %s.faces.bean.ManagedBean;

              @ManagedBean
              public class ApplicationBean2 {
              }
              """.formatted(pkg),
            """
              import jakarta.inject.Named;

              @Named
              public class ApplicationBean2 {
              }
              """
          )
        );
    }

    @Test
    void updateManagedBeanToNamedWithArg() {
        rewriteRun(
          //language=java
          java(
            """
              import %s.faces.bean.ManagedBean;

              @ManagedBean(name="myBean")
              public class ApplicationBean2 {
              }
              """.formatted(pkg),
            """
              import jakarta.inject.Named;

              @Named("myBean")
              public class ApplicationBean2 {
              }
              """
          )
        );
    }
}
