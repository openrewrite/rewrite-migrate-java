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

class WsWsocServerContainerDeprecationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "WsWsocServerContainer_test"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.WsWsocServerContainerDeprecation"));
    }

    @Test
    void deprecateWsWsocServerContainer() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpServletResponse;
                            
              import com.ibm.websphere.wsoc.ServerEndpointConfig;
              import com.ibm.websphere.wsoc.WsWsocServerContainer;
                            
              class Test {
                  void doX(HttpServletRequest req, HttpServletResponse res, ServerEndpointConfig sConfig, java.util.Map<String,String> map){
                      WsWsocServerContainer.doUpgrade(req, res, sConfig, map);
                  }
               }
              """,
            """
              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpServletResponse;
                            
              import com.ibm.websphere.wsoc.ServerEndpointConfig;
              import jakarta.websocket.server.ServerContainer;
                            
              class Test {
                  void doX(HttpServletRequest req, HttpServletResponse res, ServerEndpointConfig sConfig, java.util.Map<String,String> map){
                      ServerContainer.upgradeHttpToWebSocket(req, res, sConfig, map);
                  }
               }
              """
          )
        );
    }

}
