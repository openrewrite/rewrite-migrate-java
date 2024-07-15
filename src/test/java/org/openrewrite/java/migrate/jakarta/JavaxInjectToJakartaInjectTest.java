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
package org.openrewrite.java.migrate.jakarta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

public class JavaxInjectToJakartaInjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxInjectMigrationToJakartaInject"));
    }

    @Language("xml")
    private static final String POM =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>0.0.1-SNAPSHOT</version>
          <name>demo</name>
          <description>Demo project for Jakarta Inject</description>
          <properties>
              <java.version>17</java.version>
          </properties>
          <dependencies>
              <dependency>
                    <groupId>javax.inject</groupId>
                    <artifactId>javax.inject</artifactId>
                    <version>1</version>
              </dependency>
          </dependencies>

          <build>
              <plugins>
                  <plugin>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-maven-plugin</artifactId>
                  </plugin>
              </plugins>
          </build>

      </project>
      """;

    private static final String JAKARTA_INJECT_REGEX =
      """
              <dependency>
                    <groupId>jakarta.inject</groupId>
                    <artifactId>jakarta.inject-api</artifactId>
                    <version>([0-9]+\\.[0-9]+\\.[0-9]+)</version>
              </dependency>
      """;



    @Test
    void projectWithJavaxInject() {
        rewriteRun(
          pomXml(POM,
            spec -> spec.after(actual -> {
                assertThat(actual).isNotNull();
                Matcher version = Pattern.compile(JAKARTA_INJECT_REGEX).matcher(actual);
                assertThat(version.find()).isTrue();

                return actual;
            })
          )
        );
    }

}
