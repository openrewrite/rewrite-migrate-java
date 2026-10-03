/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddApiDependencyForImportsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddApiDependencyForImports("jakarta.xml.bind", "jakarta.xml.bind", "jakarta.xml.bind-api", "3.0.1"))
          .parser(JavaParser.fromJavaVersion().dependsOn("package javax.xml.bind; public class JAXBContext {}"));
    }

    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.example</groupId>
          <artifactId>app</artifactId>
          <version>1.0</version>
      </project>
      """;

    private static final String USAGE = """
      import javax.xml.bind.JAXBContext;
      class A { JAXBContext context; }
      """;

    @Test
    void testOnlyUsage() {
        rewriteRun(
          mavenProject("app",
            srcTestJava(java(USAGE)),
            pomXml(POM, spec -> spec.after(pom -> assertThat(pom)
              .contains("<artifactId>jakarta.xml.bind-api</artifactId>")
              .contains("<scope>test</scope>").actual()))
          )
        );
    }

    @Test
    void mainAndTestUsageRequiresCompileScope() {
        rewriteRun(
          mavenProject("app",
            srcTestJava(java(USAGE.replace("class A", "class ATest"))),
            srcMainJava(java(USAGE)),
            pomXml(POM, spec -> spec.after(pom -> assertThat(pom)
              .contains("<artifactId>jakarta.xml.bind-api</artifactId>")
              .doesNotContain("<scope>test</scope>").actual()))
          )
        );
    }

    @Test
    void preservesProvidedApi() {
        rewriteRun(
          mavenProject("app",
            srcMainJava(java(USAGE)),
            pomXml(withDependency("provided"))
          )
        );
    }

    @Test
    void promotesTestApiForMainUsage() {
        rewriteRun(
          mavenProject("app",
            srcMainJava(java(USAGE)),
            pomXml(withDependency("test"), spec -> spec.after(pom -> assertThat(pom)
              .contains("<artifactId>jakarta.xml.bind-api</artifactId>")
              .doesNotContain("<scope>test</scope>").actual()))
          )
        );
    }

    @Test
    void promotesRuntimeApiForMainUsage() {
        rewriteRun(
          mavenProject("app",
            srcMainJava(java(USAGE)),
            pomXml(withDependency("runtime"), spec -> spec.after(pom -> assertThat(pom)
              .contains("<artifactId>jakarta.xml.bind-api</artifactId>")
              .doesNotContain("<scope>runtime</scope>").actual()))
          )
        );
    }

    @Test
    void runtimeProviderDoesNotSupplyCompileApi() {
        rewriteRun(
          mavenProject("app",
            srcMainJava(java(USAGE)),
            pomXml(withDependency("runtime")
                .replace("<groupId>jakarta.xml.bind</groupId>", "<groupId>org.glassfish.jaxb</groupId>")
                .replace("<artifactId>jakarta.xml.bind-api</artifactId>", "<artifactId>jaxb-runtime</artifactId>")
                .replace("<version>3.0.1</version>", "<version>3.0.2</version>"),
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<artifactId>jaxb-runtime</artifactId>")
                .contains("<artifactId>jakarta.xml.bind-api</artifactId>")
                .containsOnlyOnce("<scope>runtime</scope>").actual()))
          )
        );
    }

    @Test
    void leavesUnrelatedModuleAlone() {
        rewriteRun(
          mavenProject("app",
            srcMainJava(java(USAGE)),
            pomXml(POM, spec -> spec.after(pom -> assertThat(pom)
              .contains("<artifactId>jakarta.xml.bind-api</artifactId>").actual()))
          ),
          mavenProject("other",
            srcMainJava(java("class B {}")),
            pomXml(POM.replace("<artifactId>app</artifactId>", "<artifactId>other</artifactId>"))
          )
        );
    }

    @Test
    void leavesAnnotationProcessingApiAlone() {
        rewriteRun(
          spec -> spec.recipe(new AddApiDependencyForImports(
            "jakarta.annotation", "jakarta.annotation", "jakarta.annotation-api", "2.0.0")),
          mavenProject("app",
            srcMainJava(java("import javax.annotation.processing.Processor; class A { Processor processor; }")),
            pomXml(POM)
          )
        );
    }

    private static String withDependency(String scope) {
        return POM.replace("</project>", """
              <dependencies>
                  <dependency>
                      <groupId>jakarta.xml.bind</groupId>
                      <artifactId>jakarta.xml.bind-api</artifactId>
                      <version>3.0.1</version>
                      <scope>%s</scope>
                  </dependency>
              </dependencies>
          </project>""".formatted(scope));
    }
}
