/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.java.Assertions.java;

public class UpdateLombokToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.lombok")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.lombok.UpdateLombokToJava17")
        ).parser(JavaParser.fromJavaVersion().dependsOn(
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

        ));
    }

    @Test
    void upgradeLombokToJava17() {

        rewriteRun(
          pomXml(
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
                            <version>1.18.24</version>
                        </dependency>
                    </dependencies>
                </project>
            """
          ),
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
