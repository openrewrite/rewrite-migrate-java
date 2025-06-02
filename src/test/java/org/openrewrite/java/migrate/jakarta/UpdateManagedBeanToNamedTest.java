package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UpdateManagedBeanToNamedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jsf-api-2.1.29-11", "jakarta.faces-api-3.0.0", "jakarta.inject-api-2.0.1"))
            .recipe(new UpdateManagedBeanToNamed());
    }

    @DocumentExample
    @ParameterizedTest
    @ValueSource(strings = {"javax", "jakarta"})
    void updateManagedBeanToNamed(String pkg) {
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

    @ParameterizedTest
    @ValueSource(strings = {"javax", "jakarta"})
    void updateManagedBeanToNamedWithArg(String pkg) {
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
