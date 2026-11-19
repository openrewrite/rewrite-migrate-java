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

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EclipseManifestJavaVersion {
    private static final String REQUIRED_ENVIRONMENT = "Bundle-RequiredExecutionEnvironment";
    private static final Pattern MAIN_SECTION_END = Pattern.compile("(?m)^$");
    private static final Pattern HEADERS = Pattern.compile("(?im)^(" + REQUIRED_ENVIRONMENT +
            "|Require-Capability): ([^\\r\\n]*(?:(?:\\r\\n|\\r|\\n) [^\\r\\n]*)*)");
    private static final Pattern FOLD = Pattern.compile("(?:\\r\\n|\\r|\\n) ");
    private static final Pattern ENVIRONMENT = Pattern.compile("[^,]+");
    // A comma in a quoted LDAP filter or attribute does not separate capability clauses.
    private static final Pattern CLAUSE = Pattern.compile("(?:[^,\"\\\\]|\\\\.|\"(?:[^\"\\\\]|\\\\.)*\")+");
    private static final Pattern EE_NAMESPACE = Pattern.compile("^\\s*osgi\\.ee\\s*;");
    private static final Pattern FILTER = Pattern.compile(";\\s*filter\\s*:=\\s*\"([^\"]*)\"");
    private static final String JAVA_SE = "\\(\\s*osgi\\.ee\\s*=\\s*JavaSE\\s*\\)";
    private static final String VERSION = "\\(\\s*version\\s*(?:>=|=)\\s*(1\\.[1-8]|[1-9][0-9]*)(?:\\.0){0,2}\\s*\\)";
    private static final Pattern JAVA_SE_FILTER = Pattern.compile("\\(\\s*&\\s*(?:" +
            JAVA_SE + "\\s*" + VERSION + "|" + VERSION + "\\s*" + JAVA_SE + ")\\s*\\)");

    private EclipseManifestJavaVersion() {
    }

    static String upgrade(String manifest, UnaryOperator<String> environment, UnaryOperator<String> level) {
        Matcher blankLine = MAIN_SECTION_END.matcher(manifest);
        Matcher headers = HEADERS.matcher(manifest);
        if (blankLine.find()) {
            headers.region(0, blankLine.start());
        }
        StringBuffer updated = new StringBuffer(manifest.length());
        while (headers.find()) {
            String physical = headers.group(2);
            String logical = FOLD.matcher(physical).replaceAll("");
            String value = REQUIRED_ENVIRONMENT.equalsIgnoreCase(headers.group(1)) ?
                    upgradeEnvironments(physical, logical, environment) : upgradeCapabilities(physical, logical, level);
            headers.appendReplacement(updated, Matcher.quoteReplacement(headers.group(1) + ": " + value));
        }
        return headers.appendTail(updated).toString();
    }

    private static String upgradeEnvironments(String physical, String logical, UnaryOperator<String> environment) {
        StringBuilder result = new StringBuilder(physical);
        Matcher tokens = ENVIRONMENT.matcher(logical);
        int delta = 0;
        while (tokens.find()) {
            String token = tokens.group().trim();
            int start = tokens.start() + tokens.group().indexOf(token);
            delta += replace(result, physical, start, start + token.length(), token, environment, delta);
        }
        return result.toString();
    }

    private static String upgradeCapabilities(String physical, String logical, UnaryOperator<String> level) {
        StringBuilder result = new StringBuilder(physical);
        Matcher clauses = CLAUSE.matcher(logical);
        int delta = 0;
        while (clauses.find()) {
            String clause = clauses.group();
            if (!EE_NAMESPACE.matcher(clause).find()) {
                continue;
            }
            Matcher filter = FILTER.matcher(clause);
            if (filter.find()) {
                Matcher version = JAVA_SE_FILTER.matcher(filter.group(1));
                if (version.matches()) {
                    int group = version.group(1) != null ? 1 : 2;
                    int offset = clauses.start() + filter.start(1);
                    delta += replace(result, physical, offset + version.start(group), offset + version.end(group),
                            version.group(group), level, delta);
                }
            }
        }
        return result.toString();
    }

    private static int replace(StringBuilder result, String physical, int start, int end, String current,
                               UnaryOperator<String> upgrade, int delta) {
        String updated = upgrade.apply(current);
        if (updated.equals(current)) {
            return 0;
        }
        int from = physicalOffset(physical, start);
        int to = physicalOffset(physical, end - 1) + 1;
        String rawToken = physical.substring(from, to);
        Matcher folds = FOLD.matcher(rawToken);
        StringBuilder replacement = new StringBuilder(updated);
        int inserted = 0;
        while (folds.find()) {
            replacement.insert(Math.min(folds.start() - inserted, updated.length()) + inserted, folds.group());
            inserted += folds.group().length();
        }
        result.replace(from + delta, to + delta, replacement.toString());
        return replacement.length() - (to - from);
    }

    private static int physicalOffset(String text, int logicalOffset) {
        Matcher folds = FOLD.matcher(text);
        int offset = logicalOffset;
        while (folds.find() && folds.start() <= offset) {
            offset += folds.group().length();
        }
        return offset;
    }
}
