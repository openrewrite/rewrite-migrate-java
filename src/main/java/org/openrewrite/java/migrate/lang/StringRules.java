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

public class StringRules {

    @SuppressWarnings("StringOperationCanBeSimplified")
    static class RedundantCall {
        @BeforeTemplate
        public String start(String string) {
            return string.substring(0, string.length());
        }

        @BeforeTemplate
        public String startAndEnd(String string) {
            return string.substring(0);
        }

        @BeforeTemplate
        public String toString(String string) {
            return string.toString();
        }

        @AfterTemplate
        public String self(String string) {
            return string;
        }
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    static class IndexOfString {
        @BeforeTemplate
        public int indexOfZero(String string, String test) {
            return string.indexOf(test, 0);
        }

        @AfterTemplate
        public int indexOf(String string, String test) {
            return string.indexOf(test);
        }
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    static class IndexOfChar {
        @BeforeTemplate
        public int indexOfZero(String string, char test) {
            return string.indexOf(test, 0);
        }

        @AfterTemplate
        public int indexOf(String string, char test) {
            return string.indexOf(test);
        }
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    static class UseEqualsIgnoreCase {
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
