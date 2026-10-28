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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.kotlin.Assertions.kotlin;

class RenameUnderscoreIdentifierTest implements RewriteTest {

    /// Setup recipe that renames `UNDERSCORE` to `_` in the LST,
    /// simulating Java 8 source code that uses `_` as an identifier.
    /// This is necessary because the Java 9+ parser cannot parse `_` as a regular identifier.
    private static Recipe renameToUnderscore() {
        return new Recipe() {
            @Override
            public String getDisplayName() {
                return "Rename UNDERSCORE to _";
            }

            @Override
            public String getDescription() {
                return "Test setup recipe.";
            }

            @Override
            public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new RenameUnderscoreIdentifier.RenameIdentifierVisitor("UNDERSCORE", "_");
            }
        };
    }

    /// Setup recipe that renames the *type* `UNDERSCORE` to `_`, declaration, constructor and every
    /// bound reference alike, so that the resulting LST is shaped like Java 8 source declaring a
    /// type named `_`. `renameToUnderscore()` only rewrites declaration names, which is not enough
    /// to exercise the type reference paths.
    private static Recipe renameTypeToUnderscore() {
        return new ChangeType("UNDERSCORE", "_", false);
    }

    /// Runs the rename in a single pass over a clean parse, `UNDERSCORE` standing in for `_`.
    /// The two-pass setup above rewrites each declaration's method type but not the method list of
    /// the declaring `JavaType.Class`, so on the second pass `TypeUtils.findOverriddenMethod` can
    /// no longer link an override to the method it overrides. Real Java 8 source arrives as a
    /// single clean parse, which this setup preserves, so the override-sensitive tests use it.
    private static Recipe renameUnderscoreWordIdentifier() {
        return new Recipe() {
            @Override
            public String getDisplayName() {
                return "Rename UNDERSCORE to __";
            }

            @Override
            public String getDescription() {
                return "Test setup recipe.";
            }

            @Override
            public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new RenameUnderscoreIdentifier.RenameIdentifierVisitor("UNDERSCORE", "__");
            }
        };
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(renameToUnderscore(), new RenameUnderscoreIdentifier())
          .allSources(s -> s.markers(javaVersion(8)));
    }

    @DocumentExample
    @Test
    void localVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int test() {
                      int UNDERSCORE = 1;
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class Test {
                  int test() {
                      int __ = 1;
                      return __;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void foo(int UNDERSCORE) {
                      System.out.println(UNDERSCORE);
                  }
              }
              """,
            """
              class Test {
                  void foo(int __) {
                      System.out.println(__);
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceField() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  private int UNDERSCORE;
                  void set(int value) {
                      this.UNDERSCORE = value;
                  }
                  int get() {
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class Test {
                  private int __;
                  void set(int value) {
                      this.__ = value;
                  }
                  int get() {
                      return __;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodName() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void UNDERSCORE() {
                  }
                  void caller() {
                      UNDERSCORE();
                  }
              }
              """,
            """
              class Test {
                  void __() {
                  }
                  void caller() {
                      __();
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnJava9() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(9))),
          //language=java
          java(
            """
              class Test {
                  int _foo = 1;
              }
              """
          )
        );
    }

    @Test
    void noChangeForUnrelatedIdentifiers() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier()),
          //language=java
          java(
            """
              class Test {
                  int _foo = 1;
                  int foo_ = 2;
                  int my_value = 3;
              }
              """
          )
        );
    }

    @Test
    void noChangeForDoubleUnderscore() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier()),
          //language=java
          java(
            """
              class Test {
                  int __ = 1;
              }
              """
          )
        );
    }

    @Test
    void catchParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test() {
                      try {
                          Integer.parseInt("x");
                      } catch (NumberFormatException UNDERSCORE) {
                          System.out.println("error");
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      try {
                          Integer.parseInt("x");
                      } catch (NumberFormatException __) {
                          System.out.println("error");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void test() {
                      Stream.of("a", "b")
                          .collect(Collectors.toMap(String::toUpperCase, UNDERSCORE -> "val"));
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void test() {
                      Stream.of("a", "b")
                          .collect(Collectors.toMap(String::toUpperCase, __ -> "val"));
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinFileNotChanged() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=kotlin
          kotlin(
            """
              class Test {
                  fun test() {
                      val pairs = listOf(1 to "a", 2 to "b")
                      pairs.forEach { _, _ -> println("ignored") }
                  }
              }
              """
          )
        );
    }

    @Test
    void parameterCollision() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int sum(int UNDERSCORE, int __) {
                      return UNDERSCORE + __;
                  }
              }
              """,
            """
              class Test {
                  int sum(int ___, int __) {
                      return ___ + __;
                  }
              }
              """
          )
        );
    }

    @Test
    void localVariableCollision() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int sum() {
                      int UNDERSCORE = 1;
                      int __ = 2;
                      return UNDERSCORE + __;
                  }
              }
              """,
            """
              class Test {
                  int sum() {
                      int ___ = 1;
                      int __ = 2;
                      return ___ + __;
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldCollision() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int UNDERSCORE = 1;
                  int __ = 2;

                  int sum() {
                      return UNDERSCORE + __;
                  }
              }
              """,
            """
              class Test {
                  int ___ = 1;
                  int __ = 2;

                  int sum() {
                      return ___ + __;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodCollision() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int UNDERSCORE() {
                      return 1;
                  }

                  int __() {
                      return 2;
                  }

                  int sum() {
                      return UNDERSCORE() + __();
                  }
              }
              """,
            """
              class Test {
                  int ___() {
                      return 1;
                  }

                  int __() {
                      return 2;
                  }

                  int sum() {
                      return ___() + __();
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedClassCollision() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  class UNDERSCORE {
                  }

                  class __ {
                  }
              }
              """,
            """
              class Test {
                  class ___ {
                  }

                  class __ {
                  }
              }
              """
          )
        );
    }

    @Test
    void classDeclarationRenamesBoundTypeAndNewClassReferences() {
        rewriteRun(
          spec -> spec.recipes(renameTypeToUnderscore(), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              class UNDERSCORE {
                  UNDERSCORE() {
                  }

                  UNDERSCORE field;

                  UNDERSCORE copy(UNDERSCORE input) {
                      return new UNDERSCORE();
                  }
              }
              """,
            """
              class __ {
                  __() {
                  }

                  __ field;

                  __ copy(__ input) {
                      return new __();
                  }
              }
              """
          )
        );
    }

    @Test
    void classFileRenamedAlongsideTheClass() {
        rewriteRun(
          spec -> spec.recipes(renameTypeToUnderscore(), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              class UNDERSCORE {
              }
              """,
            """
              class __ {
              }
              """,
            spec -> spec.path("_.java")
              .afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Paths.get("__.java")))
          )
        );
    }

    @Test
    void classFileRenameDoesNotOverwriteAnUnrelatedSourceFile() {
        rewriteRun(
          spec -> spec.recipes(renameTypeToUnderscore(), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              class UNDERSCORE {
              }
              """,
            """
              class ___ {
              }
              """,
            spec -> spec.path("_.java")
              .afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Paths.get("___.java")))
          ),
          //language=java
          java(
            """
              class Helper {
                  int keepMe = 1;
              }
              """,
            spec -> spec.path("__.java")
          ),
          //language=java
          java(
            """
              class User {
                  int use() {
                      return new Helper().keepMe;
                  }
              }
              """
          )
        );
    }

    @Test
    void classFileRenameDoesNotOverwriteACollidingClassFile() {
        rewriteRun(
          spec -> spec.recipes(renameTypeToUnderscore(), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              class UNDERSCORE {
              }
              """,
            """
              class ___ {
              }
              """,
            spec -> spec.path("_.java")
              .afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Paths.get("___.java")))
          ),
          //language=java
          java(
            """
              class __ {
                  int keepMe = 1;
              }
              """,
            spec -> spec.path("__.java")
          )
        );
    }

    @Test
    void classFileRenameAdvancesPastEveryOccupiedFile() {
        rewriteRun(
          spec -> spec.recipes(renameTypeToUnderscore(), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              class UNDERSCORE {
              }
              """,
            """
              class ____ {
              }
              """,
            spec -> spec.path("_.java")
              .afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Paths.get("____.java")))
          ),
          //language=java
          java(
            """
              class __ {
                  int keepMe = 1;
              }
              """,
            spec -> spec.path("__.java")
          ),
          //language=java
          java(
            """
              class ___ {
                  int keepMeToo = 1;
              }
              """,
            spec -> spec.path("___.java")
          )
        );
    }

    @Test
    void occupiedFileInAnotherPackageDoesNotAffectTheChosenName() {
        rewriteRun(
          spec -> spec.recipes(new ChangeType("a.UNDERSCORE", "a._", false), new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(8))),
          //language=java
          java(
            """
              package a;

              class UNDERSCORE {
              }
              """,
            """
              package a;

              class __ {
              }
              """,
            spec -> spec.path("a/_.java")
              .afterRecipe(cu -> assertThat(cu.getSourcePath()).isEqualTo(Paths.get("a/__.java")))
          ),
          //language=java
          java(
            """
              package b;

              class __ {
                  int keepMe = 1;
              }
              """,
            spec -> spec.path("b/__.java")
          )
        );
    }

    @Test
    void nestedTypeRenamedInEveryReferencePosition() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static class UNDERSCORE {
                  }

                  Object use() {
                      UNDERSCORE value = new UNDERSCORE();
                      Class<?> literal = UNDERSCORE.class;
                      Object o = value;
                      if (o instanceof UNDERSCORE) {
                          return (UNDERSCORE) o;
                      }
                      return literal;
                  }
              }
              """,
            """
              class Test {
                  static class __ {
                  }

                  Object use() {
                      __ value = new __();
                      Class<?> literal = __.class;
                      Object o = value;
                      if (o instanceof __) {
                          return (__) o;
                      }
                      return literal;
                  }
              }
              """
          )
        );
    }

    @Test
    void selectionAdvancesPastEveryTakenUnderscoreName() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int sum(int UNDERSCORE, int __, int ___) {
                      return UNDERSCORE + __ + ___;
                  }
              }
              """,
            """
              class Test {
                  int sum(int ____, int __, int ___) {
                      return ____ + __ + ___;
                  }
              }
              """
          )
        );
    }

    @Test
    void existingDoubleUnderscoreDeclarationIsNeverRenamed() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int a() {
                      int UNDERSCORE = 1;
                      return UNDERSCORE;
                  }

                  int b() {
                      int __ = 2;
                      return __;
                  }
              }
              """,
            """
              class Test {
                  int a() {
                      int ___ = 1;
                      return ___;
                  }

                  int b() {
                      int __ = 2;
                      return __;
                  }
              }
              """
          )
        );
    }

    @Test
    void overloadsAndMethodReferencesRenameTogether() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.IntSupplier;

              class Test {
                  int UNDERSCORE() {
                      return 1;
                  }

                  int UNDERSCORE(int i) {
                      return i;
                  }

                  int __() {
                      return 2;
                  }

                  IntSupplier supplier() {
                      return this::UNDERSCORE;
                  }

                  int sum() {
                      return UNDERSCORE() + UNDERSCORE(1) + __();
                  }
              }
              """,
            """
              import java.util.function.IntSupplier;

              class Test {
                  int ___() {
                      return 1;
                  }

                  int ___(int i) {
                      return i;
                  }

                  int __() {
                      return 2;
                  }

                  IntSupplier supplier() {
                      return this::___;
                  }

                  int sum() {
                      return ___() + ___(1) + __();
                  }
              }
              """
          )
        );
    }

    @Test
    void localVariableShadowingFieldKeepsBindings() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int UNDERSCORE = 1;

                  int test() {
                      int UNDERSCORE = 2;
                      return UNDERSCORE + this.UNDERSCORE;
                  }
              }
              """,
            """
              class Test {
                  int __ = 1;

                  int test() {
                      int __ = 2;
                      return __ + this.__;
                  }
              }
              """
          )
        );
    }

    @Test
    void inheritedMemberNameIsNotShadowed() {
        rewriteRun(
          //language=java
          java(
            """
              class Base {
                  int __ = 1;
              }
              """
          ),
          //language=java
          java(
            """
              class Test extends Base {
                  int UNDERSCORE = 2;

                  int sum() {
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class Test extends Base {
                  int ___ = 2;

                  int sum() {
                      return ___;
                  }
              }
              """
          )
        );
    }

    @Test
    void namesInUseAreComputedPerSourceFile() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  int a() {
                      int UNDERSCORE = 1;
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class A {
                  int a() {
                      int __ = 1;
                      return __;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class B {
                  int b() {
                      int UNDERSCORE = 1;
                      int __ = 2;
                      return UNDERSCORE + __;
                  }
              }
              """,
            """
              class B {
                  int b() {
                      int ___ = 1;
                      int __ = 2;
                      return ___ + __;
                  }
              }
              """
          )
        );
    }

    @Test
    void callersInAnotherSourceFileFollowTheDeclaration() {
        rewriteRun(
          //language=java
          java(
            """
              class Lib {
                  int UNDERSCORE() {
                      return 1;
                  }

                  int __() {
                      return 2;
                  }
              }
              """,
            """
              class Lib {
                  int ___() {
                      return 1;
                  }

                  int __() {
                      return 2;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Caller {
                  int call(Lib lib) {
                      return lib.UNDERSCORE() + lib.__();
                  }
              }
              """,
            """
              class Caller {
                  int call(Lib lib) {
                      return lib.___() + lib.__();
                  }
              }
              """
          )
        );
    }

    @Test
    void overriddenMethodAndOverrideAgreeOnOneName() {
        rewriteRun(
          spec -> spec.recipes(renameUnderscoreWordIdentifier()),
          //language=java
          java(
            """
              class A {
                  void UNDERSCORE() {
                  }
              }

              class B extends A {
                  @Override
                  void UNDERSCORE() {
                  }

                  void __(int i) {
                  }
              }
              """,
            """
              class A {
                  void __() {
                  }
              }

              class B extends A {
                  @Override
                  void __() {
                  }

                  void __(int i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void interfaceMethodAndImplementationAgreeOnOneName() {
        rewriteRun(
          spec -> spec.recipes(renameUnderscoreWordIdentifier()),
          //language=java
          java(
            """
              interface I {
                  void UNDERSCORE();
              }
              """,
            """
              interface I {
                  void __();
              }
              """
          ),
          //language=java
          java(
            """
              class Impl implements I {
                  @Override
                  public void UNDERSCORE() {
                  }

                  void __(int i) {
                  }
              }
              """,
            """
              class Impl implements I {
                  @Override
                  public void __() {
                  }

                  void __(int i) {
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldNamedDoubleUnderscoreDoesNotBlockAMethodRename() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void UNDERSCORE() {
                  }
              }

              class B extends A {
                  @Override
                  void UNDERSCORE() {
                  }

                  int __ = 1;
              }
              """,
            """
              class A {
                  void __() {
                  }
              }

              class B extends A {
                  @Override
                  void __() {
                  }

                  int __ = 1;
              }
              """
          )
        );
    }

    @Test
    void overrideKeepsDynamicDispatchAcrossSourceFiles() {
        rewriteRun(
          spec -> spec.recipes(renameUnderscoreWordIdentifier()),
          //language=java
          java(
            """
              class A {
                  int UNDERSCORE() {
                      return 1;
                  }
              }
              """,
            """
              class A {
                  int __() {
                      return 1;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class B extends A {
                  @Override
                  int UNDERSCORE() {
                      return 2;
                  }

                  int __(int i) {
                      return i;
                  }
              }
              """,
            """
              class B extends A {
                  @Override
                  int __() {
                      return 2;
                  }

                  int __(int i) {
                      return i;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class Main {
                  int run() {
                      A a = new B();
                      return a.UNDERSCORE();
                  }
              }
              """,
            """
              class Main {
                  int run() {
                      A a = new B();
                      return a.__();
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyRenamedSourceIsLeftAlone() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier()),
          //language=java
          java(
            """
              class Test {
                  int sum(int ___, int __) {
                      return ___ + __;
                  }
              }
              """
          )
        );
    }

    @Test
    void forEachLoopVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  int count(List<String> items) {
                      int total = 0;
                      for (String UNDERSCORE : items) {
                          total++;
                      }
                      return total;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int count(List<String> items) {
                      int total = 0;
                      for (String __ : items) {
                          total++;
                      }
                      return total;
                  }
              }
              """
          )
        );
    }
}
