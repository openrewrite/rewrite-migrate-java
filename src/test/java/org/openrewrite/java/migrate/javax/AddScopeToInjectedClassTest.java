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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.DocumentExample;
import org.openrewrite.java.migrate.javax.AddScopeToInjectedClass;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.java.JavaParser;
import org.junit.jupiter.api.Test;

import static org.openrewrite.java.Assertions.java;

class AddScopeToInjectedClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {

        spec.recipe(new AddScopeToInjectedClass());
        spec.parser(JavaParser.fromJavaVersion().dependsOn("""
            package javax.enterprise.context;
            @Target({ElementType.Type})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Dependent {
            }
            """,
          """
            package javax.inject;
            @Target({ElementType.Type})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Inject {
            }
            """));
    }

    @DocumentExample
    @Test
    void scopeRequired() {
        rewriteRun(spec ->
          java("""
              package com.sample.service;

              public class Bar {}

              """,
            """
              package com.sample.service;

              import javax.enterprise.context.Dependent;

              @Dependent
              public class Bar {}

              """),

          java("""
            package com.sample;

            import javax.inject.Inject;
            import com.sample.service.Bar;

            public class Foo{

                @javax.inject.Inject
                Bar service;
            }
            """));
    }


    @Test
    void noMemberVariableAnnotation() {
        rewriteRun(spec ->
          java("""
            package com.sample.service;

            public class Bar {}

            """),

          java("""
            package com.sample;

            import com.sample.service.Bar;

            public class Foo{

                Bar service;
            }
            """));
    }

    @Test
    void nonInjectAnnotation() {
        rewriteRun(spec ->
          java("""
            package com.sample.service;

            public class Bar {}

            """),

          java("""
            package com.sample;

            import com.sample.service.Bar;
            import javax.inject.NotInject;

            public class Foo{
                @NotInject
                Bar service;
            }
            """),
          java("""
            package javax.inject;
            @Target({ElementType.Type})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NotInject {
            }
            """));
    }


    @Test
    void scopeAnnotationAlreadyExists() {
        rewriteRun(spec ->
          java("""
            package com.sample.service;

            import javax.enterprise.context.Dependent;

            @Dependent
            public class Bar {}

            """),

          java("""
            package com.sample;

            import javax.inject.Inject;
            import com.sample.service.Bar;

            public class Foo{

                @javax.inject.Inject
                Bar service;
            }
            """));
    }

}