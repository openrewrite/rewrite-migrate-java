/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.joda;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JodaTimeScannerTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("joda-time"));
    }

    @Test
    void noUnsafeVar() {
        JodaTimeScanner scanner = new JodaTimeScanner(new HashSet<>());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;
               import java.util.Date;

               class A {
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = new DateTime(dtz);
                       print(dt.toDate());
                   }
                   private void print(Date date) {
                       System.out.println(date);
                   }
              }
              """
          )
        );
        assertTrue(scanner.getUnsafeVars().isEmpty());
    }

    @Test
    void hasUnsafeVars() {
        JodaTimeScanner scanner = new JodaTimeScanner(new HashSet<>());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   DateTime dateTime;
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = new DateTime(dtz);
                       print(dt.toDateTime());
                   }
                   private void print(DateTime dateTime) { // method parameter not handled yet
                       System.out.println(dateTime);
                   }
              }
              """
          )
        );
        // The variable dtz is unsafe due to dt. The dt variable is unsafe because its associated expression
        // is passed as argument to method, and migration of method parameters has not been implemented yet.
        assertEquals(4, scanner.getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dtz") ||
                       var.getSimpleName().equals("dt") ||
                       var.getSimpleName().equals("dateTime")
            );
        }
    }

    @Test
    void localVarReferencingClassVar() { // not supported yet
        JodaTimeScanner scanner = new JodaTimeScanner(new HashSet<>());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   DateTime dateTime;
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = dateTime.minus(2);
                       System.out.println(dt);
                   }
              }
              """
          )
        );
        // The local variable dt is unsafe due to class var datetime.
        assertEquals(2, scanner.getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dateTime") || var.getSimpleName().equals("dt"));
        }
    }

    @Test
    void localVarUsedReferencedInReturnStatement() { // not supported yet
        JodaTimeScanner scanner = new JodaTimeScanner(new HashSet<>());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   public DateTime foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = new DateTime(dtz);
                       return dt.plus(2);
                   }
              }
              """
          )
        );
        // The local variable dt used in return statement.
        assertEquals(2, scanner.getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dtz") || var.getSimpleName().equals("dt"));
        }
    }
}
