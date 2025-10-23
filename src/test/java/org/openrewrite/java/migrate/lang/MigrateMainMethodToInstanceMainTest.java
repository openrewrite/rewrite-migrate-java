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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@SuppressWarnings("ConfusingMainMethod")
class MigrateMainMethodToInstanceMainTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateMainMethodToInstanceMain())
          .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void migrateMainMethodWithUnusedArgs() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] args) {
                      System.out.println("Hello, World!");
                  }
              }
              """,
            """
              class Application {
                  void main() {
                      System.out.println("Hello, World!");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArgsWhenUsed() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] args) {
                      if (args.length > 0) {
                          System.out.println("Args provided: " + args[0]);
                      }
                  }
              }
              """,
            """
              class Application {
                  void main(String[] args) {
                      if (args.length > 0) {
                          System.out.println("Args provided: " + args[0]);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainArgsWhenUsedInMethodCall() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] args) {
                      processArgs(args);
                  }

                  private static void processArgs(String[] args) {
                      // Process arguments
                  }
              }
              """,
            """
              class Application {
                  void main(String[] args) {
                      processArgs(args);
                  }

                  private static void processArgs(String[] args) {
                      // Process arguments
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMainMethodWithEmptyBody() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] args) {
                  }
              }
              """,
            """
              class Application {
                  void main() {
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateNonMainMethod() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void notMain(String[] args) {
                      System.out.println("Not a main method");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainWithDifferentSignature() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static int main(String[] args) {
                      return 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigratePrivateMain() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  private static void main(String[] args) {
                      System.out.println("Private main");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateInstanceMain() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public void main(String[] args) {
                      System.out.println("Already instance main");
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMainWithComplexUnusedArgs() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] arguments) {
                      System.out.println("Starting application...");
                      new Application().run();
                  }

                  void run() {
                      System.out.println("Running...");
                  }
              }
              """,
            """
              class Application {
                  void main() {
                      System.out.println("Starting application...");
                      new Application().run();
                  }

                  void run() {
                      System.out.println("Running...");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainWithMultipleParameters() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String first, String[] args) {
                      System.out.println("Invalid main method");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainWithNoParameters() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main() {
                      System.out.println("No parameters");
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMainWithAnnotations() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  @SuppressWarnings("unused")
                  public static void main(String[] args) {
                      System.out.println("Hello!");
                  }
              }
              """,
            """
              class Application {
                  @SuppressWarnings("unused")
                  void main() {
                      System.out.println("Hello!");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainUsedAsMethodReference() {
        //language=java
        rewriteRun(
          java(
            """
              interface MainMethod {
                  void run(String[] args);
              }
              """
          ),
          java(
            """
              class Application {
                  public static void main(String[] args) {
                      System.out.println("Hello from main");
                  }
              }

              class Runner {
                  void executeMain() {
                      MainMethod foo = Application::main;
                      foo.run(null);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainWithNonDefaultConstructor() {
        //language=java
        rewriteRun(
          java(
            """
              class Application {
                  public static void main(String[] args) {
                      System.out.println("Hello!");
                  }

                  public Application(String config) {
                      // Non-default constructor
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMainInSpringBootApplication() {
        //language=java
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
              package org.springframework.boot.autoconfigure;
              public @interface SpringBootApplication {}
              """,
            """
              package org.springframework.boot;
              public class SpringApplication {
                  public static void run(Class<?> primarySource, String... args) {}
              }
              """
          )),
          java(
            """
              package com.example.demo;

              import org.springframework.boot.SpringApplication;
              import org.springframework.boot.autoconfigure.SpringBootApplication;

              @SpringBootApplication
              class DemoApplication {
                  public static void main(String[] args) {
                      SpringApplication.run(DemoApplication.class, args);
                  }
              }
              """
          )
        );
    }
}
