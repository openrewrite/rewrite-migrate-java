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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxEnterpriseToJakartaEnterpriseTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JavaxEnterpriseToJakartaEnterprise");
    }

    @Test
    void preservesProvidedCdiApi() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.enterprise.inject.spi; public interface BeanManager {}")),
          mavenProject("demo",
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.enterprise</groupId>
                            <artifactId>cdi-api</artifactId>
                            <version>1.0</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("jakarta.enterprise.cdi-api", "<scope>provided</scope>")
                .doesNotContain("<scope>compile</scope>").actual())
            ),
            srcMainJava(
              java(
                "class A { javax.enterprise.inject.spi.BeanManager manager; }",
                "class A { jakarta.enterprise.inject.spi.BeanManager manager; }"
              )
            )
          )
        );
    }
    @Test
    void stillAddsMissingCdiApi() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.enterprise.inject.spi; public interface BeanManager {}")),
          mavenProject("demo",
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>1.0</version>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom).contains("jakarta.enterprise.cdi-api").actual())
            ),
            srcMainJava(
              java(
                "class A { javax.enterprise.inject.spi.BeanManager manager; }",
                "class A { jakarta.enterprise.inject.spi.BeanManager manager; }"
              )
            )
          )
        );
    }

}
