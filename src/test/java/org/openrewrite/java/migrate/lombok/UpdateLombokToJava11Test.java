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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateLombokToJava11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.lombok")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.lombok.UpdateLombokToJava11")
          )
          .parser(
            //language=java
            JavaParser.fromJavaVersion().dependsOn(
              """
                package lombok.experimental;

                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;

                @Target({ElementType.FIELD, ElementType.TYPE})
                @Retention(RetentionPolicy.SOURCE)
                public @interface Wither {
                }

                @Target(ElementType.TYPE)
                @Retention(RetentionPolicy.SOURCE)
                public @interface Value {
                }

                @Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
                @Retention(RetentionPolicy.SOURCE)
                public @interface Builder {
                }
                """
            )
          );
    }

    @SuppressWarnings({"DeprecatedLombok", "deprecation", "Lombok", "RedundantModifiersValueLombok"})
    @Test
    void updateLombokToJava11() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example.jackson</groupId>
                  <artifactId>jackson-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.projectlombok</groupId>
                          <artifactId>lombok</artifactId>
                          <version>1.18.6</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("1.[1-9]\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                //language=xml
                return """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example.jackson</groupId>
                      <artifactId>jackson-legacy</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>org.projectlombok</groupId>
                              <artifactId>lombok</artifactId>
                              <version>%s</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(version.group(0));
            })
          ),
          //language=java
          java(
            """
              import lombok.experimental.Wither;
              import lombok.experimental.Builder;
              import lombok.experimental.Value;

              @Wither
              @Builder
              @Value
              public class Fred {
                  private String firstName;
                  private String lastName;
              }
              """,
            """
              import lombok.Value;
              import lombok.With;
              import lombok.Builder;

              @With
              @Builder
              @Value
              public class Fred {
                  private String firstName;
                  private String lastName;
              }
              """
          )
        );
    }
}
