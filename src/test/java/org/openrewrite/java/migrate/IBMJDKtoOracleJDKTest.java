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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class IBMJDKtoOracleJDKTest implements RewriteTest {
    @DocumentExample
    @Test
    void Krb5LoginModuleTest() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("""
              package com.ibm.security.auth.module;
              public class Krb5LoginModule {
                  public void login() {
                  }
              }
              """))
            .recipeFromResources("org.openrewrite.java.migrate.IBMJDKtoOracleJDK"),
          //language=java
          java(
            """
              import com.ibm.security.auth.module.Krb5LoginModule;

              class TestClass {
                  public void testClass() {
                      Krb5LoginModule krb = new Krb5LoginModule();
                      krb.login();
                  }
              }
              """,
            """
              import com.sun.security.auth.module.Krb5LoginModule;

              class TestClass {
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
