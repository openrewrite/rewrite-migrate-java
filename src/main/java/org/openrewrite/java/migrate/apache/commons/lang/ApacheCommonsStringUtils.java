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
import org.openrewrite.java.template.NotMatches;

import java.util.Objects;

@SuppressWarnings("ALL")
public class ApacheCommonsStringUtils {

    private static class Abbreviate {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s1,
                      @NotMatches(MethodInvocationMatcher.class) int width) {
            return StringUtils.abbreviate(s1, width);
        }

        @AfterTemplate
        String after(String s, int width) {
            return (s == null || s.length() <= width ? s : s.substring(0, width - 3) + "...");
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class Capitalize {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.capitalize(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.isEmpty() || Character.isTitleCase(s.charAt(0)) ? s : Character.toTitleCase(s.charAt(0)) + s.substring(1));
        }
    }

    //NOTE: The test for this recipe fails, I think it could be due to a `rewrite-templating` bug
    //private static class Chomp {
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
    //private static class Chop {
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
    //private static class Contains {
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
    //private static class CountMatchesChar {
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

    private static class DefaultString {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.defaultString(s);
        }

        @AfterTemplate
        String after(String s) {
            return Objects.toString(s, "");
        }
    }

    private static class DefaultStringFallback {
        @BeforeTemplate
        String before(String s, String nullDefault) {
            return StringUtils.defaultString(s, nullDefault);
        }

        @AfterTemplate
        String after(String s, String nullDefault) {
            return Objects.toString(s, nullDefault);
        }
    }

    private static class DeleteWhitespace {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.replaceAll("\\s+", ""));
        }
    }

    // NOTE: unlikely to go over well due to added complexity
    //private static class EndsWithIgnoreCase {
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

    private static class EqualsIgnoreCase {
        @BeforeTemplate
        boolean before(@NotMatches(MethodInvocationMatcher.class) String s,
                       @NotMatches(MethodInvocationMatcher.class) String other) {
            return StringUtils.equalsIgnoreCase(s, other);
        }

        @AfterTemplate
        boolean after(String s, String other) {
            return (s == null && other == null || s != null && s.equalsIgnoreCase(other));
        }
    }

    private static class Equals {
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
    //private static class IndexOfAny {
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
    //private static class IsAlphanumericSpace {
    //    @BeforeTemplate
    //    boolean before(String s) {
    //        return StringUtils.isAlphanumericSpace(s);
    //    }

    //    boolean after(String s) {
    //        return (s != null && s.matches("^[a-zA-Z0-9\\s]*$"));
    //    }
    //}

    private static class IsAlphanumeric {
        @BeforeTemplate
        boolean before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.isAlphanumeric(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetterOrDigit));
        }
    }

    // NOTE: not sure if accurate replacement
    //private static class IsAlphaSpace {
    //    @BeforeTemplate
    //    boolean before(String s) {
    //        return StringUtils.isAlphaSpace(s);
    //    }

    //    @AfterTemplate
    //    boolean after(String s) {
    //        return (s != null && s.matches("[a-zA-Z\\s]+"));
    //    }
    //}

    //private static class StripAll {
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

    private static class IsAlpha {
        @BeforeTemplate
        boolean before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.isAlpha(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetter));
        }
    }

    // NOTE: better handled by `org.openrewrite.java.migrate.apache.commons.lang.IsNotEmptyToJdk`
    //private static class IsEmpty {
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

    //private static class Join {
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
    //private static class Left {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.left(s, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return (s == null ? null : s.substring(s.length() - l, s.length() - 1));
    //    }
    //}

    private static class Lowercase {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.lowerCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.toLowerCase());
        }
    }

    // NOTE: not sure if accurate replacement
    //private static class Mid {
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
    //private static class Overlay {
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
    //private static class Center {
    //    @BeforeTemplate
    //    String before(String s, int size) {
    //        return StringUtils.center(s, size);
    //    }

    //    @AfterTemplate
    //    String after(String s, int size) {
    //        return String.format("%" + size + "s" + s + "%" + size + "s", "", "");
    //    }
    //}

    private static class RemoveEnd {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s,
                      @NotMatches(MethodInvocationMatcher.class) String end) {
            return StringUtils.removeEnd(s, end);
        }

        @AfterTemplate
        String after(String s, String end) {
            return (s == null || s.isEmpty() || end == null || end.isEmpty() || !s.endsWith(end) ?
                    s : s.substring(0, s.length() - end.length()));
        }
    }

    //private static class Repeat {
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
    //private static class ReplaceOnce {
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

    private static class Replace {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s,
                      @NotMatches(MethodInvocationMatcher.class) String search,
                      @NotMatches(MethodInvocationMatcher.class) String replacement) {
            return StringUtils.replace(s, search, replacement);
        }

        @AfterTemplate
        String after(String s, String search, String replacement) {
            return (s == null || s.isEmpty() || search == null || search.isEmpty() || replacement == null ? s : s.replace(search, replacement));
        }
    }

    private static class Reverse {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : new StringBuilder(s).reverse().toString());
        }
    }

    // NOTE: not sure if accurate replacement
    //private static class Right {
    //    @BeforeTemplate
    //    String before(String s, int l) {
    //        return StringUtils.right(s, l);
    //    }

    //    @AfterTemplate
    //    String after(String s, int l) {
    //        return (s == null ? null : s.substring(s.length() - l, s.length() - 1));
    //    }
    //}

    private static class Split {
        @BeforeTemplate
        String[] before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return (s == null ? null : s.split("\\s+"));
        }
    }

    // NOTE: requires dedicated recipe to clean up `Pattern.quote(",")`
    //private static class SplitSeparator {
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
    //private static class SplitSeparatorMax {
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

    private static class Strip {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.strip(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    // NOTE: suffix is a set of characters, not a complete literal string
    //private static class StripEnd {
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
    //private static class StripStart {
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
    //private static class StartsWith {
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
    //private static class SubstringAfter {
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
    //private static class Substring {
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
    //private static class SwapCase {
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

    @SuppressWarnings("ConstantValue")
    private static class TrimToEmpty {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.trimToEmpty(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? "" : s.trim());
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class TrimToNull {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.trimToNull(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.trim().isEmpty() ? null : s.trim());
        }
    }

    private static class Trim {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.trim(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    private static class Uppercase {
        @BeforeTemplate
        String before(@NotMatches(MethodInvocationMatcher.class) String s) {
            return StringUtils.upperCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.toUpperCase());
        }
    }

    // NOTE: breaks on empty strings
    //private static class Uncapitalize {
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
