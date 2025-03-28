/*
 * Copyright 2024 the original author or authors.
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

import com.google.errorprone.refaster.Refaster;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        name = "A collection of `String` rules",
        description = "A collection of rules for refactoring methods called on `String` instances in Java code."
)
public class StringRules {
    @RecipeDescriptor(
            name = "Replace redundant `String` method calls with self",
            description = "Replace redundant `substring(..)` and `toString()` method calls with the `String` self."
    )
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static class RedundantCall {
        @BeforeTemplate
        public String before(String string) {
            return Refaster.anyOf(string.substring(0, string.length()), string.substring(0), string);
        }

        @AfterTemplate
        public String after(String string) {
            return string;
        }
    }

    @RecipeDescriptor(
            name = "Replace `String.indexOf(String, 0)` with `String.indexOf(String)`",
            description = "Replace `String.indexOf(String str, int fromIndex)` with `String.indexOf(String)`.")
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static class IndexOfString {
        @BeforeTemplate
        public int indexOfZero(String string, String test) {
            return string.indexOf(test, 0);
        }

        @AfterTemplate
        public int indexOf(String string, String test) {
            return string.indexOf(test);
        }
    }

    @RecipeDescriptor(
            name = "Replace `String.indexOf(char, 0)` with `String.indexOf(char)`",
            description = "Replace `String.indexOf(char ch, int fromIndex)` with `String.indexOf(char)`.")
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static class IndexOfChar {
        @BeforeTemplate
        public int indexOfZero(String string, char test) {
            return string.indexOf(test, 0);
        }

        @AfterTemplate
        public int indexOf(String string, char test) {
            return string.indexOf(test);
        }
    }

    @RecipeDescriptor(
            name = "Replace lower and upper case `String` comparisons with `String.equalsIgnoreCase(String)`",
            description = "Replace `String` equality comparisons involving `.toLowerCase()` or `.toUpperCase()` with `String.equalsIgnoreCase(String anotherString)`.")
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static class UseEqualsIgnoreCase {
        @BeforeTemplate
        public boolean bothLowerCase(String string, String test) {
            return string.toLowerCase().equals(test.toLowerCase());
        }

        @BeforeTemplate
        public boolean bothUpperCase(String string, String test) {
            return string.toUpperCase().equals(test.toUpperCase());
        }

        @AfterTemplate
        public boolean equalsIgnoreCase(String string, String test) {
            return string.equalsIgnoreCase(test);
        }
    }
}
