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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;


import static org.openrewrite.java.Assertions.java;

class Java8ToJava11Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal.new"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate").build()
            .activateRecipes("org.openrewrite.java.migrate.Java8toJava11"));
    }

    //language=java
    String Krb5LoginModuleClass = """
      
       package com.ibm.security.auth.module;

       public class Krb5LoginModule {

        public void login() {
        }

       }
      """;

    @DocumentExample
    @Test
    void internalBindContextFactory() {
        //language=java
        rewriteRun(
          java(
            """
              class Foo {
                  void bar() {
                      com.sun.xml.internal.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      com.sun.xml.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
              """
          ),
          java(
            """
              import com.sun.xml.internal.bind.v2.ContextFactory;

              class Foo2 {
                void bar() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
              }
              """,
            """
              import com.sun.xml.bind.v2.ContextFactory;

              class Foo2 {
                void bar() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
              }
              """
          ),
          java(
            """
              import com.sun.xml.internal.bind.v2.*;

              class Foo3 {
                void bar() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }

              }
              """,
            """
              import com.sun.xml.bind.v2.ContextFactory;
              import com.sun.xml.internal.bind.v2.*;

              class Foo3 {
                void bar() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }

              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void Krb5LoginModuleTest() {
        //language=java
        rewriteRun(
          java(Krb5LoginModuleClass),
          java(
            """
              package com.ibm.test;
              
              import com.ibm.security.auth.module.Krb5LoginModule;
              
              public class TestClass {
              
                public void testClass() {
                   Krb5LoginModule krb = new Krb5LoginModule();
                   krb.login();
                }   
              }
            """,
            """
                    package com.ibm.test;

                    import com.sun.security.auth.module.Krb5LoginModule;

                    public class TestClass {

                      public void testClass() {
                         Krb5LoginModule krb = new Krb5LoginModule();
                         krb.login();
                      }
                    }
                    """
          )
        );
    }
}
