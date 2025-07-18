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
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwitchUtilsTest {
    private static J.Switch extractSwitch(@Language("java") String code) {
        J.CompilationUnit cu = (J.CompilationUnit) JavaParser.fromJavaVersion().build().parse(code).findFirst().get();
        return new JavaIsoVisitor<AtomicReference<J.Switch>>() {
            @Override
            public J.Switch visitSwitch(J.Switch _switch, AtomicReference<J.Switch> switchAtomicReference) {
                switchAtomicReference.set(_switch);
                return _switch;
            }
        }.reduce(cu, new AtomicReference<>()).get();
    }

    @Test
    void coversAllCasesAllEnums() {
        assertTrue(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    void method(TrafficLight light) {
                        switch (light) {
                            case RED -> System.out.println("stop");
                            case YELLOW -> System.out.println("caution");
                            case GREEN -> System.out.println("go");
                        }
                    }
                    enum TrafficLight { RED, YELLOW, GREEN }
                }
                """
            )
          )
        );
    }

    @Test
    void coversAllCasesMissingEnums() {
        assertFalse(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    void method(TrafficLight light) {
                        switch (light) {
                            case RED -> System.out.println("stop");
                            case YELLOW -> System.out.println("caution");
                        }
                    }
                    enum TrafficLight { RED, YELLOW, GREEN }
                }
                """
            )
          )
        );
    }

    @Test
    void coversAllCasesMissingEnumsWithDefault() {
        assertTrue(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    void method(TrafficLight light) {
                        switch (light) {
                            case RED -> System.out.println("stop");
                            case YELLOW -> System.out.println("caution");
                            default -> System.out.println("unknown");
                        }
                    }
                    enum TrafficLight { RED, YELLOW, GREEN }
                }
                """
            )
          )
        );
    }

    @Test
    void coversAllCasesEnumOnlyDefault() {
        assertTrue(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    void method(TrafficLight light) {
                        switch (light) {
                            default -> System.out.println("unknown");
                        }
                    }
                    enum TrafficLight { RED, YELLOW, GREEN }
                }
                """
            )
          )
        );
    }

    @Test
    void coversAllCasesObjectOnlyDefault() {
        assertTrue(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    void method(Object obj) {
                        switch (obj) {
                            default -> System.out.println("default");
                        }
                    }
                }
                """
            )
          )
        );
    }

    @ExpectedToFail("Not implemented yet for sealed classes")
    @Test
    void coversAllCasesAllSealedClasses() {
        assertTrue(
          SwitchUtils.coversAllPossibleValues(
            extractSwitch(
              """
                class Test {
                    sealed abstract class Shape permits Circle, Square, Rectangle {}
                    void method(Shape shape) {
                        switch (shape) {
                            case Circle c -> System.out.println("circle");
                            case Square s -> System.out.println("square");
                            case Rectangle r -> System.out.println("rectangle");
                        }
                    }
                }
                """
            )
          )
        );
    }
}
