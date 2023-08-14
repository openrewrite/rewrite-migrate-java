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

import java.util.Objects;
import java.util.regex.Pattern;

public class ApacheCommonsStringUtils {
    public static class Chop {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.chop(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.substring(0, s.length() - 1);
        }
    }

    public static class DefaultString {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.defaultString(s);
        }

        @AfterTemplate
        String after(String s) {
            return Objects.toString(s);
        }
    }

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

    public static class IsEmpty {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isEmpty(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s == null || s.isEmpty();
        }
    }

    public static class Replace {
        @BeforeTemplate
        String before(String s, String target, String replacement) {
            return StringUtils.replace(s, target, replacement);
        }

        @AfterTemplate
        String after(String s, String target, String replacement) {
            return s.replaceAll(target, replacement);
        }
    }

    public static class Split {
        @BeforeTemplate
        String[] before(String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return s.split(" ");
        }
    }

    public static class StripEnd {
        @BeforeTemplate
        String before(String s, String suffix) {
            return StringUtils.stripEnd(s, suffix);
        }

        @AfterTemplate
        String after(String s, String suffix) {
            return s.endsWith(suffix) ? s.substring(0, s.lastIndexOf(suffix)) : s;
        }
    }

    public static class Strip {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.strip(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.trim();
        }
    }

    //public static class LeftPad {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.leftPad(s, 5);
    //    }

    //    @AfterTemplate
    //    String after(String s) {
    //        return String.format("%" + 5 + "s", s);
    //    }
    //}

    //public static class RightPad {
    //    @BeforeTemplate
    //    String before(String s) {
    //        return StringUtils.rightPad(s, 5);
    //    }

    //    @AfterTemplate
    //    String after(String s) {
    //        return String.format("%" + (-5) + "s", s);
    //    }
    //}

    public static class Join {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.join(s);
        }

        @AfterTemplate
        String after(String s) {
            return String.join(s);
        }
    }

    public static class DeleteWhitespace {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.replaceAll("\\s+", "");
        }
    }

    public static class EndsWithIgnoreCase {
        @BeforeTemplate
        boolean before(String s, String suffix) {
            return StringUtils.endsWithIgnoreCase(s, suffix);
        }

        @AfterTemplate
        boolean after(String s, String suffix) {
            return s.toUpperCase().endsWith(suffix.toUpperCase());
        }
    }

    public static class IsAlphanumeric {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isAlphanumeric(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return s.matches("^[a-zA-Z0-9]*$");
        }
    }

    public static class IsAlphanumericSpace {
        @BeforeTemplate
        boolean before(String s) {
            return StringUtils.isAlphanumericSpace(s);
        }

        boolean after(String s) {
            return s.matches("^[a-zA-Z0-9\\s]*$");
        }
    }

    public static class Left {
        @BeforeTemplate
        String before(String s, int l) {
            return StringUtils.left(s, l);
        }

        @AfterTemplate
        String after(String s, int l) {
            return s == null ? null : s.substring(0, l);
        }
    }

    public static class Lowercase {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.lowerCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.toLowerCase();
        }
    }

    public static class Uppercase {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.upperCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.toUpperCase();
        }
    }

    public static class ReplaceOnce {
        @BeforeTemplate
        String before(String s, String search, String replacement) {
            return StringUtils.replaceOnce(s, search, replacement);
        }

        @AfterTemplate
        String after(String s, String search, String replacement) {
            return s.replaceFirst(Pattern.quote(search), replacement);
        }
    }

    public static class Reverse {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return new StringBuilder(s).reverse().toString();
        }
    }

    public static class Contains {
        @BeforeTemplate
        boolean before(String s, String search) {
            return StringUtils.contains(s, search);
        }

        @AfterTemplate
        boolean after(String s, String search) {
            return s.contains(search);
        }
    }

    public static class Substring {
        @BeforeTemplate
        String before(String s, int l, int w) {
            return StringUtils.substring(s, l, w);
        }

        @AfterTemplate
        String after(String s, int l, int w) {
            return s.substring(l, w);
        }
    }

    public static class StartsWith {
        @BeforeTemplate
        boolean before(String s, String prefix) {
            return StringUtils.startsWith(s, prefix);
        }

        @AfterTemplate
        boolean after(String s, String prefix) {
            return s.startsWith(prefix);
        }
    }

    public static class Trim {
        @BeforeTemplate
        String before(String s) {
            return StringUtils.trim(s);
        }

        @AfterTemplate
        String after(String s) {
            return s.trim();
        }
    }

    public static class EqualsIgnoreCase {
        @BeforeTemplate
        boolean before(String s, String other) {
            return StringUtils.equalsIgnoreCase(s, other);
        }

        @AfterTemplate
        boolean after(String s, String other) {
            return s.equalsIgnoreCase(other);
        }
    }

    public static class ReplaceTest {
        @BeforeTemplate
        String before(String s, String target, String replacement) {
            return StringUtils.replace(s, target, replacement);
        }

        @AfterTemplate
        String after(String s, String target, String replacement) {
            return s.replaceAll(target, replacement);
        }
    }

    public static class Repeat {
        @BeforeTemplate
        String before(String s, int l) {
            return StringUtils.repeat(s, l);
        }

        @AfterTemplate
        String after(String s, int l) {
            return new String(new char[l]).replace("\0", s);
        }
    }

    public static class Overlay {
        @BeforeTemplate
        String before(String s, int w, int l, String overlay) {
            return StringUtils.overlay(s, overlay, w, l);
        }

        @AfterTemplate
        String after(String s, int w, int l, String overlay) {
            return s.substring(0, w) + overlay + s.substring(l);
        }
    }

}
