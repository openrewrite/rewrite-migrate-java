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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.openrewrite.java.Assertions.java;

class InternalBindPackagesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpath(singletonList(resourceJar("sun.internal.newClass.jar"))))
          .recipeFromResources("org.openrewrite.java.migrate.InternalBindPackages");
    }

    private static Path resourceJar(String name) {
        URL url = InternalBindPackagesTest.class.getClassLoader().getResource("META-INF/rewrite/classpath/" + name);
        try {
            return Paths.get(Objects.requireNonNull(url, "Resource not found: " + name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @DocumentExample
    @Test
    void contextFactoryImportVariants() {
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
              import com.sun.xml.bind.v2.*;

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

    @Test
    void wellKnownNamespace() {
        rewriteRun(
          //language=java
          java(
            """
              package com.ibm.test;
              public class TestInternalBindPackages {
                  public void testInternalBindPackages() {
                      com.sun.xml.internal.bind.v2.WellKnownNamespace namespace = null;
                      namespace.hashCode();
                  }
              }
              """,
            """
              package com.ibm.test;
              public class TestInternalBindPackages {
                  public void testInternalBindPackages() {
                      com.sun.xml.bind.v2.WellKnownNamespace namespace = null;
                      namespace.hashCode();
                  }
              }
              """
          )
        );
    }
}
