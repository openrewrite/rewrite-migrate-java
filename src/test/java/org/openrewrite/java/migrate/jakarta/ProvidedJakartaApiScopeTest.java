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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class ProvidedJakartaApiScopeTest implements RewriteTest {
    @ParameterizedTest
    @CsvSource({
      "JavaxAnnotationMigrationToJakartaAnnotation,javax.annotation,jsr250-api,1.0,annotation,Resource",
      "JavaxElToJakartaEl,javax.el,el-api,2.2,el,ExpressionFactory",
      "JavaxEjbToJakartaEjb,javax.ejb,ejb-api,3.0,ejb,EJB",
      "JavaxEjbToJakartaEjb,org.jboss.spec.javax.ejb,jboss-ejb-api_3.1_spec,1.0.2.Final,ejb,EJB",
      "JavaxMailToJakartaMail,javax.mail,mail,1.4.7,mail,Session"
    })
    void migratesLegacyApiAndPreservesProvidedScope(String recipe, String group, String artifact,
                                                   String version, String namespace, String type) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.jakarta." + recipe)
            .parser(JavaParser.fromJavaVersion().dependsOn(
              "package javax." + namespace + "; public class " + type + " {}")),
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
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>%s</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(group, artifact, version),
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta." + namespace + "</groupId>",
                  "<artifactId>jakarta." + namespace + "-api</artifactId>", "<scope>provided</scope>")
                .doesNotContain("<artifactId>" + artifact + "</artifactId>", "<scope>compile</scope>").actual())
            ),
            srcMainJava(
              java(
                "class A { javax." + namespace + "." + type + " api; }",
                "class A { jakarta." + namespace + "." + type + " api; }"
              )
            )
          )
        );
    }
}
