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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.maven.Assertions.pomXml;

class AddCommonAnnotationsDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
            "/META-INF/rewrite/add-common-annotations-dependencies.yml",
            "org.openrewrite.java.migrate.javax.AddCommonAnnotationsDependencies")
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @Test
    void preserveJavaxAlongsideModernTransitiveAnnotations() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <dependencies>
              <dependency>
                <groupId>org.apache.cxf</groupId>
                <artifactId>cxf-core</artifactId>
                <version>4.0.5</version>
              </dependency>
              <dependency>
                <groupId>javax.annotation</groupId>
                <artifactId>javax.annotation-api</artifactId>
                <version>1.3.2</version>
                <scope>test</scope>
              </dependency>
            </dependencies>
          </project>
          """));
    }

    @Test
    void preserveJavaxAlongsidePropertyBasedModernAnnotations() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><annotations.version>2.1.1</annotations.version></properties>
            <dependencies>
              <dependency>
                <groupId>jakarta.annotation</groupId>
                <artifactId>jakarta.annotation-api</artifactId>
                <version>${annotations.version}</version>
              </dependency>
              <dependency>
                <groupId>javax.annotation</groupId>
                <artifactId>javax.annotation-api</artifactId>
                <version>1.3.2</version>
              </dependency>
            </dependencies>
          </project>
          """));
    }

    @Test
    void addSeparateJavaxDependencyForModernGraph() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package javax.annotation; public @interface Generated {}")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.Generated;

                  @Generated("Hello")
                  class A {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
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
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.annotation</groupId>
                      <artifactId>jakarta.annotation-api</artifactId>
                      <version>3.0.0</version>
                    </dependency>
                    <dependency>
                      <groupId>javax.annotation</groupId>
                      <artifactId>javax.annotation-api</artifactId>
                      <version>1.3.2</version>
                      <scope>provided</scope>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void preserveManagedModernAnnotations() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>org.sample</groupId>
            <artifactId>sample</artifactId>
            <version>1</version>
            <properties><annotations.version>3.0.0</annotations.version></properties>
            <dependencyManagement>
              <dependencies>
                <dependency>
                  <groupId>jakarta.annotation</groupId>
                  <artifactId>jakarta.annotation-api</artifactId>
                  <version>${annotations.version}</version>
                </dependency>
              </dependencies>
            </dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>javax.annotation</groupId>
                <artifactId>javax.annotation-api</artifactId>
                <version>1.3.2</version>
              </dependency>
            </dependencies>
          </project>
          """));
    }

    @Test
    void preserveGradleJavaxAlongsideModernTransitiveAnnotations() {
        ResolvedDependency modern = ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, "jakarta.annotation", "jakarta.annotation-api", "3.0.0", null))
          .requested(Dependency.builder()
            .gav(new GroupArtifactVersion("jakarta.annotation", "jakarta.annotation-api", "3.0.0")).build())
          .depth(1).type("jar").build();
        GradleDependencyConfiguration configuration =
          new GradleDependencyConfiguration("compileClasspath", null, true, true, false, false,
            Collections.emptyList(), Collections.emptyList(), Collections.singletonList(modern), null, null);
        rewriteRun(buildGradle("""
          plugins { id 'java' }
          repositories { mavenCentral() }
          dependencies {
              implementation 'javax.annotation:javax.annotation-api:1.3.2'
          }
          """, spec -> spec.markers(GradleProject.builder()
            .name("sample").path(":sample")
            .nameToConfiguration(Collections.singletonMap("compileClasspath", configuration)).build())));
    }

    @DocumentExample
    @Test
    void addDependencyIfAnnotationJsr250Present() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package javax.annotation; public @interface Generated {}")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.Generated;

                  @Generated("Hello")
                  class A {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.annotation</groupId>
                      <artifactId>jakarta.annotation-api</artifactId>
                      <version>1.3.5</version>
                      <scope>provided</scope>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void changeAndUpgradeDependencyIfAnnotationJsr250Present() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn("package javax.annotation; public @interface Generated {}")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.Generated;

                  @Generated("Hello")
                  class A {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
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
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.annotation</groupId>
                      <artifactId>jakarta.annotation-api</artifactId>
                      <version>1.3.5</version>
                      <scope>provided</scope>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void addSingleDependencyForMultipleJsr250Annotations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.annotation; public @interface PostConstruct {}",
            "package javax.annotation.security; public @interface RolesAllowed { String[] value(); }")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.PostConstruct;
                  import javax.annotation.security.RolesAllowed;

                  @RolesAllowed("admin")
                  class A {
                      @PostConstruct
                      void init() {
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
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>jakarta.annotation</groupId>
                      <artifactId>jakarta.annotation-api</artifactId>
                      <version>1.3.5</version>
                      <scope>provided</scope>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1312")
    @Test
    void doNotAddDependencyForJsr305Annotations() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            "package javax.annotation; public @interface Nullable {}",
            "package javax.annotation.concurrent; public @interface ThreadSafe {}")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.Nullable;
                  import javax.annotation.concurrent.ThreadSafe;

                  @ThreadSafe
                  class A {
                      @Nullable
                      private String name;
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.code.findbugs</groupId>
                      <artifactId>jsr305</artifactId>
                      <version>3.0.2</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );
    }
}
