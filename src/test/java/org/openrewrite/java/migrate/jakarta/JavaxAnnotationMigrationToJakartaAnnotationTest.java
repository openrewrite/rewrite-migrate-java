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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxAnnotationMigrationToJakartaAnnotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
          "/META-INF/rewrite/jakarta-ee-9.yml",
          "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
    }

    @DocumentExample
    @Test
    void migratesExplicitDependency() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn("package javax.annotation; public @interface Nonnull {}")),
          mavenProject("my-project",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """,
                """
                  import jakarta.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.annotation</groupId>
                            <artifactId>javax.annotation-api</artifactId>
                            <version>1.3.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                    .contains("<groupId>jakarta.annotation</groupId>")
                    .contains("<artifactId>jakarta.annotation-api</artifactId>")
                    .containsPattern("<version>2\\.0\\.\\d+</version>")
                    .actual())
            )
          )
        );
    }

    @Test
    void addsDependencyWhenNoExplicitAnnotationDependency() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn("package javax.annotation; public @interface Nonnull {}")),
          mavenProject("my-project",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """,
                """
                  import jakarta.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                    .containsPattern(
                      """
                      <groupId>jakarta\\.annotation</groupId>\\s*\
                      <artifactId>jakarta\\.annotation-api</artifactId>\\s*\
                      <version>2\\.0\\.\\d+</version>""")
                    .actual())
            )
          )
        );
    }

    @Test
    void addApiForUnattributedJdkAnnotation() {
        rewriteRun(
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          mavenProject("app",
            srcMainJava(
              java(
                """
                  import javax.annotation.Resource;
                  class A {
                      @Resource
                      String value;
                  }
                  """,
                """
                  import jakarta.annotation.Resource;
                  class A {
                      @Resource
                      String value;
                  }
                  """,
                // Saved Java 8 models can have Unknown annotation types instead of the parser's shallow types.
                source -> source.mapBeforeRecipe(cu -> (J.CompilationUnit)
                  new JavaIsoVisitor<Integer>() {
                      @Override
                      public J.Identifier visitIdentifier(J.Identifier id, Integer p) {
                          return id.getType() != null && id.getType().toString().startsWith("javax.annotation") ?
                            id.withType(JavaType.Unknown.getInstance()) : id;
                      }

                      @Override
                      public J.FieldAccess visitFieldAccess(J.FieldAccess fa, Integer p) {
                          J.FieldAccess f = super.visitFieldAccess(fa, p);
                          return f.getType() != null && f.getType().toString().startsWith("javax.annotation") ?
                            f.withType(JavaType.Unknown.getInstance()) : f;
                      }
                  }.visit(cu, 0))
              )
            ),
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0</version>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<artifactId>jakarta.annotation-api</artifactId>")
                .doesNotContain("<scope>test</scope>")
                .actual())
            )
          )
        );
    }

    @Test
    void keepsJsr305Annotations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.annotation; public @interface CheckReturnValue {}",
              "package javax.annotation; public @interface CheckForNull {}",
              "package javax.annotation; public @interface Nonnegative {}",
              "package javax.annotation; public @interface ParametersAreNonnullByDefault {}",
              "package javax.annotation; public @interface WillClose {}",
              "package javax.annotation.concurrent; public @interface ThreadSafe {}"
            )),
          //language=java
          java(
            """
              import javax.annotation.CheckForNull;
              import javax.annotation.CheckReturnValue;
              import javax.annotation.Nonnegative;
              import javax.annotation.ParametersAreNonnullByDefault;
              import javax.annotation.WillClose;
              import javax.annotation.concurrent.ThreadSafe;

              @ThreadSafe
              @ParametersAreNonnullByDefault
              class Jsr305 {
                  @CheckForNull
                  String name() {
                      return null;
                  }

                  @CheckReturnValue
                  @Nonnegative
                  int size() {
                      return 0;
                  }

                  void close(@WillClose AutoCloseable closeable) throws Exception {
                      closeable.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void migratesJsr250AnnotationsNextToJsr305() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.annotation; public @interface CheckReturnValue {}",
              "package javax.annotation; public @interface PostConstruct {}"
            )),
          //language=java
          java(
            """
              import javax.annotation.CheckReturnValue;
              import javax.annotation.PostConstruct;

              class Bean {
                  @PostConstruct
                  void init() {
                  }

                  @CheckReturnValue
                  Bean self() {
                      return this;
                  }
              }
              """,
            """
              import jakarta.annotation.PostConstruct;

              import javax.annotation.CheckReturnValue;

              class Bean {
                  @PostConstruct
                  void init() {
                  }

                  @CheckReturnValue
                  Bean self() {
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void splitsWildcardImportBetweenJakartaAndJsr305() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.annotation; public @interface CheckReturnValue {}",
              "package javax.annotation; public @interface PostConstruct {}",
              "package javax.annotation; public @interface PreDestroy {}"
            )),
          //language=java
          java(
            """
              import javax.annotation.*;

              class Bean {
                  @PostConstruct
                  void init() {
                  }

                  @PreDestroy
                  void stop() {
                  }

                  @CheckReturnValue
                  Bean self() {
                      return this;
                  }
              }
              """,
            """
              import jakarta.annotation.PostConstruct;
              import jakarta.annotation.PreDestroy;

              import javax.annotation.CheckReturnValue;

              class Bean {
                  @PostConstruct
                  void init() {
                  }

                  @PreDestroy
                  void stop() {
                  }

                  @CheckReturnValue
                  Bean self() {
                      return this;
                  }
              }
              """
          )
        );
    }

    @Test
    void migratesEveryJakartaAnnotationApiType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.annotation; public @interface Generated { String[] value(); }",
              "package javax.annotation; public @interface PreDestroy {}",
              "package javax.annotation; public @interface Priority { int value(); }",
              "package javax.annotation; public @interface Resource { String name() default \"\"; }"
            )),
          //language=java
          java(
            """
              import javax.annotation.Generated;
              import javax.annotation.PreDestroy;
              import javax.annotation.Priority;
              import javax.annotation.Resource;

              @Generated("com.example.Generator")
              @Priority(1)
              class Bean {
                  @Resource(name = "jdbc/ds")
                  Object dataSource;

                  @PreDestroy
                  void shutdown() {
                  }
              }
              """,
            """
              import jakarta.annotation.Generated;
              import jakarta.annotation.PreDestroy;
              import jakarta.annotation.Priority;
              import jakarta.annotation.Resource;

              @Generated("com.example.Generator")
              @Priority(1)
              class Bean {
                  @Resource(name = "jdbc/ds")
                  Object dataSource;

                  @PreDestroy
                  void shutdown() {
                  }
              }
              """
          )
        );
    }
}
