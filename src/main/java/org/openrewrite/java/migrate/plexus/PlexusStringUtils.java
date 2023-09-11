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
package org.openrewrite.java.migrate.plexus;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.codehaus.plexus.util.StringUtils;
import org.openrewrite.java.migrate.apache.commons.lang.RepeatableArgumentMatcher;
import org.openrewrite.java.template.Matches;

import java.util.Objects;

@SuppressWarnings("ALL")
public class PlexusStringUtils {

    private static class Abbreviate {
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

    @SuppressWarnings("ConstantValue")
    private static class Capitalize {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.capitalise(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null || s.isEmpty() || Character.isTitleCase(s.charAt(0)) ? s : Character.toTitleCase(s.charAt(0)) + s.substring(1));
        }
    }

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
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.deleteWhitespace(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.replaceAll("\\s+", ""));
        }
    }

    private static class EqualsIgnoreCase {
        @BeforeTemplate
        boolean before(@Matches(RepeatableArgumentMatcher.class) String s,
                       @Matches(RepeatableArgumentMatcher.class) String other) {
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

    private static class IsAlphanumeric {
        @BeforeTemplate
        boolean before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.isAlphanumeric(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetterOrDigit));
        }
    }

    private static class IsAlpha {
        @BeforeTemplate
        boolean before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.isAlpha(s);
        }

        @AfterTemplate
        boolean after(String s) {
            return (s != null && !s.isEmpty() && s.chars().allMatch(Character::isLetter));
        }
    }

    private static class Lowercase {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.lowerCase(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.toLowerCase());
        }
    }

    private static class Replace {
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

    private static class Reverse {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.reverse(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : new StringBuilder(s).reverse().toString());
        }
    }

    private static class Split {
        @BeforeTemplate
        String[] before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.split(s);
        }

        @AfterTemplate
        String[] after(String s) {
            return (s == null ? null : s.split("\\s+"));
        }
    }

    private static class Strip {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.strip(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    private static class Trim {
        @BeforeTemplate
        String before(@Matches(RepeatableArgumentMatcher.class) String s) {
            return StringUtils.trim(s);
        }

        @AfterTemplate
        String after(String s) {
            return (s == null ? null : s.trim());
        }
    }

    private static class Uppercase {
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
