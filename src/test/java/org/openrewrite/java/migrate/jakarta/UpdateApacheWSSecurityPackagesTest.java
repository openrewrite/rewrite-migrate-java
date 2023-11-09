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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UpdateApacheWSSecurityPackagesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().
            classpathFromResources(new InMemoryExecutionContext(), "wss4j-1.6.19", "wss4j-ws-security-common-2.0.0")).
          recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.UpdateApacheWSSecurityPackages"));
    }
    @Test
    public void updateApache(){
        rewriteRun(
          //language=java
          java("""
            package com.test;

            import org.apache.ws.security.WSSecurityException;
            import org.apache.ws.security.components.crypto.CryptoBase;
            public class Test {
                public static void main(String[] args)  {
            
                    WSSecurityException x = new WSSecurityException(1,"");
            
                    CryptoBase base = null;
            
                }
            }
            """, """
            package com.test;
            
            import org.apache.wss4j.common.ext.WSSecurityException;
            import org.apache.wss4j.common.ext.components.crypto.CryptoBase;
            public class Test {
                public static void main(String[] args)  {
            
                    WSSecurityException x = new WSSecurityException(1,"");
            
                    CryptoBase base = null;
            
                }
            }
            """));
    }
}
