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
package org.openrewrite.java.migrate.maven.shared;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.apache.maven.shared.utils.StringUtils;
import org.openrewrite.java.migrate.apache.commons.lang.RepeatableArgumentMatcher;
import org.openrewrite.java.template.Matches;
import org.openrewrite.java.template.RecipeDescriptor;

import java.util.Objects;

@SuppressWarnings("ALL")
public class MavenSharedStringUtils {
    @RecipeDescriptor(
            name = "Replace `StringUtils.abbreviate(String, int)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.abbreviate(String str, int maxWidth)` with JDK internals.")
    public static class Abbreviate {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s1,
                      @Matches(RepeatableArgumentMatcher.class) int width) {
            return StringUtils.abbreviate(s1, width);
        }

        @AfterTemplate
        String after(String s, int width) {
            return (s.length() <= width ? s : s.substring(0, width - 3) + "...");
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.capitalise(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.capitalise(String str)` with JDK internals.")
    @SuppressWarnings("ConstantValue")
    public static class Capitalise {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.capitalise(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.isEmpty() ? s : Character.toTitleCase(s.charAt(0)) + s.substring(1));
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.defaultString(Object)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.defaultString(Object obj)` with JDK internals.")
    public static class DefaultString {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.defaultString(s);
        }

        @AfterTemplate
        String after(String s) {
            return Objects.toString(s, "");
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.defaultString(Object, String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.defaultString(Object obj, String nullDefault)` with JDK internals.")
    public static class DefaultStringFallback {
        @BeforeTemplate
        String before(String s, String nullDefault) {
            return StringUtils.defaultString(s, nullDefault);
        }

        @AfterTemplate
        String after(String s, String nullDefault) {
            return Objects.toString(s, nullDefault);
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.deleteWhitespace(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.deleteWhitespace(String str)` with JDK internals.")
    public static class DeleteWhitespace {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.replaceAll("\\s+", "");
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.equalsIgnoreCase(String, String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.equalsIgnoreCase(String str1, String str2)` with JDK internals.")
    public static class EqualsIgnoreCase {
        @BeforeTemplate
        boolean before(@Matches(RepeatableArgumentMatcher.class) String s,
                       @Matches(RepeatableArgumentMatcher.class) String other) {
            return StringUtils.equalsIgnoreCase(s, other);
        }

        @AfterTemplate
        boolean after(String s, String other) {
            return (s == null ? other == null : s.equalsIgnoreCase(other));
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.equals(String, String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.equals(String str1, String str2)` with JDK internals.")
    public static class Equals {
        @BeforeTemplate
        boolean before(String s, String other) {
            return StringUtils.equals(s, other);
        }

        @AfterTemplate
        boolean after(String s, String other) {
            return Objects.equals(s, other);
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.lowerCase(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.lowerCase(String str)` with JDK internals.")
    public static class Lowercase {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.lowerCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.toLowerCase());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.replace(String, String, String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.replace(String text, String searchString, String replacement)` with JDK internals.")
    public static class Replace {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s,
                      @Matches(RepeatableArgumentMatcher.class) String search,
                      @Matches(RepeatableArgumentMatcher.class) String replacement) {
            return StringUtils.replace(s, search, replacement);
        }

        @AfterTemplate
        String after(String s, String search, String replacement) {
            return (s == null || s.isEmpty() || search == null || search.isEmpty() || replacement == null ? s : s.replace(search, replacement));
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.reverse(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.reverse(String str)` with JDK internals.")
    public static class Reverse {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : new StringBuffer(s).reverse().toString());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.split(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.split(String str)` with JDK internals.")
    public static class Split {
        @BeforeTemplate
        String[] before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return s.split("\\s+");
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.strip(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.strip(String str)` with JDK internals.")
    public static class Strip {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.strip(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.trim(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.trim(String str)` with JDK internals.")
    public static class Trim {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.trim(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.upperCase(String)` with JDK internals",
            description = "Replace Maven Shared `StringUtils.upperCase(String str)` with JDK internals.")
    public static class Uppercase {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.upperCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.toUpperCase());
        }
    }
}
