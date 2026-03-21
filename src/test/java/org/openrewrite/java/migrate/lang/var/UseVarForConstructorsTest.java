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
package org.openrewrite.java.migrate.lang.var;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UseVarForConstructorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForConstructors())
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Test
    void replacePatterns() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.ByteArrayInputStream;
              import java.util.ArrayList;
              import java.util.HashMap;

              class Test<E, K, V> {
                  void test() {
                      // Basic constructor
                      StringBuilder sb = new StringBuilder();

                      // Constructor with arguments
                      StringBuilder sbWithArg = new StringBuilder("initial");

                      // Final modifier
                      final StringBuilder finalSb = new StringBuilder();

                      // Generics with concrete types
                      ArrayList<String> list = new ArrayList<>();

                      // Explicit type arguments on constructor (non-diamond)
                      ArrayList<String> explicitList = new ArrayList<String>();

                      // Nested generics with concrete types
                      HashMap<String, ArrayList<Integer>> map = new HashMap<>();

                      // Type variable in generic
                      ArrayList<E> typeVarList = new ArrayList<>();

                      // Multiple type variables
                      HashMap<K, V> typeVarMap = new HashMap<>();

                      // Nested type variables
                      HashMap<K, ArrayList<V>> nested = new HashMap<>();

                      // Inner class constructor
                      HashMap.SimpleEntry<String, Integer> entry = new HashMap.SimpleEntry<>("key", 1);

                      // In lambda
                      Runnable r = () -> {
                          ArrayList<String> lambdaList = new ArrayList<>();
                      };

                      // For-loop initializer
                      for (StringBuilder forSb = new StringBuilder(); forSb.length() < 10; forSb.append("x")) {
                      }

                      // Try-with-resources
                      try (ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0])) {
                      } catch (Exception e) {
                      }
                  }

                  // Instance initializer
                  {
                      StringBuilder initSb = new StringBuilder();
                  }

                  // Static initializer
                  static {
                      StringBuilder staticSb = new StringBuilder();
                  }
              }
              """,
            """
              import java.io.ByteArrayInputStream;
              import java.util.ArrayList;
              import java.util.HashMap;

              class Test<E, K, V> {
                  void test() {
                      // Basic constructor
                      var sb = new StringBuilder();

                      // Constructor with arguments
                      var sbWithArg = new StringBuilder("initial");

                      // Final modifier
                      final var finalSb = new StringBuilder();

                      // Generics with concrete types
                      var list = new ArrayList<String>();

                      // Explicit type arguments on constructor (non-diamond)
                      var explicitList = new ArrayList<String>();

                      // Nested generics with concrete types
                      var map = new HashMap<String, ArrayList<Integer>>();

                      // Type variable in generic
                      var typeVarList = new ArrayList<E>();

                      // Multiple type variables
                      var typeVarMap = new HashMap<K, V>();

                      // Nested type variables
                      var nested = new HashMap<K, ArrayList<V>>();

                      // Inner class constructor
                      var entry = new HashMap.SimpleEntry<String, Integer>("key", 1);

                      // In lambda
                      Runnable r = () -> {
                          var lambdaList = new ArrayList<String>();
                      };

                      // For-loop initializer
                      for (var forSb = new StringBuilder(); forSb.length() < 10; forSb.append("x")) {
                      }

                      // Try-with-resources
                      try (var bais = new ByteArrayInputStream(new byte[0])) {
                      } catch (Exception e) {
                      }
                  }

                  // Instance initializer
                  {
                      var initSb = new StringBuilder();
                  }

                  // Static initializer
                  static {
                      var staticSb = new StringBuilder();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceWhenJavaVersionBelow10() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(9))),
          //language=java
          java(
            """
              class Test {
                  void test() {
                      StringBuilder sb = new StringBuilder();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceInvalidPatterns() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  // Field declarations
                  private final ArrayList<String> privateField = new ArrayList<>();
                  protected final ArrayList<String> protectedField = new ArrayList<>();
                  public final ArrayList<String> publicField = new ArrayList<>();
                  final ArrayList<String> packageField = new ArrayList<>();
                  ArrayList<String> nonFinalField = new ArrayList<>();

                  void test() {
                      // Interface vs implementation
                      List<String> list = new ArrayList<>();

                      // Supertype
                      Object obj = new StringBuilder();

                      // Already using var
                      var existing = new ArrayList<>();

                      // No initializer
                      ArrayList<String> noInit;
                      noInit = new ArrayList<>();

                      // Not a constructor call
                      ArrayList<String> fromFactory = getList();

                      // Multiple variables
                      String a = "a", b = "b";

                      // Null initializer
                      StringBuilder nullInit = null;

                      // Anonymous inner class
                      ArrayList<String> anonList = new ArrayList<String>() {};
                      Runnable r = new Runnable() {
                          @Override
                          public void run() {}
                      };
                  }

                  ArrayList<String> getList() {
                      return new ArrayList<>();
                  }
              }
              """
          )
        );
    }
}
