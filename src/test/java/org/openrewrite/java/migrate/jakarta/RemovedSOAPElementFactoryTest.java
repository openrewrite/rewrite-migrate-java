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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class RemovedSOAPElementFactoryTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.xml.soap-api-2.0.1"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedSOAPElementFactory"));
    }

    @DocumentExample
    @Test
    void removeSOAPElementFactory() {
        rewriteRun(
          //language=java
          java(
                """             
            package com.test;
                        
            import jakarta.xml.soap.Name;
            import jakarta.xml.soap.SOAPElementFactory;
            import jakarta.xml.soap.SOAPEnvelope;
                        
            public class Test {
                void test(SOAPEnvelope envelope) {
                    String str1 = "test";
                    String str2 = "t2";
                    String str3 = "t3";
                    Name n = envelope.createName("GetLastTradePrice", "WOMBAT", "http://www.abc.org/trader");
                    SOAPElementFactory sfe = SOAPElementFactory.newInstance();
                    sfe.create(str1);
                    sfe.create(str1, str2, str3);
                    sfe.create(n);
                }
            }
            """, """
            package com.test;
             
            import jakarta.xml.soap.Name;
            import jakarta.xml.soap.SOAPEnvelope;
            import jakarta.xml.soap.SOAPFactory;

            public class Test {
                void test(SOAPEnvelope envelope) {
                    String str1 = "test";
                    String str2 = "t2";
                    String str3 = "t3";
                    Name n = envelope.createName("GetLastTradePrice", "WOMBAT", "http://www.abc.org/trader");
                    SOAPFactory sfe = SOAPFactory.newInstance();
                    sfe.createElement(str1);
                    sfe.createElement(str1, str2, str3);
                    sfe.createElement(n);
                }
            }
            """));
    }


}
