/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.jspecify.annotations.Nullable;

/**
 * Converts Java regex patterns used in GraalVM resource-config.json (JDK 21 and earlier)
 * to glob patterns used in JDK 23+.
 */
public final class RegexToGlobConverter {

    private RegexToGlobConverter() {
    }

    /**
     * Result of a regex to glob conversion attempt.
     */
    public static final class ConversionResult {
        private final @Nullable String glob;
        private final @Nullable String warningMessage;

        public ConversionResult(@Nullable String glob, @Nullable String warningMessage) {
            this.glob = glob;
            this.warningMessage = warningMessage;
        }

        public @Nullable String glob() {
            return glob;
        }

        public @Nullable String warningMessage() {
            return warningMessage;
        }

        public boolean isSuccessful() {
            return glob != null;
        }
    }

    /**
     * Convert a Java regex pattern to a GraalVM glob pattern.
     *
     * @param regex The regex pattern from the old resource-config.json format
     * @return The conversion result containing the glob pattern or an error message
     */
    public static ConversionResult convert(String regex) {
        if (regex == null || regex.isEmpty()) {
            return new ConversionResult(null, "Empty pattern cannot be converted");
        }

        // Check for patterns that cannot be converted
        String unconvertibleReason = findUnconvertibleConstruct(regex);
        if (unconvertibleReason != null) {
            return new ConversionResult(null, unconvertibleReason);
        }

        String glob = regex;

        // Note: OpenRewrite's JSON parser preserves JSON escape sequences in values,
        // so "\\." in JSON source appears as "\\\\.properties" in the value (double backslash).
        // We need to handle this by first normalizing double backslashes.

        // Step 1: Handle escaped dots - temporarily replace with placeholder
        // The placeholder uses Unicode null chars to avoid collision with actual content
        // Handle both \\. (JSON escaped) and \. (standard regex) formats
        glob = glob.replace("\\\\.", "\u0000DOT\u0000");  // JSON escaped: \\.
        glob = glob.replace("\\.", "\u0000DOT\u0000");    // Standard regex: \.

        // Step 2: Handle [^/]* and [^/]+ (matches any characters except slash on one level) -> *
        glob = glob.replace("[^/]*", "*");
        glob = glob.replace("[^/]+", "*");

        // Step 3: Handle .* patterns
        // The order of these replacements is important!

        // Pattern: .*\.ext (match all files with extension recursively)
        // ".*\.txt" -> after DOT placeholder: ".*\u0000DOT\u0000txt"
        // We want: "**/*.txt"
        // Match .* at start followed by DOT placeholder, replace with **/*
        glob = glob.replaceAll("^\\.\\*\u0000DOT\u0000", "**/*.");

        // Handle .* at end of string -> **
        glob = glob.replaceAll("\\.\\*$", "**");

        // Handle /.* in the middle or end -> /**
        glob = glob.replace("/.*", "/**");

        // Handle remaining .* -> **  (this catches .* in the middle of a path)
        glob = glob.replace(".*", "**");

        // Step 4: Restore escaped dots
        glob = glob.replace("\u0000DOT\u0000", ".");

        // Step 5: Remove remaining backslash escapes that aren't needed in glob
        // In glob, most characters are literal, so we just remove the backslash
        glob = glob.replace("\\/", "/");
        glob = glob.replace("\\-", "-");
        glob = glob.replace("\\_", "_");

        // Step 6: Validate the resulting glob pattern
        String validationError = validateGlob(glob);
        if (validationError != null) {
            return new ConversionResult(null, validationError);
        }

        return new ConversionResult(glob, null);
    }

    private static @Nullable String findUnconvertibleConstruct(String pattern) {
        // Check for character classes (but not [^/] which we handle)
        if (pattern.matches(".*\\[[^^/].*\\].*") || pattern.matches(".*\\[\\^[^/].*\\].*")) {
            // Has character class that isn't [^/]
            if (!pattern.matches(".*\\[\\^/\\][*+].*") && pattern.matches(".*\\[.*\\].*")) {
                return "Pattern contains character class that cannot be converted to glob: " + pattern;
            }
        }

        // Check for alternation groups
        if (pattern.contains("(") && pattern.contains("|")) {
            return "Pattern contains alternation group that cannot be converted to glob: " + pattern;
        }

        // Check for quantifiers other than * and +
        if (pattern.matches(".*\\{\\d+,?\\d*}.*")) {
            return "Pattern contains bounded quantifier that cannot be converted to glob: " + pattern;
        }

        // Check for special character classes
        if (pattern.contains("\\d") || pattern.contains("\\D") ||
            pattern.contains("\\w") || pattern.contains("\\W") ||
            pattern.contains("\\s") || pattern.contains("\\S")) {
            return "Pattern contains special character class that cannot be converted to glob: " + pattern;
        }

        // Check for anchors (^ at start, $ at end outside of character class)
        if (pattern.startsWith("^") || pattern.endsWith("$")) {
            return "Pattern contains anchors that cannot be converted to glob: " + pattern;
        }

        // Check for lookahead/lookbehind
        if (pattern.contains("(?=") || pattern.contains("(?!") ||
            pattern.contains("(?<=") || pattern.contains("(?<!")) {
            return "Pattern contains lookahead/lookbehind that cannot be converted to glob: " + pattern;
        }

        // Check for backreferences
        if (pattern.matches(".*\\\\\\d+.*")) {
            return "Pattern contains backreference that cannot be converted to glob: " + pattern;
        }

        // Check for ? quantifier (zero or one)
        // But be careful not to match \? which is an escaped literal
        if (pattern.matches(".*[^\\\\]\\?.*") || pattern.startsWith("?")) {
            return "Pattern contains optional quantifier (?) that cannot be converted to glob: " + pattern;
        }

        return null;
    }

    private static @Nullable String validateGlob(String glob) {
        if (glob.isEmpty()) {
            return "Converted glob pattern is empty";
        }

        if (glob.endsWith("/")) {
            return "Glob pattern cannot end with /";
        }

        if (glob.contains("***")) {
            return "Glob pattern cannot contain more than two consecutive *";
        }

        if (glob.contains("//")) {
            return "Glob pattern cannot contain empty path segments (//)";
        }

        // Check for invalid globstar usage
        // Globstar (**) must be the entire path segment
        if (glob.matches(".*\\*\\*[^/].*") || glob.matches(".*[^/]\\*\\*.*")) {
            // Exception: **/*.ext is valid
            if (!glob.matches(".*\\*\\*/.*") && !glob.matches(".*/\\*\\*$") && !glob.equals("**")) {
                return "Globstar (**) must be a complete path segment";
            }
        }

        return null;
    }
}
