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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.JavaParser;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class GuavaEqualityCompatibilityTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.guava.PreferJavaUtilObjectsEquals")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"))
          .cycles(2).expectedCyclesThatMakeChanges(1);
    }

    @Test
    void nestedDepthTwoConvergesInOneCycle() {
        nested(2);
    }

    @Test
    void nestedDepthFiveConvergesInOneCycle() {
        nested(5);
    }

    private void nested(int depth) {
        String before = "left";
        String after = "left";
        for (int i = 0; i < depth; i++) {
            before = "equal(" + before + ", right)";
            after = "java.util.Objects.equals(" + after + ", right)";
        }
        rewriteRun(java(
          "import static com.google.common.base.Objects.equal;\n\n" +
          "class Nested { boolean compare(Object left, Object right) { return " + before + "; } }",
          "class Nested { boolean compare(Object left, Object right) { return " + after + "; } }"
        ));
    }

    @Test
    void legacyQualifiedKotlinControl() {
        rewriteRun(
          spec -> spec.recipes(
              new ChangeMethodName("com.google.common.base.Objects equal(Object, Object)", "equals", null, null),
              new ChangeMethodTargetToStatic("com.google.common.base.Objects equals(Object, Object)", "java.util.Objects", null, null))
            .parser(KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          kotlin(
            """
              import com.google.common.base.Objects

              fun same(left: Any?, right: Any?) = Objects.equal(left, right)
              """,
            """
              import java.util.Objects

              fun same(left: Any?, right: Any?) = Objects.equals(left, right)
              """
          )
        );
    }

    @Test
    void retainQualifiedKotlinMigration() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "guava")),
          kotlin(
            """
              import com.google.common.base.Objects

              fun same(left: Any?, right: Any?) = Objects.equal(left, right)
              """,
            """
              import java.util.Objects

              fun same(left: Any?, right: Any?) = Objects.equals(left, right)
              """
          )
        );
    }

    @Test
    void preserveUsedStaticImportWildcardAndComments() {
        rewriteRun(java(
          """
            // Collection API
            import java.util.*;

            import static com.google.common.base.Objects.equal;
            import static java.util.Objects.requireNonNull;

            /** Uses {@link List}. */
            class Imports {
                boolean compare(List<?> left, List<?> right) {
                    return equal(requireNonNull(left), right);
                }
            }
            """,
          """
            // Collection API
            import java.util.*;

            import static java.util.Objects.requireNonNull;

            /** Uses {@link List}. */
            class Imports {
                boolean compare(List<?> left, List<?> right) {
                    return java.util.Objects.equals(requireNonNull(left), right);
                }
            }
            """
        ));
    }

    @Test
    void preserveExistingObjectsImportForJavadoc() {
        rewriteRun(java(
          """
            import java.util.Objects;

            import static com.google.common.base.Objects.equal;

            /** Uses {@link Objects}. */
            class Imports {
                boolean compare(Object left, Object right) {
                    return equal(left, right);
                }
            }
            """,
          """
            import java.util.Objects;

            /** Uses {@link Objects}. */
            class Imports {
                boolean compare(Object left, Object right) {
                    return java.util.Objects.equals(left, right);
                }
            }
            """
        ));
    }
}
