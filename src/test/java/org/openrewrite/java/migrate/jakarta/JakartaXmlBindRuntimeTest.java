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

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.TypeValidation;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.srcTestJava;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class JakartaXmlBindRuntimeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JavaxXmlBindMigrationToJakartaXmlBind")
          .parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.xml.bind; public abstract class JAXBContext { public static JAXBContext newInstance(Class<?>... types) { return null; } }"));
    }

    @Test
    void addsRuntimeForContextFactory() {
        rewriteRun(
          mavenProject("app",
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.xml.bind</groupId>
                            <artifactId>jaxb-api</artifactId>
                            <version>2.3.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).contains("<artifactId>jaxb-runtime</artifactId>", "<scope>runtime</scope>");
                  return pom;
              })
            ),
            srcMainJava(java(
              """
                import javax.xml.bind.JAXBContext;
                class A {
                    JAXBContext context = JAXBContext.newInstance(A.class);
                }
                """,
              """
                import jakarta.xml.bind.JAXBContext;
                class A {
                    JAXBContext context = JAXBContext.newInstance(A.class);
                }
                """
            ))
          )
        );
    }

    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.example</groupId>
          <artifactId>app</artifactId>
          <version>1.0</version>
      </project>
      """;

    private static final String FACTORY = """
      import javax.xml.bind.JAXBContext;
      class A { JAXBContext context = JAXBContext.newInstance(A.class); }
      """;

    @Test
    void testOnlyFactoryGetsTestScope() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM, spec -> spec.after(pom -> {
                assertThat(pom).contains("<artifactId>jaxb-runtime</artifactId>", "<scope>test</scope>");
                return pom;
            })),
            srcTestJava(java(FACTORY))
          )
        );
    }

    @Test
    void doesNotAddRuntimeForApiReferencesWithoutFactoryCalls() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM),
            srcMainJava(java("import javax.xml.bind.JAXBContext; class A { JAXBContext context; }"))
          )
        );
    }

    @Test
    void leavesOtherModulesAlone() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM, spec -> spec.after(pom -> {
                assertThat(pom).contains("<artifactId>jaxb-runtime</artifactId>", "<scope>runtime</scope>");
                return pom;
            })),
            srcMainJava(java(FACTORY)),
            mavenProject("nested", pomXml(POM))
          ),
          mavenProject("unrelated", pomXml(POM))
        );
    }

    @Test
    void respectsExplicitMoxyProvider() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM.replace("</project>", """
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.persistence</groupId>
                          <artifactId>org.eclipse.persistence.moxy</artifactId>
                          <version>3.0.4</version>
                      </dependency>
                  </dependencies>
              </project>
              """)),
            srcMainJava(java(FACTORY))
          )
        );
    }

    @Test
    void respectsProvidedJakartaPlatform() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM.replace("</project>", """
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.platform</groupId>
                          <artifactId>jakarta.jakartaee-api</artifactId>
                          <version>9.1.0</version>
                          <scope>provided</scope>
                      </dependency>
                  </dependencies>
              </project>
              """)),
            srcMainJava(java(FACTORY))
          )
        );
    }

    @Test
    void recognizesUnattributedFactoryCalls() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()).typeValidationOptions(TypeValidation.none()),
          mavenProject("app",
            pomXml(POM, spec -> spec.after(pom -> {
                assertThat(pom).contains("<artifactId>jaxb-runtime</artifactId>", "<scope>runtime</scope>");
                return pom;
            })),
            srcMainJava(java(FACTORY, spec -> spec.beforeRecipe(cu -> new JavaIsoVisitor<Integer>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer unused) {
                    return super.visitMethodInvocation(method, unused).withMethodType(null);
                }
            }.visitNonNull(cu, 0))))
          )
        );
    }

    @Test
    void productionUsageWinsOverTestUsage() {
        rewriteRun(
          spec -> spec.recipe(new AddJakartaXmlBindRuntime()),
          mavenProject("app",
            pomXml(POM, spec -> spec.after(pom -> {
                assertThat(pom).contains("<artifactId>jaxb-runtime</artifactId>", "<scope>runtime</scope>")
                  .doesNotContain("<scope>test</scope>");
                return pom;
            })),
            srcMainJava(java(FACTORY)),
            srcTestJava(java(FACTORY.replace("class A", "class B").replace("A.class", "B.class")))
          )
        );
    }

    @Test
    void dependencyFreeApplicationGetsCompileApiAndRuntime() {
        rewriteRun(
          mavenProject("app",
            pomXml(POM, spec -> spec.after(pom -> {
                assertThat(pom).contains("<artifactId>jakarta.xml.bind-api</artifactId>",
                  "<artifactId>jaxb-runtime</artifactId>", "<scope>runtime</scope>");
                return pom;
            })),
            srcMainJava(java(FACTORY, FACTORY.replace("javax.xml.bind", "jakarta.xml.bind")))
          )
        );
    }
}
