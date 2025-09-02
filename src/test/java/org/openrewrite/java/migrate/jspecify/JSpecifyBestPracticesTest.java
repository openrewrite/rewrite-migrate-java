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
package org.openrewrite.java.migrate.jspecify;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

@SuppressWarnings("NotNullFieldNotInitialized")
class JSpecifyBestPracticesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/jspecify.yml", "org.openrewrite.java.jspecify.JSpecifyBestPractices")
          .parser(JavaParser.fromJavaVersion().classpath("jsr305", "jakarta.annotation-api", "annotations", "spring-core"));
    }

    @DocumentExample
    @Test
    void migrateFromJavaxAnnotationApiToJspecify() {
        rewriteRun(
          mavenProject("foo",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.annotation.Nonnull;
                  import javax.annotation.Nullable;

                  public class Test {
                      @Nonnull
                      public String field1;
                      @Nullable
                      public String field2;
                      @Nullable
                      public Foo.Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @Nonnull
                      public String barField;
                    }
                  }
                  """,
                """
                  import org.jspecify.annotations.NonNull;
                  import org.jspecify.annotations.Nullable;

                  public class Test {
                      @NonNull
                      public String field1;
                      @Nullable
                      public String field2;

                      public Foo.@Nullable Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NonNull
                      public String barField;
                    }
                  }
                  """
              ),
              //language=java
              java(
                """
                  @ParametersAreNonnullByDefault
                  package org.openrewrite.example;

                  import javax.annotation.ParametersAreNonnullByDefault;
                  """,
                """
                  @NullMarked
                  package org.openrewrite.example;

                  import org.jspecify.annotations.NullMarked;
                  """,
                spec -> spec.path("src/main/java/org/openrewrite/example/package-info.java")
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.annotation</groupId>
                            <artifactId>javax.annotation-api</artifactId>
                            <version>1.3.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.annotation</groupId>
                            <artifactId>javax.annotation-api</artifactId>
                            <version>1.3.2</version>
                        </dependency>
                        <dependency>
                            <groupId>org.jspecify</groupId>
                            <artifactId>jspecify</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void migrateFromJakartaAnnotationApiToJspecify() {
        rewriteRun(
          mavenProject("foo",
            //language=java
            srcMainJava(
              java(
                """
                  import jakarta.annotation.Nonnull;
                  import jakarta.annotation.Nullable;

                  public class Test {
                      @Nonnull
                      public String field1;
                      @Nullable
                      public String field2;
                      @Nullable
                      public Foo.Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @Nonnull
                      public String barField;
                    }
                  }
                  """,
                """
                  import org.jspecify.annotations.NonNull;
                  import org.jspecify.annotations.Nullable;

                  public class Test {
                      @NonNull
                      public String field1;
                      @Nullable
                      public String field2;

                      public Foo.@Nullable Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NonNull
                      public String barField;
                    }
                  }
                  """
              )
            )
            ,
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.annotation</groupId>
                            <artifactId>jakarta.annotation-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.annotation</groupId>
                            <artifactId>jakarta.annotation-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.jspecify</groupId>
                            <artifactId>jspecify</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void migrateFromJetbrainsAnnotationsToJspecify() {
        rewriteRun(
          mavenProject("foo",
            //language=java
            srcMainJava(
              java(
                """
                  import org.jetbrains.annotations.NotNull;
                  import org.jetbrains.annotations.Nullable;

                  public class Test {
                      @NotNull
                      public String field1;
                      @Nullable
                      public String field2;
                      @Nullable
                      public Foo.Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NotNull
                      public String barField;
                    }
                  }
                  """,
                """
                  import org.jspecify.annotations.NonNull;
                  import org.jspecify.annotations.Nullable;

                  public class Test {
                      @NonNull
                      public String field1;
                      @Nullable
                      public String field2;

                      public Foo.@Nullable Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NonNull
                      public String barField;
                    }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains</groupId>
                            <artifactId>annotations</artifactId>
                            <version>24.1.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains</groupId>
                            <artifactId>annotations</artifactId>
                            <version>24.1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.jspecify</groupId>
                            <artifactId>jspecify</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/pull/602")
    @Test
    void migrateFromSpringFrameworkAnnotationsToJspecify() {
        rewriteRun(
          spec -> spec.recipeFromResource(
            "/META-INF/rewrite/jspecify.yml",
            "org.openrewrite.java.jspecify.MigrateFromSpringFrameworkAnnotations"),
          mavenProject("foo",
            //language=java
            srcMainJava(
              java(
                """
                  import org.springframework.lang.NonNull;
                  import org.springframework.lang.Nullable;

                  public class Test {
                      @NonNull
                      public String field1;
                      @Nullable
                      public String field2;
                      @Nullable
                      public Foo.Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NonNull
                      public String barField;
                    }
                  }
                  """,
                """
                  import org.jspecify.annotations.NonNull;
                  import org.jspecify.annotations.Nullable;

                  public class Test {
                      @NonNull
                      public String field1;
                      @Nullable
                      public String field2;

                      public Foo.@Nullable Bar foobar;
                  }

                  interface Foo {
                    class Bar {
                      @NonNull
                      public String barField;
                    }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-core</artifactId>
                            <version>6.1.13</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example.foobar</groupId>
                    <artifactId>foobar-core</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.jspecify</groupId>
                            <artifactId>jspecify</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework</groupId>
                            <artifactId>spring-core</artifactId>
                            <version>6.1.13</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
