/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("removal")
class MigrateDeprecatedBoxedPrimitiveConstructorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateDeprecatedBoxedPrimitiveConstructorsRecipes());
    }

    @Test
    void booleanConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Boolean b1 = new Boolean(true);
                  Boolean b2 = new Boolean("true");
                  Boolean b3 = true; // Negative test, should not be rewritten
                  Boolean b4 = Boolean.parseBoolean("true"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Boolean b1 = Boolean.valueOf(true);
                  Boolean b2 = Boolean.valueOf("true");
                  Boolean b3 = true; // Negative test, should not be rewritten
                  Boolean b4 = Boolean.parseBoolean("true"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void byteConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Byte b1 = new Byte((byte) 42);
                  Byte b2 = new Byte("42");
                  Byte b3 = 84; // Negative test, should not be rewritten
                  Byte b4 = Byte.parseByte("84"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Byte b1 = Byte.valueOf((byte) 42);
                  Byte b2 = Byte.valueOf("42");
                  Byte b3 = 84; // Negative test, should not be rewritten
                  Byte b4 = Byte.parseByte("84"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void charConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Character c1 = new Character('a');
                  Character c2 = 'a'; // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Character c1 = Character.valueOf('a');
                  Character c2 = 'a'; // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void doubleConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Double d1 = new Double(3.14);
                  Double d2 = new Double("3.14");
                  Double d3 = 3.14; // Negative test, should not be rewritten
                  Double d4 = Double.parseDouble("3.14"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Double d1 = Double.valueOf(3.14);
                  Double d2 = Double.valueOf("3.14");
                  Double d3 = 3.14; // Negative test, should not be rewritten
                  Double d4 = Double.parseDouble("3.14"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void floatConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Float f1 = new Float(Double.valueOf(12.3));
                  Float f2 = new Float(42.0f);
                  Float f3 = new Float("3.14");
                  Float f4 = 3.14f; // Negative test, should not be rewritten
                  Float f5 = Float.parseFloat("3.14"); // Negative test, should not be rewritten
                  // This recipe doesn't handle new Float(double)
                  Float f6 = new Float(45.6);
              }
              """,
            """
              class Test {
                  Float f1 = Float.valueOf(Double.valueOf(12.3).floatValue());
                  Float f2 = Float.valueOf(42.0f);
                  Float f3 = Float.valueOf("3.14");
                  Float f4 = 3.14f; // Negative test, should not be rewritten
                  Float f5 = Float.parseFloat("3.14"); // Negative test, should not be rewritten
                  // This recipe doesn't handle new Float(double)
                  Float f6 = new Float(45.6);
              }
              """
          )
        );
    }

    @Test
    void intConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Integer i1 = new Integer(42);
                  Integer i2 = new Integer("42");
                  Integer i3 = 42; // Negative test, should not be rewritten
                  Integer i4 = Integer.parseInt("42"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Integer i1 = Integer.valueOf(42);
                  Integer i2 = Integer.valueOf("42");
                  Integer i3 = 42; // Negative test, should not be rewritten
                  Integer i4 = Integer.parseInt("42"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void longConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Long l1 = new Long(42L);
                  Long l2 = new Long("42");
                  Long l3 = 84L; // Negative test, should not be rewritten
                  Long l4 = Long.parseLong("84"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Long l1 = Long.valueOf(42L);
                  Long l2 = Long.valueOf("42");
                  Long l3 = 84L; // Negative test, should not be rewritten
                  Long l4 = Long.parseLong("84"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

    @Test
    void shortConstructors() {
        //language=java
        rewriteRun(
          java(
            """
              class Test {
                  Short s1 = new Short((short) 42);
                  Short s2 = new Short("42");
                  Short s3 = 84; // Negative test, should not be rewritten
                  Short s4 = Short.parseShort("84"); // Negative test, should not be rewritten
              }
              """,
            """
              class Test {
                  Short s1 = Short.valueOf((short) 42);
                  Short s2 = Short.valueOf("42");
                  Short s3 = 84; // Negative test, should not be rewritten
                  Short s4 = Short.parseShort("84"); // Negative test, should not be rewritten
              }
              """
          )
        );
    }

}
