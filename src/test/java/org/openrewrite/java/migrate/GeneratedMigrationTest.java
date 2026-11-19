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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Generated;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class GeneratedMigrationTest implements RewriteTest {

    @ParameterizedTest
    @ValueSource(strings = {"src-gen/Test.java", "xtend-gen/Test.java"})
    void keepsOptionalJava11EditsOutOfGeneratedDirectories(String path) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.Java8toJava11"),
          version(java(
            """
              import java.io.InputStream;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Paths;
              import java.util.Optional;
              import java.util.function.Predicate;
              import java.util.stream.Stream;

              class Test {
                  void resources() throws Exception {
                      InputStream in = Files.newInputStream(Paths.get("input"));
                      try {
                          in.read();
                      } finally {
                          in.close();
                      }
                  }

                  String read() throws Exception {
                      return Files.readString(Paths.get("input"), StandardCharsets.UTF_8);
                  }

                  boolean empty(Optional<String> value) {
                      return !value.isPresent();
                  }

                  boolean present(Optional<String> value) {
                      return !value.isEmpty();
                  }

                  Stream<String> values(Stream<Optional<String>> values) {
                      return values.filter(Optional::isPresent).map(Optional::get);
                  }

                  Stream<String> filter(Stream<String> values) {
                      return values.filter(((Predicate<String>) String::isEmpty).negate());
                  }
              }
              """, spec -> spec.path(path)), 11)
        );
    }

    @Test
    void keepsIoCallsAndMigratesUnsupportedSecurityManagerInAntlrOutput() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          version(java(
            """
              // Generated from Test.g4 by ANTLR 4.13.2
              class Test {
                  SecurityManager securityManager() {
                      return System.getSecurityManager();
                  }

                  void print() {
                      System.out.println("generated");
                  }
              }
              """,
            """
              // Generated from Test.g4 by ANTLR 4.13.2
              class Test {
                  SecurityManager securityManager() {
                      return null;
                  }

                  void print() {
                      System.out.println("generated");
                  }
              }
              """), 25),
          version(java(
            """
              class Handwritten {
                  void print() {
                      System.out.println("handwritten");
                  }
              }
              """,
            """
              class Handwritten {
                  void print() {
                      IO.println("handwritten");
                  }
              }
              """), 25)
        );
    }

    @Test
    void keepsOptionalEditsOutOfFileWithGeneratedMemberMarker() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava21"),
          version(java(
            """
              import java.util.Locale;

              class Test {
                  int generated;

                  Locale locale() {
                      return new Locale("en", "US");
                  }
              }
              """, source -> source.mapBeforeRecipe(cu -> (J.CompilationUnit) new JavaIsoVisitor<Void>() {
                  @Override
                  public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variables, Void unused) {
                      return variables.withMarkers(variables.getMarkers().add(new Generated(randomId())));
                  }
              }.visitNonNull(cu, null))), 21)
        );
    }
}
