/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class InternalBindPackages implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal.newClass"))
          .recipeFromResources("org.openrewrite.java.migrate.InternalBindPackages");
    }

    @Test
    @DocumentExample
    void internalBindPackages() {
        rewriteRun(
          //language=java
          java("""
              package com.ibm.test;
              public class TestInternalBindPackages {
                  public void testInternalBindPackages() {
                      com.sun.xml.internal.bind.v2.WellKnownNamespace namespace = null;
                namespace.hashCode();
                      com.sun.xml.internal.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
                  """,
            """
              package com.ibm.test;
              public class TestInternalBindPackages {
                  public void testInternalBindPackages() {
                      com.sun.xml.bind.v2.WellKnownNamespace namespace = null;
                namespace.hashCode();
                      com.sun.xml.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
              """
          )
        );
    }
}
