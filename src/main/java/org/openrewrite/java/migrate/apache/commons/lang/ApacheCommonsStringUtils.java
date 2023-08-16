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

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class ApacheCommonsStringUtils {

    private static class Abbreviate {
        @BeforeTemplate
        String before(String s1, int width) {
            return StringUtils.abbreviate(s1, width);
        }

        @AfterTemplate
        String after(String s, int width) {
            return s == null || s.length() <= width ? s : s.substring(0, width - 3) + "...";
        }
    }

    private static class Capitalize {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.capitalize(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    //TODO: The test for this recipe fails, I think it could be due to a `rewrite-templating` bug
    private static class Chomp {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.chomp(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.endsWith("\n") ? s.substring(0, s.length() - 1) : s;
        }
    }
    private static class Chop {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.chop(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.substring(0, s.length() - 1);
        }
    }

    private static class Contains {
        @BeforeTemplate
        boolean before(String s, String search) {
            return StringUtils.contains(s, search);
        }

        @AfterTemplate
        boolean after(String s, String search) {
            return s.contains(search);
        }
    }

    private static class CountMatches {
        @BeforeTemplate
        int before(String s, String pattern) {
            return StringUtils.countMatches(s, pattern);
        }

        @AfterTemplate
        int after(String s, String pattern) {
            return s.length() - s.replace(pattern, "").length();
        }
    }

    private static class DefaultString {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.defaultString(s);
        }

        @AfterTemplate
        String after(String s) {
            return Objects.toString(s);
        }
    }

    private static class DeleteWhitespace {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.replaceAll("\\s+", "");
        }
    }

    private static class EndsWithIgnoreCase {
        @BeforeTemplate
        boolean before(String s, String suffix) {
            return StringUtils.endsWithIgnoreCase(s, suffix);
        }

        @AfterTemplate
        boolean after(String s, String suffix) {
            return s.regionMatches(true, 0, suffix, 0, suffix.length());
        }
    }

    private static class EqualsIgnoreCase {
        @BeforeTemplate
        boolean before(String s, String other) {
            return StringUtils.equalsIgnoreCase(s, other);
        }

        @AfterTemplate
        boolean after(String s, String other) {
            return s.equalsIgnoreCase(other);
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

    private static class IndexOfAny {
        @BeforeTemplate
        int before(String s, String search) {
            return StringUtils.indexOfAny(s, search);
        }

        @AfterTemplate
        int after(String s, String search) {
            return IntStream.range(0, s.length())
                    .filter(i -> search.indexOf(s.charAt(i)) >= 0)
                    .min()
                    .orElse(-1);
        }
    }

    private static class IsAlphanumericSpace {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isAlphanumericSpace(s);
        }

        boolean after(String s) {
            return s.matches("^[a-zA-Z0-9\\s]*$");
        }
    }

    private static class IsAlphanumeric {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isAlphanumeric(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s.matches("^[a-zA-Z0-9]*$");
        }
    }

    private static class IsAlphaSpace {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isAlphaSpace(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s.matches("[a-zA-Z\\s]+");
        }
    }

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
        boolean before(String s) {
            return StringUtils.isAlpha(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s.chars().allMatch(Character::isLetter);
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class IsEmpty {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isEmpty(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s == null || s.isEmpty();
        }
    }

    // TODO: These two methods don't generate the recipe templates right
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

    private static class Join {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.join(s);
        }

        @AfterTemplate
        String after(String s) {
            return String.join(s);
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class Left {
        @BeforeTemplate
        String before(String s, int l) {
            return StringUtils.left(s, l);
        }

        @AfterTemplate
        String after(String s, int l) {
            return s == null ? null : s.substring(0, l);
        }
    }

    private static class Lowercase {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.lowerCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.toLowerCase();
        }
    }

    private static class Mid {
        @BeforeTemplate
        String before(String s, int p, int l) {
            return StringUtils.mid(s, p, l);
        }

        @AfterTemplate
        String after(String s, int p, int l) {
            return p + l < s.length() ? s.substring(p, p + l) : s.substring(p, s.length() - 1);
        }
    }

    private static class Overlay {
        @BeforeTemplate
        String before(String s, int w, int l, String overlay) {
            return StringUtils.overlay(s, overlay, w, l);
        }

        @AfterTemplate
        String after(String s, int w, int l, String overlay) {
            return s.substring(0, w) + overlay + s.substring(l);
        }
    }

    // TODO: Similar issues to what LeftPad and RightPad have
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
        String before(String s, String remove) {
            return StringUtils.removeEnd(s, remove);
        }

        @AfterTemplate
        String after(String s, String remove) {
            return s.endsWith(remove) ? s.substring(0, s.length() - remove.length()) : s;
        }
    }

    private static class Repeat {
        @BeforeTemplate
        String before(String s, int l) {
            return StringUtils.repeat(s, l);
        }

        @AfterTemplate
        String after(String s, int l) {
            return new String(new char[l]).replace("\0", s);
        }
    }

    private static class ReplaceOnce {
        @BeforeTemplate
        String before(String s, String search, String replacement) {
            return StringUtils.replaceOnce(s, search, replacement);
        }

        @AfterTemplate
        String after(String s, String search, String replacement) {
            return s.replaceFirst(Pattern.quote(search), replacement);
        }
    }

    private static class ReplaceTest {
        @BeforeTemplate
        String before(String s, String target, String replacement) {
            return StringUtils.replace(s, target, replacement);
        }

        @AfterTemplate
        String after(String s, String target, String replacement) {
            return s.replaceAll(target, replacement);
        }
    }

    private static class Replace {
        @BeforeTemplate
        String before(String s, String target, String replacement) {
            return StringUtils.replace(s, target, replacement);
        }

        @AfterTemplate
        String after(String s, String target, String replacement) {
            return s.replaceAll(target, replacement);
        }
    }

    private static class Reverse {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return new StringBuilder(s).reverse().toString();
        }
    }

    private static class Right {
        @BeforeTemplate
        String before(String s, int l) {
            return StringUtils.right(s, l);
        }

        @AfterTemplate
        String after(String s, int l) {
            return s.substring(s.length() - l, s.length() - 1);
        }
    }

    private static class SplitWithArg {
        @BeforeTemplate
        String[] before(String s, String arg) {
            return StringUtils.split(s, arg);
        }

        @AfterTemplate
        String[] after(String s, String arg) {
            return s == null ? null : s.split(arg);
        }
    }

    private static class StripEnd {
        @BeforeTemplate
        String before(String s, String suffix) {
            return StringUtils.stripEnd(s, suffix);
        }

        @AfterTemplate
        String after(String s, String suffix) {
            return s.endsWith(suffix) ? s.substring(0, s.lastIndexOf(suffix)) : s;
        }
    }

    private static class StripStart {
        @BeforeTemplate
        String before(String s, String chars) {
            return StringUtils.stripStart(s, chars);
        }

        @AfterTemplate
        String after(String s, String chars) {
            return s.startsWith(chars) ? s.substring(chars.length()) : s;
        }
    }

    private static class StartsWith {
        @BeforeTemplate
        boolean before(String s, String prefix) {
            return StringUtils.startsWith(s, prefix);
        }

        @AfterTemplate
        boolean after(String s, String prefix) {
            return s.startsWith(prefix);
        }
    }

    private static class Split {
        @BeforeTemplate
        String[] before(String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return s.split(" ");
        }
    }

    private static class Strip {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.strip(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.trim();
        }
    }

    @SuppressWarnings("StringOperationCanBeSimplified")
    private static class SubstringAfter {
        @BeforeTemplate
        String before(String s, String sep) {
            return StringUtils.substringAfter(s, sep);
        }

        @AfterTemplate
        String after(String s, String sep) {
            return s.substring(s.indexOf(sep) + 1, s.length());
        }
    }

    private static class Substring {
        @BeforeTemplate
        String before(String s, int l, int w) {
            return StringUtils.substring(s, l, w);
        }

        @AfterTemplate
        String after(String s, int l, int w) {
            return s.substring(l, w);
        }
    }

    private static class SwapCase {
        @BeforeTemplate
        String before(String s, char sep) {
            return StringUtils.swapCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.chars()
                    .map(c -> Character.isUpperCase(c) ? Character.toLowerCase(c) : Character.toUpperCase(c))
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class TrimToEmpty {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.trimToEmpty(s);
        }

        @AfterTemplate
        String after(String s) {
            return s != null ? s.trim() : "";
        }
    }

    @SuppressWarnings("ConstantValue")
    private static class TrimToNull {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.trimToNull(s);
        }

        @AfterTemplate
        String after(String s) {
            return s == null || s.trim() == null ? null : s.trim();
        }
    }

    private static class Trim {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.trim(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.trim();
        }
    }

    private static class Uppercase {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.upperCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.toUpperCase();
        }
    }

    private static class Uncapitalize {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.uncapitalize(s);
        }

        @AfterTemplate
        String after(String s) {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }
}
