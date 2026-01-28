/*
 * Copyright 2026 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Migrates GraalVM native-image resource-config.json files from the legacy regex pattern
 * format (JDK 21 and earlier) to the new glob pattern format (JDK 23+).
 * <p>
 * Old format:
 * <pre>
 * {
 *   "resources": {
 *     "includes": [{"pattern": ".*\\.txt"}],
 *     "excludes": [{"pattern": ".*\\.bak"}]
 *   }
 * }
 * </pre>
 * <p>
 * New format:
 * <pre>
 * {
 *   "resources": [
 *     {"glob": "**&#47;*.txt"}
 *   ]
 * }
 * </pre>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateGraalVMResourceConfig extends Recipe {

    String displayName = "Migrate GraalVM resource-config.json to glob patterns";

    String description = "Migrates GraalVM native-image resource-config.json files from the legacy regex pattern format " +
            "(JDK 21 and earlier) to the new glob pattern format (JDK 23+). " +
            "Converts `pattern` entries to `glob` entries and restructures the format. " +
            "Note: `excludes` are no longer supported in the new format and will be removed.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindSourceFiles("**/resource-config.json"),
                new ResourceConfigVisitor()
        );
    }

    private static class ResourceConfigVisitor extends JsonIsoVisitor<ExecutionContext> {

        @Override
        public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
            Json.Document doc = super.visitDocument(document, ctx);
            JsonValue value = doc.getValue();

            if (!(value instanceof Json.JsonObject)) {
                return doc;
            }

            Json.JsonObject root = (Json.JsonObject) value;
            Json.Member resourcesMember = findMember(root, "resources");

            if (resourcesMember == null) {
                return doc;
            }

            // Check if already in new format (resources is an array)
            if (resourcesMember.getValue() instanceof Json.Array) {
                return doc;
            }

            // Check if in old format (resources is an object with includes/excludes)
            if (!(resourcesMember.getValue() instanceof Json.JsonObject)) {
                return doc;
            }

            Json.JsonObject resourcesObj = (Json.JsonObject) resourcesMember.getValue();
            Json.Member includesMember = findMember(resourcesObj, "includes");
            Json.Member excludesMember = findMember(resourcesObj, "excludes");

            // If there's no includes or excludes, this might not be the old format
            if (includesMember == null && excludesMember == null) {
                return doc;
            }

            // Note: excludes are no longer supported in the new format and will be silently dropped

            // Convert includes to new format
            List<JsonRightPadded<JsonValue>> newResourceEntries = new ArrayList<>();
            boolean hasUnconvertiblePatterns = false;

            if (includesMember != null && includesMember.getValue() instanceof Json.Array) {
                Json.Array includesArray = (Json.Array) includesMember.getValue();
                for (JsonRightPadded<JsonValue> paddedEntry : includesArray.getPadding().getValues()) {
                    JsonValue entry = paddedEntry.getElement();
                    if (entry instanceof Json.JsonObject) {
                        Json.JsonObject entryObj = (Json.JsonObject) entry;
                        ConvertedEntry converted = convertPatternEntry(entryObj);
                        if (converted != null) {
                            newResourceEntries.add(paddedEntry.withElement(converted.entry));
                            if (converted.hasWarning) {
                                hasUnconvertiblePatterns = true;
                            }
                        }
                    }
                }
            }

            // Build new resources array
            Json.Array newResourcesArray = new Json.Array(
                    Tree.randomId(),
                    resourcesObj.getPrefix(),
                    Markers.EMPTY,
                    newResourceEntries
            );

            // Replace resources member value
            Json.Member newResourcesMember = resourcesMember.withValue(newResourcesArray);

            // Update root object
            Json.JsonObject newRoot = root.getPadding().withMembers(
                    ListUtils.map(root.getPadding().getMembers(), paddedMember ->
                            paddedMember.getElement() == resourcesMember ?
                                    paddedMember.withElement(newResourcesMember) :
                                    paddedMember
                    )
            );
            doc = doc.withValue(newRoot);

            if (hasUnconvertiblePatterns) {
                doc = SearchResult.found(doc, "Some patterns could not be automatically converted to glob format");
            }

            return doc;
        }

        private static final class ConvertedEntry {
            final Json.JsonObject entry;
            final boolean hasWarning;

            ConvertedEntry(Json.JsonObject entry, boolean hasWarning) {
                this.entry = entry;
                this.hasWarning = hasWarning;
            }
        }

        private @Nullable ConvertedEntry convertPatternEntry(Json.JsonObject entryObj) {
            Json.Member patternMember = findMember(entryObj, "pattern");
            if (patternMember == null) {
                // Already in glob format or unknown format, keep as is
                return new ConvertedEntry(entryObj, false);
            }

            // Get the pattern value
            String patternValue = getLiteralValue(patternMember.getValue());
            if (patternValue == null) {
                return new ConvertedEntry(entryObj, false);
            }

            // Convert regex to glob
            ConversionResult result = convertRegexToGlob(patternValue);

            if (!result.isSuccessful()) {
                // Mark the entry for manual review but keep the original pattern
                return new ConvertedEntry(
                        SearchResult.found(entryObj, result.warningMessage),
                        true
                );
            }

            // Create new entry with glob key using ListUtils.map
            Json.JsonObject newEntry = entryObj.getPadding().withMembers(
                    ListUtils.map(entryObj.getPadding().getMembers(), paddedMember -> {
                        Json member = paddedMember.getElement();
                        if (member instanceof Json.Member) {
                            Json.Member m = (Json.Member) member;
                            if ("pattern".equals(getKeyName(m))) {
                                // Replace pattern with glob
                                return paddedMember.withElement(createGlobMember(m, result.glob));
                            }
                        }
                        return paddedMember;
                    })
            );
            return new ConvertedEntry(newEntry, false);
        }

        private Json.Member createGlobMember(Json.Member patternMember, String globValue) {
            // Change the key from "pattern" to "glob"
            Json.Literal newKey;
            if (patternMember.getKey() instanceof Json.Literal) {
                Json.Literal oldKey = (Json.Literal) patternMember.getKey();
                String newKeySource = oldKey.getSource().replace("pattern", "glob");
                newKey = oldKey.withSource(newKeySource);
            } else {
                newKey = new Json.Literal(
                        Tree.randomId(),
                        patternMember.getKey().getPrefix(),
                        Markers.EMPTY,
                        "\"glob\"",
                        "glob"
                );
            }

            // Change the value
            Json.Literal newValue;
            if (patternMember.getValue() instanceof Json.Literal) {
                Json.Literal oldValue = (Json.Literal) patternMember.getValue();
                String newValueSource = "\"" + escapeJsonString(globValue) + "\"";
                newValue = oldValue.withSource(newValueSource).withValue(globValue);
            } else {
                newValue = new Json.Literal(
                        Tree.randomId(),
                        patternMember.getValue().getPrefix(),
                        Markers.EMPTY,
                        "\"" + escapeJsonString(globValue) + "\"",
                        globValue
                );
            }

            return patternMember
                    .getPadding().withKey(patternMember.getPadding().getKey().withElement(newKey))
                    .withValue(newValue);
        }

        private static Json.@Nullable Member findMember(Json.JsonObject obj, String keyName) {
            for (JsonRightPadded<Json> paddedMember : obj.getPadding().getMembers()) {
                if (paddedMember.getElement() instanceof Json.Member) {
                    Json.Member member = (Json.Member) paddedMember.getElement();
                    if (keyName.equals(getKeyName(member))) {
                        return member;
                    }
                }
            }
            return null;
        }

        private static @Nullable String getKeyName(Json.Member member) {
            if (member.getKey() instanceof Json.Literal) {
                Object value = ((Json.Literal) member.getKey()).getValue();
                return value instanceof String ? (String) value : null;
            }
            if (member.getKey() instanceof Json.Identifier) {
                return ((Json.Identifier) member.getKey()).getName();
            }
            return null;
        }

        private static @Nullable String getLiteralValue(JsonValue value) {
            if (value instanceof Json.Literal) {
                Object v = ((Json.Literal) value).getValue();
                return v instanceof String ? (String) v : null;
            }
            return null;
        }

        private static String escapeJsonString(String s) {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        sb.append(c);
                }
            }
            return sb.toString();
        }

        // Regex to glob conversion logic integrated from RegexToGlobConverter

        private static final class ConversionResult {
            final @Nullable String glob;
            final @Nullable String warningMessage;

            ConversionResult(@Nullable String glob, @Nullable String warningMessage) {
                this.glob = glob;
                this.warningMessage = warningMessage;
            }

            boolean isSuccessful() {
                return glob != null;
            }
        }

        private static ConversionResult convertRegexToGlob(String regex) {
            if (StringUtils.isNullOrEmpty(regex)) {
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
                if (!glob.matches(".*\\*\\*/.*") && !glob.matches(".*/\\*\\*$") && !"**".equals(glob)) {
                    return "Globstar (**) must be a complete path segment";
                }
            }

            return null;
        }
    }
}
