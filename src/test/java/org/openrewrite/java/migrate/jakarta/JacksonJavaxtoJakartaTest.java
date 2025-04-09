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
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class JacksonJavaxtoJakartaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JacksonJavaxToJakarta"));
    }

    @DocumentExample
    @Test
    void migrateJacksonDependencies() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example.jackson</groupId>
                  <artifactId>jackson-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-jaxb-annotations</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jaxrs</groupId>
                          <artifactId>jackson-jaxrs-cbor-provider</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jaxrs</groupId>
                          <artifactId>jackson-jaxrs-json-provider</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jaxrs</groupId>
                          <artifactId>jackson-jaxrs-smile-provider</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jaxrs</groupId>
                          <artifactId>jackson-jaxrs-xml-provider</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jaxrs</groupId>
                          <artifactId>jackson-jaxrs-yaml-provider</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.datatype</groupId>
                          <artifactId>jackson-datatype-jsr353</artifactId>
                          <version>2.12.1</version>
                      </dependency>
                      <dependency>
                          <groupId>org.glassfish</groupId>
                          <artifactId>javax.json</artifactId>
                          <version>1.1.4</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.example.jackson</groupId>
                  <artifactId>jackson-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-jakarta-xmlbind-annotations</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                          <artifactId>jackson-jakarta-rs-cbor-provider</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                          <artifactId>jackson-jakarta-rs-json-provider</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                          <artifactId>jackson-jakarta-rs-smile-provider</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                          <artifactId>jackson-jakarta-rs-xml-provider</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                          <artifactId>jackson-jakarta-rs-yaml-provider</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.datatype</groupId>
                          <artifactId>jackson-datatype-jakarta-jsonp</artifactId>
                          <version>2.13.5</version>
                      </dependency>
                      <dependency>
                          <groupId>org.glassfish</groupId>
                          <artifactId>jakarta.json</artifactId>
                          <version>2.0.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void migrateJacksonManagedDependencies() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example.jackson</groupId>
                  <artifactId>jackson-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.module</groupId>
                              <artifactId>jackson-module-jaxb-annotations</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jaxrs</groupId>
                              <artifactId>jackson-jaxrs-cbor-provider</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jaxrs</groupId>
                              <artifactId>jackson-jaxrs-json-provider</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jaxrs</groupId>
                              <artifactId>jackson-jaxrs-smile-provider</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jaxrs</groupId>
                              <artifactId>jackson-jaxrs-xml-provider</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jaxrs</groupId>
                              <artifactId>jackson-jaxrs-yaml-provider</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.datatype</groupId>
                              <artifactId>jackson-datatype-jsr353</artifactId>
                              <version>2.12.1</version>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish</groupId>
                              <artifactId>javax.json</artifactId>
                              <version>1.1.4</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            """
              <project>
                  <groupId>com.example.jackson</groupId>
                  <artifactId>jackson-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.fasterxml.jackson.module</groupId>
                              <artifactId>jackson-module-jakarta-xmlbind-annotations</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                              <artifactId>jackson-jakarta-rs-cbor-provider</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                              <artifactId>jackson-jakarta-rs-json-provider</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                              <artifactId>jackson-jakarta-rs-smile-provider</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                              <artifactId>jackson-jakarta-rs-xml-provider</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.jakarta.rs</groupId>
                              <artifactId>jackson-jakarta-rs-yaml-provider</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>com.fasterxml.jackson.datatype</groupId>
                              <artifactId>jackson-datatype-jakarta-jsonp</artifactId>
                              <version>2.13.5</version>
                          </dependency>
                          <dependency>
                              <groupId>org.glassfish</groupId>
                              <artifactId>jakarta.json</artifactId>
                              <version>2.0.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void changeJsonpModuleType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath(
            "jackson-datatype-jsr353",
            "jackson-core",
            "jackson-databind")),
          //language=java
          java(
            """
              import com.fasterxml.jackson.databind.ObjectMapper;
              import com.fasterxml.jackson.databind.json.JsonMapper;
              import com.fasterxml.jackson.datatype.jsr353.JSR353Module;

              public class JacksonTest {
                  ObjectMapper mapper = JsonMapper.builder().addModule(new JSR353Module()).build();
                  ObjectMapper mapper2 = JsonMapper.builder().addModule(getModule()).build();

                  private JSR353Module getModule() {
                      return new JSR353Module();
                  }
              }
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper;
              import com.fasterxml.jackson.databind.json.JsonMapper;
              import com.fasterxml.jackson.datatype.jsonp.JSONPModule;

              public class JacksonTest {
                  ObjectMapper mapper = JsonMapper.builder().addModule(new JSONPModule()).build();
                  ObjectMapper mapper2 = JsonMapper.builder().addModule(getModule()).build();

                  private JSONPModule getModule() {
                      return new JSONPModule();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/652")
    @Test
    void thatJaxbAnnotationModuleIsRewritten() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath(
            "jackson-core",
            "jackson-databind",
            "jackson-module-jaxb-annotations")),
          //language=java
          java(
            """
              import com.fasterxml.jackson.databind.ObjectMapper;
              import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

              public class JacksonTest {
                  void foo() {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.registerModule(new JaxbAnnotationModule());
                  }
              }
              """,
            """
              import com.fasterxml.jackson.databind.ObjectMapper;
              import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

              public class JacksonTest {
                  void foo() {
                      ObjectMapper mapper = new ObjectMapper();
                      mapper.registerModule(new JakartaXmlBindAnnotationModule());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/653")
    @Test
    void thatJaxbJsonProviderIsRewritten() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath(
              "jackson-databind",
              "jackson-jaxrs-json-provider")),
          //language=java
          java(
            """
              import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

              public class A extends JacksonJaxbJsonProvider {}
              """,
            """
              import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

              public class A extends JacksonXmlBindJsonProvider {}
              """
          )
        );
    }
}
