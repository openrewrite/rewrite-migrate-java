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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchUtilsTest {
    private static J.Switch getSwitchElement(@Language("java") String code) {
        JavaParser parser = JavaParser.fromJavaVersion().build();
        J.CompilationUnit cu = (J.CompilationUnit) parser.parse(code).findFirst().get();

        AtomicReference<J.Switch> foundSwitch = new AtomicReference<>();
        new TreeVisitor<J, ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext executionContext) {
                if (foundSwitch.get() != null) {
                    return tree;
                }
                if (tree instanceof J.Switch sw) {
                    foundSwitch.set(sw);
                }
                return super.preVisit(tree, executionContext);
            }


        }.visit(cu, new InMemoryExecutionContext());

        return foundSwitch.get();
    }

    @Test
    void coversAllCasesAllEnums() {
        assertTrue(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              void method(TrafficLight light) {
                    switch (light) {
                        case RED -> System.out.println("stop");
                        case YELLOW -> System.out.println("caution");
                        case GREEN -> System.out.println("go");
                    }
              }
              enum TrafficLight { RED, YELLOW, GREEN }
          }
          """)));
    }

    @Test
    void coversAllCasesMissingEnums() {
        assertFalse(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              void method(TrafficLight light) {
                    switch (light) {
                        case RED -> System.out.println("stop");
                        case YELLOW -> System.out.println("caution");
                    }
              }
              enum TrafficLight { RED, YELLOW, GREEN }
          }
          """)));
    }

    @Test
    void coversAllCasesMissingEnumsWithDefault() {
        assertTrue(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              void method(TrafficLight light) {
                    switch (light) {
                        case RED -> System.out.println("stop");
                        case YELLOW -> System.out.println("caution");
                        default -> System.out.println("unknown");
                    }
              }
              enum TrafficLight { RED, YELLOW, GREEN }
          }
          """)));
    }

    @Test
    void coversAllCasesEnumOnlyDefault() {
        assertTrue(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              void method(TrafficLight light) {
                    switch (light) {
                        default -> System.out.println("unknown");
                    }
              }
              enum TrafficLight { RED, YELLOW, GREEN }
          }
          """)));
    }

    @Test
    void coversAllCasesObjectOnlyDefault() {
        assertTrue(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              void method(Object obj) {
                    switch (obj) {
                        default -> System.out.println("default");
                    }
              }
          }
          """)));
    }

    @Test
    @Disabled("Unsupported yet")
    void coversAllCasesAllSealedClasses() {
        assertTrue(SwitchUtils.coversAllPossibleValues(getSwitchElement("""
          public class Test {
              sealed abstract class Shape permits Circle, Square, Rectangle {}
              void method(Shape shape) {
                    switch (shape) {
                        case Circle c -> System.out.println("circle");
                        case Square s -> System.out.println("square");
                        case Rectangle r -> System.out.println("rectangle");
                    }
              }
          }
          """)));
    }
}
