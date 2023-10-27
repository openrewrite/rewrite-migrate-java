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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

/**
 * Handles the migration of the deprecated constructors of primitive wrappers
 * highlighted <a href="https://docs.oracle.com/javase/9/docs/api/deprecated-list.html#constructor">here</a>.
 */
@SuppressWarnings("removal")
public class MigrateDeprecatedBoxedPrimitiveConstructors {

    @RecipeDescriptor(
        name = "Replace deprecated `Boolean(boolean)` constructor invocations",
        description = "Replace deprecated `Boolean(boolean)` constructor invocations with `Boolean.valueOf(boolean)`."
    )
    public static class BooleanConstructor {
        @BeforeTemplate
        public Boolean booleanConstructor(boolean value) {
            return new Boolean(value);
        }

        @AfterTemplate
        public Boolean valueOf(boolean value) {
            return Boolean.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Boolean(String)` constructor invocations",
        description = "Replace deprecated `Boolean(String)` constructor invocations with `Boolean.valueOf(String)`."
    )
    public static class BooleanStringConstructor {
        @BeforeTemplate
        public Boolean stringConstructor(String value) {
            return new Boolean(value);
        }

        @AfterTemplate
        public Boolean valueOf(String value) {
            return Boolean.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Byte(byte)` constructor invocations",
        description = "Replace deprecated `Byte(byte)` constructor invocations with `Byte.valueOf(byte)`."
    )
    public static class ByteConstructor {
        @BeforeTemplate
        public Byte byteConstructor(byte value) {
            return new Byte(value);
        }

        @AfterTemplate
        public Byte valueOf(byte value) {
            return Byte.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Byte(String)` constructor invocations",
        description = "Replace deprecated `Byte(String)` constructor invocations with `Byte.valueOf(String)`."
    )
    public static class ByteStringConstructor {
        @BeforeTemplate
        public Byte stringConstructor(String value) {
            return new Byte(value);
        }

        @AfterTemplate
        public Byte valueOf(String value) {
            return Byte.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Character(char)` constructor invocations",
        description = "Replace deprecated `Character(char)` constructor invocations with `Character.valueOf(char)`."
    )
    public static class CharacterConstructor {
        @BeforeTemplate
        public Character charConstructor(char value) {
            return new Character(value);
        }

        @AfterTemplate
        public Character valueOf(char value) {
            return Character.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Double(double)` constructor invocations",
        description = "Replace deprecated `Double(double)` constructor invocations with `Double.valueOf(double)`."
    )
    public static class DoubleConstructor {
        @BeforeTemplate
        public Double doubleConstructor(double value) {
            return new Double(value);
        }

        @AfterTemplate
        public Double valueOf(double value) {
            return Double.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Double(String)` constructor invocations",
        description = "Replace deprecated `Double(String)` constructor invocations with `Double.valueOf(String)`."
    )
    public static class DoubleStringConstructor {
        @BeforeTemplate
        public Double stringConstructor(String value) {
            return new Double(value);
        }

        @AfterTemplate
        public Double valueOf(String value) {
            return Double.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Float(float)` constructor invocations",
        description = "Replace deprecated `Float(float)` constructor invocations with `Float.valueOf(float)`."
    )
    public static class FloatConstructor {
        @BeforeTemplate
        public Float floatConstructor(float value) {
            return new Float(value);
        }

        @AfterTemplate
        public Float valueOf(float value) {
            return Float.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Float(String)` constructor invocations",
        description = "Replace deprecated `Float(String)` constructor invocations with `Float.valueOf(String)`."
    )
    public static class FloatStringConstructor {
        @BeforeTemplate
        public Float stringConstructor(String value) {
            return new Float(value);
        }

        @AfterTemplate
        public Float valueOf(String value) {
            return Float.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Float(Double)` constructor invocations",
        description = "Replace deprecated `Float(Double)` constructor invocations with `Float.valueOf(Double#floatValue)`."
    )
    public static class FloatDoubleConstructor {
        @BeforeTemplate
        public Float doubleConstructor(Double value) {
            return new Float(value);
        }

        @AfterTemplate
        public Float valueOf(Double value) {
            return Float.valueOf(value.floatValue());
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Integer(int)` constructor invocations",
        description = "Replace deprecated `Integer(int)` constructor invocations with `Integer.valueOf(int)`."
    )
    public static class IntegerConstructor {
        @BeforeTemplate
        public Integer intConstructor(int value) {
            return new Integer(value);
        }

        @AfterTemplate
        public Integer valueOf(int value) {
            return Integer.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Integer(String)` constructor invocations",
        description = "Replace deprecated `Integer(String)` constructor invocations with `Integer.valueOf(String)`."
    )
    public static class IntegerStringConstructor {
        @BeforeTemplate
        public Integer stringConstructor(String value) {
            return new Integer(value);
        }

        @AfterTemplate
        public Integer valueOf(String value) {
            return Integer.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Long(long)` constructor invocations",
        description = "Replace deprecated `Long(long)` constructor invocations with `Long.valueOf(long)`."
    )
    public static class LongConstructor {
        @BeforeTemplate
        public Long longConstructor(long value) {
            return new Long(value);
        }

        @AfterTemplate
        public Long valueOf(long value) {
            return Long.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Long(String)` constructor invocations",
        description = "Replace deprecated `Long(String)` constructor invocations with `Long.valueOf(String)`."
    )
    public static class LongStringConstructor {
        @BeforeTemplate
        public Long stringConstructor(String value) {
            return new Long(value);
        }

        @AfterTemplate
        public Long valueOf(String value) {
            return Long.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Short(short)` constructor invocations",
        description = "Replace deprecated `Short(short)` constructor invocations with `Short.valueOf(short)`."
    )
    public static class ShortConstructor {
        @BeforeTemplate
        public Short shortConstructor(short value) {
            return new Short(value);
        }

        @AfterTemplate
        public Short valueOf(short value) {
            return Short.valueOf(value);
        }
    }

    @RecipeDescriptor(
        name = "Replace deprecated `Short(String)` constructor invocations",
        description = "Replace deprecated `Short(String)` constructor invocations with `Short.valueOf(String)`."
    )
    public static class ShortStringConstructor {
        @BeforeTemplate
        public Short stringConstructor(String value) {
            return new Short(value);
        }

        @AfterTemplate
        public Short valueOf(String value) {
            return Short.valueOf(value);
        }
    }

}
