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
package org.openrewrite.java.migrate.jakarta;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxToJakartaTest implements RewriteTest {

    @Language("java")
    private static final String javax =
      """
        package javax.xml.bind.annotation;
        public class A {
            public static void stat() {}
            public void foo() {}
        }
        """;

    @Language("java")
    private static final String javaxServlet =
      """
        package javax.servlet;
        public class A {
            public static void stat() {}
            public void foo() {}
        }
        """;

    @Language("java")
    private static final String jakarta =
      """
        package jakarta.xml.bind.annotation;
        public class A {
            public static void stat() {}
            public void foo() {}
        }
        """;

    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")
        );
    }

    @Test
    void doNotAddImportWhenNoChangesWereMade() {
        rewriteRun(
          java("public class B {}")
        );
    }

    @DocumentExample
    @Test
    void changeImport() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          //language=java
          java(
            """
              import javax.xml.bind.annotation.A;
              public class B {
              }
              """,
            """
              import jakarta.xml.bind.annotation.A;
              public class B {
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedName() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          //language=java
          java(
            "public class B extends javax.xml.bind.annotation.A {}",
            "public class B extends jakarta.xml.bind.annotation.A {}"
          )
        );
    }

    @Test
    void annotation() {
        //language=java
        rewriteRun(
          spec ->
            spec.parser(
              JavaParser.fromJavaVersion().dependsOn(
                """
                  package javax.xml.bind.annotation;
                  public @interface A {}
                  """,
                """
                  package jakarta.xml.bind.annotation;
                  public @interface A {}
                  """
              )
            )
          ,
          java(
            "@javax.xml.bind.annotation.A public class B {}",
            "@jakarta.xml.bind.annotation.A public class B {}"
          )
        );
    }

    @Test
    void arrayTypesAndNewArrays() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class B {
                 javax.xml.bind.annotation.A[] a = new javax.xml.bind.annotation.A[0];
              }
              """,
            """
              public class B {
                 jakarta.xml.bind.annotation.A[] a = new jakarta.xml.bind.annotation.A[0];
              }
              """
          )
        );
    }

    @Test
    void classDecl() {
        rewriteRun(
          spec -> spec
            .recipe(Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")
              .doNext(new ChangeType("I1", "I2", false))
            )
            .parser(JavaParser.fromJavaVersion()
              //language=java
              .dependsOn(
                javax,
                jakarta,
                "public interface I1 {}",
                "public interface I2 {}"
              )
            ),
          //language=java
          java(
            "public class B extends javax.xml.bind.annotation.A implements I1 {}",
            "public class B extends jakarta.xml.bind.annotation.A implements I2 {}"
          )
        );
    }

    @SuppressWarnings("RedundantThrows")
    @Test
    void method() {
        rewriteRun(
          spec -> spec
            .recipe(Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")
              .doNext(new ChangeType("I1", "I2", false))
            )
            .parser(
              //language=java
              JavaParser.fromJavaVersion().dependsOn(
                javax,
                jakarta,
                "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
                "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"
              )
            ),
          //language=java
          java(
            """
              public class B {
                  public javax.xml.bind.annotation.A foo() throws javax.xml.bind.annotation.NewException { return null; }
              }
              """,
            """
              public class B {
                  public jakarta.xml.bind.annotation.A foo() throws jakarta.xml.bind.annotation.NewException { return null; }
              }
              """
          )
        );
    }

    @Test
    void methodInvocationTypeParametersAndWildcard() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              import java.util.List;
              public class B {
                 public <T extends javax.xml.bind.annotation.A> T generic(T n, List<? super javax.xml.bind.annotation.A> in) {
                      return n;
                 }
                 public void test() {
                     javax.xml.bind.annotation.A.stat();
                     this.<javax.xml.bind.annotation.A>generic(null, null);
                 }
              }
              """,
            """
              import java.util.List;
              public class B {
                 public <T extends jakarta.xml.bind.annotation.A> T generic(T n, List<? super jakarta.xml.bind.annotation.A> in) {
                      return n;
                 }
                 public void test() {
                     jakarta.xml.bind.annotation.A.stat();
                     this.<jakarta.xml.bind.annotation.A>generic(null, null);
                 }
              }
              """
          )
        );
    }

    @SuppressWarnings({"EmptyTryBlock", "CatchMayIgnoreException"})
    @Test
    void multiCatch() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              "package javax.xml.bind.annotation; public class NewException extends Throwable {}",
              "package jakarta.xml.bind.annotation; public class NewException extends Throwable {}"
            )
          ),
          java(
            """
              public class B {
                 public void test() {
                     try {}
                     catch(javax.xml.bind.annotation.NewException | RuntimeException e) {}
                 }
              }
              """,
            """
              public class B {
                 public void test() {
                     try {}
                     catch(jakarta.xml.bind.annotation.NewException | RuntimeException e) {}
                 }
              }
              """
          )
        );
    }

    @Test
    void multiVariable() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class B {
                 javax.xml.bind.annotation.A f1, f2;
              }
              """,
            """
              public class B {
                 jakarta.xml.bind.annotation.A f1, f2;
              }
              """
          )
        );
    }

    @Test
    void newClass() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class B {
                 javax.xml.bind.annotation.A a = new javax.xml.bind.annotation.A();
              }
              """,
            """
              public class B {
                 jakarta.xml.bind.annotation.A a = new jakarta.xml.bind.annotation.A();
              }
              """
          )
        );
    }

    @Test
    void parameterizedType() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              import java.util.Map;
              public class B {
                 Map<javax.xml.bind.annotation.A, javax.xml.bind.annotation.A> m;
              }
              """,
            """
              import java.util.Map;
              public class B {
                 Map<jakarta.xml.bind.annotation.A, jakarta.xml.bind.annotation.A> m;
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantCast")
    @Test
    void typeCast() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class B {
                 javax.xml.bind.annotation.A a = (javax.xml.bind.annotation.A) null;
              }
              """,
            """
              public class B {
                 jakarta.xml.bind.annotation.A a = (jakarta.xml.bind.annotation.A) null;
              }
              """
          )
        );
    }

    @Test
    void classReference() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class A {
                  Class<?> class_ = javax.xml.bind.annotation.A.class;
              }
              """,
            """
              public class A {
                  Class<?> class_ = jakarta.xml.bind.annotation.A.class;
              }
              """
          )
        );
    }

    @Test
    void methodSelect() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              public class B {
                 javax.xml.bind.annotation.A a = null;
                 public void test() { a.foo(); }
              }
              """,
            """
              public class B {
                 jakarta.xml.bind.annotation.A a = null;
                 public void test() { a.foo(); }
              }
              """
          )
        );
    }

    @Test
    void staticImport() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax, jakarta)),
          java(
            """
              import static javax.xml.bind.annotation.A.stat;
              public class B {
                  public void test() {
                      stat();
                  }
              }
              """,
            """
              import static jakarta.xml.bind.annotation.A.stat;
              public class B {
                  public void test() {
                      stat();
                  }
              }
              """
          )
        );
    }

    @Test
    void projectWithSpringBootStarterWeb() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javaxServlet)),
          mavenProject(
            "Sample",
            pomXml("""
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  	<modelVersion>4.0.0</modelVersion>
                  	<parent>
                  		<groupId>org.springframework.boot</groupId>
                  		<artifactId>spring-boot-starter-parent</artifactId>
                  		<version>2.7.6</version>
                  		<relativePath/> <!-- lookup parent from repository -->
                  	</parent>
                  	<groupId>com.example</groupId>
                  	<artifactId>demo</artifactId>
                  	<version>0.0.1-SNAPSHOT</version>
                  	<name>demo</name>
                  	<description>Demo project for Spring Boot</description>
                  	<properties>
                  		<java.version>17</java.version>
                  	</properties>
                  	<dependencies>
                  		<dependency>
                  			<groupId>org.springframework.boot</groupId>
                  			<artifactId>spring-boot-starter-web</artifactId>
                  		</dependency>
                  	</dependencies>

                  	<build>
                  		<plugins>
                  			<plugin>
                  				<groupId>org.springframework.boot</groupId>
                  				<artifactId>spring-boot-maven-plugin</artifactId>
                  			</plugin>
                  		</plugins>
                  	</build>

                  </project>
              """),
            srcMainJava(
              java("""
                import javax.servlet.A;
                public class TestApplication {
                }
            """, """
                import jakarta.servlet.A;
                public class TestApplication {
                }
            """)
            )
          )
        );
    }
}
