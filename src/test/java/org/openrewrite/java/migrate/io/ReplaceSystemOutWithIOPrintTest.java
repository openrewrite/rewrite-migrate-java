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
package org.openrewrite.java.migrate.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_25;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@EnabledForJreRange(min = JAVA_25)
class ReplaceSystemOutWithIOPrintTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceSystemOutWithIOPrint())
          .parser(JavaParser.fromJavaVersion())
          .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void replaceSystemOutPrint() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.print("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.print("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeBeforeJava25() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.print("Hello");
                  }
              }
              """,
            spec -> spec.markers(javaVersion(21))
          )
        );
    }

    @Test
    void replaceSystemOutPrintln() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.println("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainBoundedGenericArguments() {
        rewriteRun(
          java(
            """
              interface TreeNode {}
              interface Parser {
                  <T extends TreeNode> T readValueAsTree();
              }
              class Example {
                  void test(Parser parser) {
                      System.out.print(parser.readValueAsTree());
                      System.out.println(parser.readValueAsTree());
                  }
              }
              """
          )
        );
    }

    @Test
    void retainGenericArgumentsWhileMigratingOtherCalls() {
        rewriteRun(
          java(
            """
              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test() {
                      System.out.println(value());
                      System.out.println("plain");
                  }
              }
              """,
            """
              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test() {
                      System.out.println(value());
                      IO.println("plain");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainGenericConditionalResults() {
        rewriteRun(
          java(
            """
              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test(boolean choose) {
                      System.out.print(choose ? value() : "fallback");
                      System.out.println(choose ? value() : "fallback");
                  }
              }
              """
          )
        );
    }

    @Test
    void retainGenericSwitchResults() {
        rewriteRun(
          java(
            """
              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test(int selector) {
                      System.out.println(switch (selector) {
                          case 0 -> value();
                          default -> "fallback";
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void retainParenthesizedGenericArguments() {
        rewriteRun(
          java(
            """
              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test() {
                      System.out.println((value()));
                  }
              }
              """
          )
        );
    }

    @Test
    void retainStaticImportWhenGenericCallStillUsesIt() {
        rewriteRun(
          java(
            """
              import static java.lang.System.out;

              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test() {
                      out.println(value());
                      out.println("plain");
                  }
              }
              """,
            """
              import static java.lang.System.out;

              class Example {
                  <T extends CharSequence> T value() { return null; }

                  void test() {
                      out.println(value());
                      IO.println("plain");
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateExplicitCastsThatSupplyTheirOwnContext() {
        rewriteRun(
          java(
            """
              interface TreeNode {}
              interface Parser { <T extends TreeNode> T readValueAsTree(); }

              class Example {
                  void test(Parser parser) {
                      System.out.print((Object) parser.readValueAsTree());
                      System.out.println((String) (Object) parser.readValueAsTree());
                  }
              }
              """,
            """
              interface TreeNode {}
              interface Parser { <T extends TreeNode> T readValueAsTree(); }

              class Example {
                  void test(Parser parser) {
                      IO.print((Object) parser.readValueAsTree());
                      IO.println((String) (Object) parser.readValueAsTree());
                  }
              }
              """
          )
        );
    }

    static Stream<JavaType> missingArgumentTypes() {
        return Stream.of(null, JavaType.Unknown.getInstance());
    }

    @ParameterizedTest
    @MethodSource("missingArgumentTypes")
    void retainArgumentWithMissingTypeWhileMigratingKnownCall(JavaType missingType) {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { throw new AssertionError(t); });
        J.CompilationUnit parsed = (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(ctx,
          """
            class Example {
                String value() { return null; }
                void test() {
                    System.out.println(value());
                    System.out.println("plain");
                }
            }
            """)
          .findFirst().orElseThrow();
        parsed = parsed.withMarkers(Markers.EMPTY.add(javaVersion(25)));
        // Only these standalone contracts remove attribution. Normal RewriteTest
        // fixtures retain their standard validation and Java 25 source marker.
        J.CompilationUnit unattributed = (J.CompilationUnit) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                return "value".equals(m.getSimpleName()) ?
                  m.withMethodType(m.getMethodType().withReturnType(missingType)) : m;
            }
        }.visit(parsed, ctx);
        J.CompilationUnit result = (J.CompilationUnit) new ReplaceSystemOutWithIOPrint()
          .getVisitor().visit(unattributed, ctx);
        assertThat(result).isNotNull();
        assertThat(result.printAll()).isEqualTo(unattributed.printAll()
          .replace("System.out.println(\"plain\")", "IO.println(\"plain\")"));
    }

    @Test
    void retainGenericFieldLoads() {
        rewriteRun(
          java(
            """
              class Example {
                  static class Box<T> { T value; }

                  void test(Box<String> box) {
                      System.out.print(box.value);
                      System.out.println(box.value);
                      System.out.println("plain");
                  }
              }
              """,
            """
              class Example {
                  static class Box<T> { T value; }

                  void test(Box<String> box) {
                      System.out.print(box.value);
                      System.out.println(box.value);
                      IO.println("plain");
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateExplicitGenericFieldCastsAndOrdinaryFields() {
        rewriteRun(
          java(
            """
              class Example {
                  static class Box<T> { T value; String plain; }

                  void test(Box<String> box) {
                      System.out.print((Object) box.value);
                      System.out.println((String) box.value);
                      System.out.println(box.plain);
                  }
              }
              """,
            """
              class Example {
                  static class Box<T> { T value; String plain; }

                  void test(Box<String> box) {
                      IO.print((Object) box.value);
                      IO.println((String) box.value);
                      IO.println(box.plain);
                  }
              }
              """
          )
        );
    }

    @Test
    void retainFieldLoadWithMissingDeclarationWhileMigratingQualifiedCalls() {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { throw new AssertionError(t); });
        J.CompilationUnit parsed = (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(ctx,
          """
            class Example {
                static class Box<T> { T value; }
                void test(Box<String> box) {
                    System.out.print(box.value);
                    System.out.println(box.value);
                    System.out.println(java.lang.String.valueOf(123));
                    System.out.println(String.class.getSimpleName());
                }
            }
            """).findFirst().orElseThrow();
        parsed = parsed.withMarkers(Markers.EMPTY.add(javaVersion(25)));
        J.CompilationUnit unattributed = (J.CompilationUnit) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess field, ExecutionContext executionContext) {
                J.FieldAccess f = super.visitFieldAccess(field, executionContext);
                return "value".equals(f.getSimpleName()) ?
                  f.withName(f.getName().withFieldType(null)) : f;
            }
        }.visit(parsed, ctx);
        J.CompilationUnit result = (J.CompilationUnit) new ReplaceSystemOutWithIOPrint()
          .getVisitor().visit(unattributed, ctx);
        assertThat(result).isNotNull();
        assertThat(result.printAll()).isEqualTo(unattributed.printAll()
          .replace("System.out.println(java.lang.String.valueOf(123))", "IO.println(java.lang.String.valueOf(123))")
          .replace("System.out.println(String.class.getSimpleName())", "IO.println(String.class.getSimpleName())"));
    }

    @Test
    void retainGenericReturnWhenFormalNamesAreUnavailable() {
        ExecutionContext ctx = new InMemoryExecutionContext(t -> { throw new AssertionError(t); });
        J.CompilationUnit parsed = (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(ctx,
          """
            class Example {
                <T extends CharSequence> T value() { return null; }
                void test() {
                    System.out.println(value());
                    System.out.println("plain");
                }
            }
            """)
          .findFirst().orElseThrow();
        parsed = parsed.withMarkers(Markers.EMPTY.add(javaVersion(25)));
        J.CompilationUnit unattributed = (J.CompilationUnit) new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);
                if (!"value".equals(m.getSimpleName())) {
                    return m;
                }
                JavaType.Class owner = (JavaType.Class) m.getMethodType().getDeclaringType();
                JavaType.Class withoutFormalNames = owner.withMethods(owner.getMethods().stream()
                  .map(declaration -> "value".equals(declaration.getName()) ?
                    declaration.withDeclaredFormalTypeNames(new String[0]) : declaration)
                  .collect(Collectors.toList()));
                return m.withMethodType(m.getMethodType().withDeclaringType(withoutFormalNames));
            }
        }.visit(parsed, ctx);
        J.CompilationUnit result = (J.CompilationUnit) new ReplaceSystemOutWithIOPrint()
          .getVisitor().visit(unattributed, ctx);
        assertThat(result).isNotNull();
        assertThat(result.printAll()).isEqualTo(unattributed.printAll()
          .replace("System.out.println(\"plain\")", "IO.println(\"plain\")"));
    }

    @Test
    void replaceSystemOutPrintlnWithStaticImport() {
        rewriteRun(
          java(
            """
              import static java.lang.System.out;

              class Example {
                  void test() {
                      out.println("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintWithVariable() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      String message = "Hello World";
                      System.out.print(message);
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      String message = "Hello World";
                      IO.print(message);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintlnEmpty() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.println();
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMultipleSystemOutCalls() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.print("Hello");
                      System.out.println(" World");
                      System.out.print(42);
                      System.out.println();
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.print("Hello");
                      IO.println(" World");
                      IO.print(42);
                      IO.println();
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesPrintWithComplexExpressions() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      String name = "John";
                      int age = 30;
                      System.out.print("Name: " + name + ", Age: " + age);
                      System.out.println(String.format("Formatted: %s is %d years old", name, age));
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      String name = "John";
                      int age = 30;
                      IO.print("Name: " + name + ", Age: " + age);
                      IO.println(String.format("Formatted: %s is %d years old", name, age));
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceSystemErrCalls() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.err.print("Error message");
                      System.err.println("Error message");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceOtherPrintStreams() {
        rewriteRun(
          java(
            """
              import java.io.PrintStream;

              class Example {
                  void test() {
                      PrintStream ps = new PrintStream(System.out);
                      ps.print("Should not change");
                      ps.println("Should not change");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotAddImportForRegularClassWithPackage() {
        rewriteRun(
          java(
            """
              package com.helloworld;

              public class Main {
                  public void greet() {
                      System.out.println("hello");
                  }
              }
              """,
            """
              package com.helloworld;

              public class Main {
                  public void greet() {
                      IO.println("hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplacePrintf() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.printf("Hello%n");
                  }
              }
              """
          )
        );
    }
}
