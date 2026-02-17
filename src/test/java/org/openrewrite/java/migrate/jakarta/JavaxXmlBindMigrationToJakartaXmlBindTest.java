/*
 * Copyright 2025 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

@SuppressWarnings("LanguageMismatch")
class JavaxXmlBindMigrationToJakartaXmlBindTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind"));
    }

    @Language("java")
    private static final String XML_ELEMENT_STUB = """
      package javax.xml.bind.annotation;
      public @interface XmlElement {}
      """;

    @Language("java")
    private static final String JAKARTA_XML_ELEMENT_STUB = """
      package jakarta.xml.bind.annotation;
      public @interface XmlElement {}
      """;

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/504")
    @Test
    void retainJaxbApiWhenJacksonJaxbAnnotationsPresent() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().dependsOn(XML_ELEMENT_STUB, JAKARTA_XML_ELEMENT_STUB))
            .expectedCyclesThatMakeChanges(2),
          //language=java
          java(
            """
              import javax.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """,
            """
              import jakarta.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """
          ),
          buildGradle(
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "javax.xml.bind:jaxb-api:2.3.1"
                  api "com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.16.0"
              }
              """,
            spec -> spec.after(buildGradle -> {
                // Verify that both jakarta.xml.bind-api AND jaxb-api are present
                assertThat(buildGradle)
                  .contains("jakarta.xml.bind:jakarta.xml.bind-api")
                  .contains("javax.xml.bind:jaxb-api")
                  .contains("jackson-module-jaxb-annotations");
                return buildGradle;
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/504")
    @Test
    void retainJaxbApiWhenJacksonJaxbAnnotationsPresentMaven() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(XML_ELEMENT_STUB, JAKARTA_XML_ELEMENT_STUB))
            .expectedCyclesThatMakeChanges(2),
          //language=java
          java(
            """
              import javax.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """,
            """
              import jakarta.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """
          ),
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.bind</groupId>
                          <artifactId>jaxb-api</artifactId>
                          <version>2.3.1</version>
                      </dependency>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-jaxb-annotations</artifactId>
                          <version>2.16.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                assertThat(pom)
                  .contains("jakarta.xml.bind")
                  .contains("jakarta.xml.bind-api")
                  .contains("javax.xml.bind")
                  .contains("jaxb-api")
                  .contains("jackson-module-jaxb-annotations");
                return pom;
            })
          )
        );
    }

    @Test
    void dontRetainJaxbApiWhenJacksonNotPresent() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().dependsOn(XML_ELEMENT_STUB, JAKARTA_XML_ELEMENT_STUB)),
          //language=java
          java(
            """
              import javax.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """,
            """
              import jakarta.xml.bind.annotation.XmlElement;

              public class Test {
                  @XmlElement
                  private String name;
              }
              """
          ),
          buildGradle(
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "javax.xml.bind:jaxb-api:2.3.1"
              }
              """,
            spec -> spec.after(buildGradle -> {
                assertThat(buildGradle)
                  .contains("jakarta.xml.bind:jakarta.xml.bind-api")
                  .doesNotContain("javax.xml.bind:jaxb-api");
                return buildGradle;
            })
          )
        );
    }
}
