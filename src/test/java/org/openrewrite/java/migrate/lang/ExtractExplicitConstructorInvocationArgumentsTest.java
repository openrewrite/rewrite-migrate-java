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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_25;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@EnabledForJreRange(min = JAVA_25)
class ExtractExplicitConstructorInvocationArgumentsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .allSources(src -> src.markers(javaVersion(25)))
          .recipe(new ExtractExplicitConstructorInvocationArguments());
    }

    @DocumentExample
    @Test
    void extractMethodInvocationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(Objects.requireNonNull(name));
                  }
              }
              """,
            """
              import java.util.Objects;

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      String name1 = Objects.requireNonNull(name);
                      super(name1);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractObjectCreationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              class Holder {
                  Holder(StringBuilder sb) {
                  }
              }

              class Child extends Holder {
                  Child(String value) {
                      super(new StringBuilder(value));
                  }
              }
              """,
            """
              class Holder {
                  Holder(StringBuilder sb) {
                  }
              }

              class Child extends Holder {
                  Child(String value) {
                      StringBuilder sb = new StringBuilder(value);
                      super(sb);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractPrimitiveReturningInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      int value1 = Integer.parseInt(value);
                      super(value1);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractSimpleIdentifier() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractLiteral() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      super("constant");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeImplicitSuper() {
        rewriteRun(
          //language=java
          java(
            """
              class Child {
                  Child(String name) {
                      System.out.println(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractMultipleArgumentsInOrder() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim(), value.strip());
                  }
              }
              """,
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      String a = value.trim();
                      String b = value.strip();
                      super(a, b);
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveTrivialArgumentsInlineWhenExtractingSiblings() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String first, int second, String third) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      super(name, Integer.parseInt(name), "literal");
                  }
              }
              """,
            """
              class Parent {
                  Parent(String first, int second, String third) {
                  }
              }

              class Child extends Parent {
                  Child(String name) {
                      int second = Integer.parseInt(name);
                      super(name, second, "literal");
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractWhenNoArgumentDoesWork() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String a, String b) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value, value);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractGenericParameterType() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Parent {
                  Parent(List<String> values) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      super(java.util.Collections.singletonList("a"));
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotClobberLocalClassConstructorInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Base {
                  Base(String s) {
                  }
              }

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim());
                      class Local extends Base {
                          Local() {
                              super("literal");
                          }
                      }
                  }
              }
              """,
            """
              class Base {
                  Base(String s) {
                  }
              }

              class Parent {
                  Parent(String name) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      String name = value.trim();
                      super(name);
                      class Local extends Base {
                          Local() {
                              super("literal");
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotExtractFromVarargsConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(String... values) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(value.trim(), value.strip());
                  }
              }
              """
          )
        );
    }

    @Test
    void insertDeclarationsAfterExistingStatementsBeforeSuper() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      System.out.println("before");
                      super(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      System.out.println("before");
                      int value1 = Integer.parseInt(value);
                      super(value1);
                  }
              }
              """
          )
        );
    }

    @Test
    void extractStaticFieldArgumentToPreserveOrder() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int a, int b) {
                  }
              }

              class Child extends Parent {
                  static final int CONST = 1;

                  Child(String value) {
                      super(CONST, Integer.parseInt(value));
                  }
              }
              """,
            """
              class Parent {
                  Parent(int a, int b) {
                  }
              }

              class Child extends Parent {
                  static final int CONST = 1;

                  Child(String value) {
                      int a = CONST;
                      int b = Integer.parseInt(value);
                      super(a, b);
                  }
              }
              """
          )
        );
    }

    @Test
    void avoidNameCollisionWithLaterDeclaredVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      super(Integer.parseInt(value));
                      int value1 = 5;
                  }
              }
              """,
            """
              class Parent {
                  Parent(int value) {
                  }
              }

              class Child extends Parent {
                  Child(String value) {
                      int value2 = Integer.parseInt(value);
                      super(value2);
                      int value1 = 5;
                  }
              }
              """
          )
        );
    }

    @Test
    void extractWithSourcePathParameterType() {
        rewriteRun(
          //language=java
          java(
            """
              package foo;
              public class Widget {
              }
              """
          ),
          //language=java
          java(
            """
              package foo;

              class Parent {
                  Parent(Widget w) {
                  }
              }

              class Child extends Parent {
                  Child(Widget w) {
                      super(identity(w));
                  }

                  static Widget identity(Widget w) {
                      return w;
                  }
              }
              """,
            """
              package foo;

              class Parent {
                  Parent(Widget w) {
                  }
              }

              class Child extends Parent {
                  Child(Widget w) {
                      Widget w1 = identity(w);
                      super(w1);
                  }

                  static Widget identity(Widget w) {
                      return w;
                  }
              }
              """,
            // Even with a context-free template, the rewritten reference must carry both its type and
            // its binding to the freshly created local; otherwise downstream type-aware recipes break.
            spec -> spec.afterRecipe(cu -> {
                var refs = new ArrayList<J.Identifier>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, Integer p) {
                        if ("super".equals(mi.getSimpleName())) {
                            for (Expression a : mi.getArguments()) {
                                if (a instanceof J.Identifier identifier) {
                                    refs.add(identifier);
                                }
                            }
                        }
                        return super.visitMethodInvocation(mi, p);
                    }
                }.visit(cu, 0);
                assertThat(refs).hasSize(1);
                assertThat(refs.getFirst().getType()).as("type").isNotNull();
                assertThat(refs.getFirst().getFieldType()).as("fieldType (variable binding)").isNotNull();
            })
          )
        );
    }

    @Test
    void extractWithSourcePathWideningSupertype() {
        rewriteRun(
          //language=java
          java(
            """
              package foo;
              public class Base {
              }
              """
          ),
          //language=java
          java(
            """
              package foo;
              public class Derived extends Base {
              }
              """
          ),
          //language=java
          java(
            """
              package foo;

              class Parent {
                  Parent(Base b) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      super(makeDerived());
                  }

                  static Derived makeDerived() {
                      return new Derived();
                  }
              }
              """,
            """
              package foo;

              class Parent {
                  Parent(Base b) {
                  }
              }

              class Child extends Parent {
                  Child() {
                      Base b = makeDerived();
                      super(b);
                  }

                  static Derived makeDerived() {
                      return new Derived();
                  }
              }
              """,
            // The extracted variable is declared with a widening supertype ('Base') that lives in a sibling
            // source and is referenced nowhere else in this unit. A context-free template still resolves it
            // via the shared type cache, so the declared type must be a real FQ type, not Unknown.
            spec -> spec.afterRecipe(cu -> {
                var decls = new ArrayList<J.VariableDeclarations>();
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, Integer p) {
                        if ("b".equals(vd.getVariables().getFirst().getSimpleName())) {
                            decls.add(vd);
                        }
                        return super.visitVariableDeclarations(vd, p);
                    }
                }.visit(cu, 0);
                assertThat(decls).hasSize(1);
                assertThat(decls.getFirst().getTypeExpression().getType()).as("declared type 'Base'").isNotNull();
                assertThat(TypeUtils.asFullyQualified(decls.getFirst().getTypeExpression().getType()))
                  .as("declared type is a resolved FQ type, not Unknown").isNotNull();
            })
          )
        );
    }

    @Test
    void extractThisDelegationArgument() {
        rewriteRun(
          //language=java
          java(
            """
              class Child {
                  Child(int value) {
                  }

                  Child(String value) {
                      this(Integer.parseInt(value));
                  }
              }
              """,
            """
              class Child {
                  Child(int value) {
                  }

                  Child(String value) {
                      int value1 = Integer.parseInt(value);
                      this(value1);
                  }
              }
              """
          )
        );
    }
}
