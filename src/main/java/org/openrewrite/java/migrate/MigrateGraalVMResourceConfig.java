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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonRightPadded;
import org.openrewrite.json.tree.JsonValue;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Override
    public String getDisplayName() {
        return "Migrate GraalVM resource-config.json to glob patterns";
    }

    @Override
    public String getDescription() {
        return "Migrates GraalVM native-image resource-config.json files from the legacy regex pattern format " +
               "(JDK 21 and earlier) to the new glob pattern format (JDK 23+). " +
               "Converts `pattern` entries to `glob` entries and restructures the format. " +
               "Note: `excludes` are no longer supported in the new format and will be removed.";
    }

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
                        ConvertedEntry converted = convertPatternEntry(entryObj, ctx);
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
                    UUID.randomUUID(),
                    resourcesObj.getPrefix(),
                    Markers.EMPTY,
                    newResourceEntries
            );

            // Replace resources member value
            Json.Member newResourcesMember = resourcesMember.withValue(newResourcesArray);

            // Update root object
            List<JsonRightPadded<Json>> newRootMembers = new ArrayList<>();
            for (JsonRightPadded<Json> paddedMember : root.getPadding().getMembers()) {
                if (paddedMember.getElement() == resourcesMember) {
                    newRootMembers.add(paddedMember.withElement(newResourcesMember));
                } else {
                    newRootMembers.add(paddedMember);
                }
            }

            Json.JsonObject newRoot = root.getPadding().withMembers(newRootMembers);
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

        private @Nullable ConvertedEntry convertPatternEntry(Json.JsonObject entryObj, ExecutionContext ctx) {
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
            RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(patternValue);

            if (!result.isSuccessful()) {
                // Mark the entry for manual review but keep the original pattern
                return new ConvertedEntry(
                        SearchResult.found(entryObj, result.warningMessage()),
                        true
                );
            }

            // Create new entry with glob key
            List<JsonRightPadded<Json>> newMembers = new ArrayList<>();

            for (JsonRightPadded<Json> paddedMember : entryObj.getPadding().getMembers()) {
                Json member = paddedMember.getElement();
                if (member instanceof Json.Member) {
                    Json.Member m = (Json.Member) member;
                    String key = getKeyName(m);
                    if ("pattern".equals(key)) {
                        // Replace pattern with glob
                        Json.Member globMember = createGlobMember(m, result.glob());
                        newMembers.add(paddedMember.withElement(globMember));
                    } else {
                        // Keep other members (like "module")
                        newMembers.add(paddedMember);
                    }
                }
            }

            Json.JsonObject newEntry = entryObj.getPadding().withMembers(newMembers);
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
                        UUID.randomUUID(),
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
                        UUID.randomUUID(),
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
            } else if (member.getKey() instanceof Json.Identifier) {
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
    }
}
