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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpdateAnnotationAttributeJavaxToJakartaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaEE10")
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
                package javax.jms;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Repeatable;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                import javax.annotation.sql.DataSourceDefinition;

                /**
                 * Used to define a JMS destination resource that will be created
                 * and made available for JNDI lookup at runtime.
                 *
                 * @since JMS 2.0
                 */
                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.RUNTIME)
                @Repeatable(JMSDestinationDefinitions.class)
                public @interface JMSDestinationDefinition {

                    /**
                     * The name of the JNDI location where the destination will be bound.
                     */
                    String name();

                    /**
                     * The type of destination, either javax.jms.Queue or javax.jms.Topic.
                     */
                    String interfaceName();

                    /**
                     * The class name of the implementation for the destination.
                     */
                    String className() default "";

                    /**
                     * The name of the destination.
                     */
                    String destinationName() default "";

                    /**
                     * Specifies whether the destination is durable.
                     */
                    boolean durable() default false;

                    /**
                     * Description of this destination.
                     */
                    String description() default "";
                }
                """,
              """
                package javax.validation.constraints;

                import java.lang.annotation.*;
                import static java.lang.annotation.ElementType.*;
                import static java.lang.annotation.RetentionPolicy.RUNTIME;

                @Target(value={METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER})
                @Retention(value=RUNTIME)
                public @interface NotNull {
                    String message() default "{javax.validation.constraints.NotNull.message}";
                }
                """
            )
          );
    }

    @Test
    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/688")
    void replaceInterfaceName() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.jms.*;

              @JMSDestinationDefinition(name = "Testing",
                          interfaceName = "javax.jms.Topic",
                          destinationName = "Testing")
              class Test {
              }
              """,
            """
              import jakarta.jms.*;

              @JMSDestinationDefinition(name = "Testing",
                          interfaceName = "jakarta.jms.Topic",
                          destinationName = "Testing")
              class Test {
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/pull/374")
    void replaceMessage() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.validation.constraints.*;

              class Test {
                 @NotNull(message = "Resource Code {javax.validation.constraints.NotNull.message}")
                 private String resourceCode;
              }
              """,
            """
              import jakarta.validation.constraints.*;

              class Test {
                 @NotNull(message = "Resource Code {jakarta.validation.constraints.NotNull.message}")
                 private String resourceCode;
              }
              """
          )
        );
    }

    @Test
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
              import jakarta.ejb.*;

              @ActivationConfigProperty(propertyName="destinationType", propertyValue="jakarta.jms.Queue")
              class Test {
              }
              """
          )
        );
    }
}
