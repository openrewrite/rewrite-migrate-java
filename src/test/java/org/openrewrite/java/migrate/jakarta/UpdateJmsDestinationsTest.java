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

class UpdateJmsDestinationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateJmsDestinations())
          .parser(JavaParser.fromJavaVersion()
            //language=java
            .dependsOn(
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
              package jakarta.jms;
              
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
                   * The type of destination, either jakarta.jms.Queue or jakarta.jms.Topic.
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
              import javax.jms.*;

              @JMSDestinationDefinition(name = "Testing",
                          interfaceName = "jakarta.jms.Topic",
                          destinationName = "Testing")
              class Test {
              }
              """
          )
        );
    }
}
