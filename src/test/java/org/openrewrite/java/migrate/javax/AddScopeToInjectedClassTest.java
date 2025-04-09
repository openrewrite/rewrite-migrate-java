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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddScopeToInjectedClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddScopeToInjectedClass());
        spec.parser(JavaParser.fromJavaVersion()
          .dependsOn(
            //language=java
            """
              package javax.enterprise.context;
              @Target({ElementType.Type})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Dependent {
              }
              """,
            //language=java
            """
              package javax.inject;
              @Target({ElementType.Type})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface Inject {
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void scopeRequired() {
        //language=java
        rewriteRun(
          java(
            """
              package com.sample.service;
              
              public class Bar {}
              """,
            """
              package com.sample.service;
              
              import javax.enterprise.context.Dependent;
              
              @Dependent
              public class Bar {}
              """
          ),
          java(
            """
              package com.sample;
              
              import javax.inject.Inject;
              import com.sample.service.Bar;
              
              public class Foo {
              
                  @Inject
                  Bar service;
              }
              """
          )
        );
    }

    @Test
    void noMemberVariableAnnotation() {
        //language=java
        rewriteRun(
          java(
            """
              package com.sample.service;
              
              public class Bar {}
              """
          ),
          java(
            """
              package com.sample;
              
              import com.sample.service.Bar;
              
              public class Foo{
              
                  Bar service;
              }
              """
          )
        );
    }

    @Test
    void nonInjectAnnotation() {
        //language=java
        rewriteRun(
          java(
            """
              package com.sample.service;
              
              public class Bar {}
              """
          ),
          java(
            """
              package com.sample;
              
              import com.sample.service.Bar;
              import javax.inject.NotInject;
              
              public class Foo{
                  @NotInject
                  Bar service;
              }
              """
          ),
          java(
            """
              package javax.inject;
              
              import java.lang.annotation.*;
              
              @Target({ElementType.Type})
              @Retention(RetentionPolicy.RUNTIME)
              public @interface NotInject {
              }
              """
          )
        );
    }


    @Test
    void scopeAnnotationAlreadyExists() {
        //language=java
        rewriteRun(
          java(
            """
              package com.sample.service;
              
              import javax.enterprise.context.Dependent;
              
              @Dependent
              public class Bar {}
              """
          ),
          java(
            """
              package com.sample;
              
              import com.sample.service.Bar;
              import javax.inject.Inject;
              
              public class Foo {
              
                  @javax.inject.Inject
                  Bar service;
              }
              """
          )
        );
    }
}
