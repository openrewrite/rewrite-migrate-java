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
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxXmlWsMigrationToJakartaXmlWsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build().activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxXmlWsMigrationToJakartaXmlWs"))
          .parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.xml.ws.http; public class HTTPException extends RuntimeException {}"));
    }

    @Test
    void addApiForTypesPreviouslySuppliedByJavaEight() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          mavenProject("app",
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<artifactId>jakarta.xml.ws-api</artifactId>")
                .actual())
            ),
            srcMainJava(
              java(
                """
                  import javax.xml.ws.http.HTTPException;
                  class A { HTTPException error; }
                  """,
                """
                  import jakarta.xml.ws.http.HTTPException;
                  class A { HTTPException error; }
                  """,
                source -> source.mapBeforeRecipe(cu -> (J.CompilationUnit)
                  new JavaIsoVisitor<Integer>() {
                      @Override
                      public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
                          return id.getType() != null && id.getType().toString().startsWith("javax.xml.ws") ?
                            id.withType(JavaType.Unknown.getInstance()) : id;
                      }

                      @Override
                      public J.FieldAccess visitFieldAccess(J.FieldAccess fa, Integer p) {
                          J.FieldAccess f = super.visitFieldAccess(fa, p);
                          return f.getType() != null && f.getType().toString().startsWith("javax.xml.ws") ?
                            f.withType(JavaType.Unknown.getInstance()) : f;
                      }
                  }.visit(cu, 0))
              )
            )
          )
        );
    }
}
