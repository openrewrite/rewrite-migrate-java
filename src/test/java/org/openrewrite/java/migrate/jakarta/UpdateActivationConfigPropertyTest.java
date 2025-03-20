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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class UpdateActivationConfigPropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateActivationConfigProperty())
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
              """
              package javax.ejb;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              /**
               * Specifies a name/value pair for a configuration property that is passed to
               * the endpoint deployment.
               * 
               * @since EJB 3.0
               */
              @Target({ ElementType.METHOD, ElementType.TYPE })
              @Retention(RetentionPolicy.RUNTIME)
              public @interface ActivationConfigProperty {
                  /**
                   * Name of the configuration property.
                   */
                  String propertyName();

                  /**
                   * Value of the configuration property.
                   */
                  String propertyValue();
              }
              """,
              """
              package jakarta.ejb;

              import java.lang.annotation.ElementType;
              import java.lang.annotation.Retention;
              import java.lang.annotation.RetentionPolicy;
              import java.lang.annotation.Target;

              /**
               * Specifies a name/value pair for a configuration property that is passed to
               * the endpoint deployment.
               * 
               * @since EJB 3.0
               */
              @Target({ ElementType.METHOD, ElementType.TYPE })
              @Retention(RetentionPolicy.RUNTIME)
              public @interface ActivationConfigProperty {
                  /**
                   * Name of the configuration property.
                   */
                  String propertyName();

                  /**
                   * Value of the configuration property.
                   */
                  String propertyValue();
              }
                """
            )
          );
    }

    @Test
    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/688")
    void replacePropertyValue() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.ejb.*;

              @ActivationConfigProperty(propertyName="destinationType", propertyValue="javax.jms.Queue")
              class Test {
              }
              """,
              """
              import javax.ejb.*;

              @ActivationConfigProperty(propertyName="destinationType", propertyValue="jakarta.jms.Queue")
              class Test {
              }
              """
          )
        );
    }
}
