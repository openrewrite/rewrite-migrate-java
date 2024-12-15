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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpdateApacheWSSecurityPackagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "wss4j-1.6.19", "wss4j-ws-security-common-2.0.0"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.UpdateApacheWSSecurityPackages"));
    }

    @DocumentExample
    @Test
    void updateApacheWSSecurityPackages() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.ws.security.WSSecurityException;
              import org.apache.ws.security.components.crypto.CryptoBase;
              class Test {
                  void foo() {
                      WSSecurityException x = new WSSecurityException(1,"");
                      CryptoBase base = null;
                  }
              }
              """,
            """
              import org.apache.wss4j.common.ext.WSSecurityException;
              import org.apache.wss4j.common.ext.components.crypto.CryptoBase;
              class Test {
                  void foo() {
                      WSSecurityException x = new WSSecurityException(1,"");
                      CryptoBase base = null;
                  }
              }
              """
          )
        );
    }
}
