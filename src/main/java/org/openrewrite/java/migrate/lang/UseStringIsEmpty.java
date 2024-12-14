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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        name = "Replace `0 < s.length()` with `!s.isEmpty()`",
        description = "Replace `0 < s.length()` and `s.length() != 0` with `!s.isEmpty()`."
)
public class UseStringIsEmpty {
    @BeforeTemplate
    boolean beforeGreaterThan(String s) {
        return s.length() > 0;
    }

    @BeforeTemplate
    boolean beforeLessThan(String s) {
        return 0 < s.length();
    }

    @BeforeTemplate
    boolean beforeNotZero(String s) {
        return 0 != s.length();
    }

    @BeforeTemplate
    boolean beforeNotZeroEither(String s) {
        return s.length() != 0;
    }

    @AfterTemplate
    boolean after(String s) {
        return !s.isEmpty();
    }
}
