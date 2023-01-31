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
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath()
          .build()
          .activateRecipes("org.openrewrite.java.migrate.UpgradeToJava17"));
    }

    @Test
    void upgradeFromJava8ToJava17() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <java.version>1.8</java.version>
                  <maven.compiler.source>1.8</maven.compiler.source>
                  <maven.compiler.target>1.8</maven.compiler.target>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                 
                <properties>
                  <java.version>17</java.version>
                  <maven.compiler.source>17</maven.compiler.source>
                  <maven.compiler.target>17</maven.compiler.target>
                </properties>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          ),
          version(
            //language=java
            srcMainJava(
              java(
                """
                  package com.abc;
                          
                  import java.util.Collections;
                  import java.util.List;
                  import java.util.Map;
                  import java.util.Set;
                                        
                  class A {
                     private static final List<String> staticList = Collections.singletonList("0");
                     
                     /* This is a comment */
                     public void test() {
                         // This is a comment
                         Set<String> stringSet = Collections.singleton("aaa");
                         List<String> stringList = Collections.singletonList("bbb");
                         Map<String, Object> stringMap = Collections.singletonMap("a-key", "a-value");
                         Object value = stringMap.get("a-key");
                         if (value instanceof String) {
                             System.out.println(((String) value).length());
                         }
                     }
                  }
                  """,
                """
                  package com.abc;
                          
                  import java.util.List;
                  import java.util.Map;
                  import java.util.Set;
                          
                  class A {
                     private static final List<String> staticList = List.of("0");

                     /* This is a comment */
                     public void test() {
                         // This is a comment
                         Set<String> stringSet = Set.of("aaa");
                         List<String> stringList = List.of("bbb");
                         Map<String, Object> stringMap = Map.of("a-key", "a-value");
                         Object value = stringMap.get("a-key");
                         if (value instanceof String s) {
                             System.out.println(s.length());
                         }
                     }
                  }
                  """
              )
            ),
            17)
        );
    }
}
