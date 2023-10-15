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
package org.openrewrite.java.migrate.apache.commons.lang;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.apache.commons.lang3.StringUtils;
import org.openrewrite.java.template.Matches;
import org.openrewrite.java.template.RecipeDescriptor;

import java.util.Objects;

@SuppressWarnings("ALL")
public class ApacheCommonsStringUtils {

    @RecipeDescriptor(
            name = "Replace `StringUtils.abbreviate(String, int)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.abbreviate(String str, int maxWidth)` with JDK internals.")
    public static class Abbreviate {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s1,
                      @Matches(RepeatableArgumentMatcher.class) int width) {
            return StringUtils.abbreviate(s1, width);
        }

        @AfterTemplate
        String after(String s, int width) {
            return (s == null || s.length() <= width ? s : s.substring(0, width - 3) + "...");
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.capitalize(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.capitalize(String str)` with JDK internals.")
    @SuppressWarnings("ConstantValue")
    public static class Capitalize {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.capitalize(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.isEmpty() || Character.isTitleCase(s.charAt(0)) ? s : Character.toTitleCase(s.charAt(0)) + s.substring(1));
        }
    }

    //NOTE: The test for this recipe fails, I think it could be due to a `rewrite-templating` bug
    //public static class Chomp {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.chomp(s);
    //    }

    //    @AfterTemplate
    //    String after(String s) {
    //        return (s == null ? null : (s.endsWith("\n") ? s.substring(0, s.length() - 1) : s));
    //    }
    //}

    // NOTE: fails with __P__. inserted
    //public static class Chop {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.chop(s);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s) {
    //        return (s == null ?
    //                null : s.length() < 2 ?
    //                "" : s.endsWith("\r\n") ?
    //                s.substring(0, s.length() - 2) : s.substring(0, s.length() - 1));
    //    }
    //}

    // NOTE: not sure if accurate replacement
    //public static class Contains {
    //    @BeforeTemplate
    //    boolean before(String s, String search) {
    //        return StringUtils.contains(s, search);
    //    }
    //
    //    @AfterTemplate
    //    boolean after(String s, String search) {
    //        return (s != null && search != null && s.contains(search));
    //    }
    //}

    // NOTE: Requires Java 9+ for s.chars()
    //public static class CountMatchesChar {
    //    @BeforeTemplate
    //    int before(String s, char pattern) {
    //        return StringUtils.countMatches(s, pattern);
    //    }
    //
    //    @AfterTemplate
    //    int after(String s, char pattern) {
    //        return (s == null || s.isEmpty() ? 0 : (int) s.chars().filter(c -> c == pattern).count());
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.defaultString(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.defaultString(String str)` with JDK internals.")
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
            name = "Replace `StringUtils.defaultString(String, String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.defaultString(String str, String nullDefault)` with JDK internals.")
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
            description = "Replace Apache Commons `StringUtils.deleteWhitespace(String str)` with JDK internals.")
    public static class DeleteWhitespace {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.replaceAll("\\s+", ""));
        }
    }

    // NOTE: unlikely to go over well due to added complexity
    //public static class EndsWithIgnoreCase {
    //    @BeforeTemplate
    //    boolean before(String s, String suffix) {
    //        return StringUtils.endsWithIgnoreCase(s, suffix);
    //    }
    //
    //    @AfterTemplate
    //    boolean after(String s, String suffix) {
    //        return (s == null && suffix == null || s != null && suffix != null && s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length()));
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.equalsIgnoreCase(CharSequence, CharSequence)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.equalsIgnoreCase(CharSequence cs1, CharSequence cs2)` with JDK internals.")
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
            name = "Replace `StringUtils.equals(CharSequence, CharSequence)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.equals(CharSequence cs1, CharSequence cs2)` with JDK internals.")
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

    // NOTE: unlikely to go over well due to added complexity
    //public static class IndexOfAny {
    //    @BeforeTemplate
    //    int before(String s, String searchChars) {
    //        return StringUtils.indexOfAny(s, searchChars);
    //    }
    //
    //    @AfterTemplate
    //    int after(String s, String searchChars) {
    //        return searchChars == null || searchChars.isEmpty() ? -1 :
    //                IntStream.range(0, searchChars.length())
    //                        .map(searchChars::charAt)
    //                        .map(s::indexOf)
    //                        .min()
    //                        .orElse(-1);
    //    }
    //}

    // NOTE: not sure if accurate replacement
    //public static class IsAlphanumericSpace {
    //    @BeforeTemplate
    //    boolean before(String s) {
    //        return StringUtils.isAlphanumericSpace(s);
    //    }

    //    boolean after(String s) {
    //        return (s != null && s.matches("^[a-zA-Z0-9\\s]*$"));
    //    }
    //}


    // `chars()` is only in Java 9+
    //public static class IsAlphanumeric {
    //    @BeforeTemplate
    //    boolean before(@Matches(RepeatableArgumentMatcher.class) String s) {
    //        return StringUtils.isAlphanumeric(s);
    //    }
    //
    //    @AfterTemplate
    //    boolean after(String s) {
    //        return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetterOrDigit));
    //    }
    //}

    // NOTE: not sure if accurate replacement
    //public static class IsAlphaSpace {
    //    @BeforeTemplate
    //    boolean before(String s) {
    //        return StringUtils.isAlphaSpace(s);
    //    }

    //    @AfterTemplate
    //    boolean after(String s) {
    //        return (s != null && s.matches("[a-zA-Z\\s]+"));
    //    }
    //}

    //public static class StripAll {
    //    @BeforeTemplate
    //    String[] before(String[] s) {
    //        return StringUtils.stripAll(s);
    //    }

    //    @AfterTemplate
    //    String[] after(String[] s) {
    //        return Arrays.stream(s)
    //                .map(String::trim)
    //                .toArray(String[]::new);
    //    }
    //}

    // `chars()` is only in Java 9+
    //public static class IsAlpha {
    //    @BeforeTemplate
    //    boolean before(@Matches(RepeatableArgumentMatcher.class) String s) {
    //        return StringUtils.isAlpha(s);
    //    }
    //
    //    @AfterTemplate
    //    boolean after(String s) {
    //        return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetter));
    //    }
    //}

    // NOTE: better handled by `org.openrewrite.java.migrate.apache.commons.lang.IsNotEmptyToJdk`
    //public static class IsEmpty {
    //    @BeforeTemplate
    //    boolean before(String s) {
    //        return StringUtils.isEmpty(s);
    //    }
    //
    //    @AfterTemplate
    //    boolean after(String s) {
    //        return (s == null || s.isEmpty());
    //    }
    //}

    // NOTE: These two methods don't generate the recipe templates right
    //public static class LeftPad {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.leftPad(s, l);
    //    }
    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return String.format("%" + l + "s", s);
    //    }
    //}

    //public static class RightPad {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.rightPad(s, l);
    //    }
    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return String.format("%" + (-l) + "s", s);
    //    }
    //}

    //public static class Join {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.join(s);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s) {
    //        return (s == null ? null : String.join("", s));
    //    }
    //}

    // NOTE: not sure if accurate replacement
    @SuppressWarnings("ConstantValue")
    //public static class Left {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.left(s, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return (s == null ? null : s.substring(s.length() - l, s.length() - 1));
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.lowerCase(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.lowerCase(String str)` with JDK internals.")
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

    // NOTE: not sure if accurate replacement
    //public static class Mid {
    //    @BeforeTemplate
    //    String before(String s, int p, int l) {
    //        return StringUtils.mid(s, p, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int p, int l) {
    //        return (s == null ? null : (p + l < s.length() ? s.substring(p, p + l) : s.substring(p, s.length() - 1)));
    //    }
    //}

    // NOTE: not sure if accurate replacement
    //public static class Overlay {
    //    @BeforeTemplate
    //    String before(String s, int w, int l, String overlay) {
    //        return StringUtils.overlay(s, overlay, w, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int w, int l, String overlay) {
    //        return (s == null ? null : s.substring(0, w) + overlay + s.substring(l));
    //    }
    //}

    // NOTE: Similar issues to what LeftPad and RightPad have
    //public static class Center {
    //    @BeforeTemplate
    //    String before(String s, int size) {
    //        return StringUtils.center(s, size);
    //    }

    //    @AfterTemplate
    //    String after(String s, int size) {
    //        return String.format("%" + size + "s" + s + "%" + size + "s", "", "");
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.removeEnd(String, String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.removeEnd(String str, String remove)` with JDK internals.")
    public static class RemoveEnd {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s,
                      @Matches(RepeatableArgumentMatcher.class) String end) {
            return StringUtils.removeEnd(s, end);
        }

        @AfterTemplate
        String after(String s, String end) {
            return (s == null || s.isEmpty() || end == null || end.isEmpty() || !s.endsWith(end) ?
                    s : s.substring(0, s.length() - end.length()));
        }
    }

    //public static class Repeat {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.repeat(s, l);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return (s == null ? null : new String(new char[l]).replace("\0", s));
    //    }
    //}

    // NOTE: requires dedicated recipe to clean up `Pattern.quote(",")`
    //public static class ReplaceOnce {
    //    @BeforeTemplate
    //    String before(String s, String search, String replacement) {
    //        return StringUtils.replaceOnce(s, search, replacement);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, String search, String replacement) {
    //        return (s == null || s.isEmpty() || search == null || search.isEmpty() || replacement == null ? s : s.replaceFirst(Pattern.quote(search), replacement));
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.replace(String, String, String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.replace(String text, String searchString, String replacement)` with JDK internals.")
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
            description = "Replace Apache Commons `StringUtils.reverse(String str)` with JDK internals.")
    public static class Reverse {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : new StringBuilder(s).reverse().toString());
        }
    }

    // NOTE: not sure if accurate replacement
    //public static class Right {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.right(s, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return (s == null ? null : s.substring(s.length() - l, s.length() - 1));
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.split(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.split(String str)` with JDK internals.")
    public static class Split {
        @BeforeTemplate
        String[] before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return (s == null ? null : s.split("\\s+"));
        }
    }

    // NOTE: requires dedicated recipe to clean up `Pattern.quote(",")`
    //public static class SplitSeparator {
    //    @BeforeTemplate
    //    String[] before(String s, String arg) {
    //        return StringUtils.split(s, arg);
    //    }
    //
    //    @AfterTemplate
    //    String[] after(String s, String arg) {
    //        return (s == null ? null : s.split(Pattern.quote(arg)));
    //    }
    //}

    // NOTE: different semantics in handling max=0 to discard trailing empty strings
    //public static class SplitSeparatorMax {
    //    @BeforeTemplate
    //    String[] before(String s, String arg, int max) {
    //        return StringUtils.split(s, arg, max);
    //    }
    //
    //    @AfterTemplate
    //    String[] after(String s, String arg, int max) {
    //        return (s == null ? null : s.split(Pattern.quote(arg), max));
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.strip(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.strip(String str)` with JDK internals.")
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

    // NOTE: suffix is a set of characters, not a complete literal string
    //public static class StripEnd {
    //    @BeforeTemplate
    //    String before(String s, String suffix) {
    //        return StringUtils.stripEnd(s, suffix);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, String suffix) {
    //        return (s == null ? null : (s.endsWith(suffix) ? s.substring(0, s.lastIndexOf(suffix)) : s));
    //    }
    //}

    // NOTE: suffix is a set of characters, not a complete literal string
    //public static class StripStart {
    //    @BeforeTemplate
    //    String before(String s, String chars) {
    //        return StringUtils.stripStart(s, chars);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, String chars) {
    //        return (s == null ? null : (s.startsWith(chars) ? s.substring(chars.length()) : s));
    //    }
    //}

    // NOTE: not sure if accurate replacement
    //public static class StartsWith {
    //    @BeforeTemplate
    //    boolean before(String s, String prefix) {
    //        return StringUtils.startsWith(s, prefix);
    //    }

    //    @AfterTemplate
    //    boolean after(String s, String prefix) {
    //        return (s == null || prefix == null ? null : s.startsWith(prefix));
    //    }
    //}

    // NOTE: Incorrect handling of before null/empty and separator null/empty
    //public static class SubstringAfter {
    //    @BeforeTemplate
    //    String before(String s, String sep) {
    //        return StringUtils.substringAfter(s, sep);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, String sep) {
    //        return (s == null ? null : s.substring(s.indexOf(sep) + 1, s.length()));
    //    }
    //}

    // NOTE: Incorrect handling of negative values
    //public static class Substring {
    //    @BeforeTemplate
    //    String before(String s, int l, int w) {
    //        return StringUtils.substring(s, l, w);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s, int l, int w) {
    //        return (s == null ? null : s.substring(l, w));
    //    }
    //}

    // NOTE: fails to account for isTitleCase
    //public static class SwapCase {
    //    @BeforeTemplate
    //    String before(String s, char sep) {
    //        return StringUtils.swapCase(s);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s) {
    //        return (s == null ? null : s.chars()
    //                .map(c -> Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c))
    //                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
    //                .toString());
    //    }
    //}

    @RecipeDescriptor(
            name = "Replace `StringUtils.trimToEmpty(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.trimToEmpty(String str)` with JDK internals.")
    @SuppressWarnings("ConstantValue")
    public static class TrimToEmpty {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.trimToEmpty(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? "" : s.trim());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.trimToNull(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.trimToNull(String str)` with JDK internals.")
    @SuppressWarnings("ConstantValue")
    public static class TrimToNull {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.trimToNull(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.trim().isEmpty() ? null : s.trim());
        }
    }

    @RecipeDescriptor(
            name = "Replace `StringUtils.trim(String)` with JDK internals",
            description = "Replace Apache Commons `StringUtils.trim(String str)` with JDK internals.")
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
            description = "Replace Apache Commons `StringUtils.upperCase(String str)` with JDK internals.")
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

    // NOTE: breaks on empty strings
    //public static class Uncapitalize {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.uncapitalize(s);
    //    }
    //
    //    @AfterTemplate
    //    String after(String s) {
    //        return (s == null ? null : Character.toLowerCase(s.charAt(0)) + s.substring(1));
    //    }
    //}
}
