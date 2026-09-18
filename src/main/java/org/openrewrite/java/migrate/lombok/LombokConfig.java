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
package org.openrewrite.java.migrate.lombok;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.binary.Binary;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Shared handling of Lombok's configuration format: which files Lombok reads, how a line of one is read, and how
 * lines are written back to it.
 */
final class LombokConfig {

    static final String LOMBOK_CONFIG = "lombok.config";

    private static final String CONFIG_EXTENSION = ".config";

    /**
     * Tells Lombok to stop looking at parent directories, so it is scoped to the directory that declares it.
     */
    static final String STOP_BUBBLING = "config.stopBubbling";

    static final String STOP_BUBBLING_KEY = normalizeKey(STOP_BUBBLING);

    /**
     * The line grammar of {@code lombok.core.configuration.ConfigurationParser}; anything else is an invalid line.
     */
    private static final Pattern IMPORT = Pattern.compile("import\\s+(.+)");

    private static final Pattern CLEAR = Pattern.compile("clear\\s+([^=]+)");

    private static final Pattern ASSIGNMENT = Pattern.compile("(\\S+?)\\s*([+-]?=)\\s*(.*)");

    private LombokConfig() {
    }

    enum Kind {
        BLANK, COMMENT, IMPORT, CLEAR, ASSIGN, ADD, REMOVE, INVALID
    }

    /**
     * One line of a {@code lombok.config}, classified the way Lombok classifies it and kept as it was written, so
     * that merging never has to reformat it.
     */
    static class Line {
        final Kind kind;

        final String text;

        final @Nullable String key;

        final @Nullable String value;

        /**
         * The key as Lombok compares it; {@code null} for a line that names no key, an {@code import} included, as it
         * names a path.
         */
        final @Nullable String normalizedKey;

        Line(Kind kind, String text, @Nullable String key, @Nullable String value) {
            this.kind = kind;
            this.text = text;
            this.key = key;
            this.value = value;
            this.normalizedKey = key == null || kind == Kind.IMPORT ? null : normalizeKey(key);
        }

        /**
         * The directive as Lombok compares it, so that {@code key=value}, {@code key = value} and {@code KEY = value}
         * dedupe against each other; {@code null} for a line that is not a directive.
         */
        @Nullable
        String canonical() {
            switch (kind) {
                case ASSIGN:
                    return normalizedKey + "=" + value;
                case ADD:
                    return normalizedKey + "+=" + value;
                case REMOVE:
                    return normalizedKey + "-=" + value;
                default:
                    return null;
            }
        }
    }

    static boolean isLombokConfig(SourceFile sourceFile) {
        return isConfig(sourceFile) && LOMBOK_CONFIG.equals(sourceFile.getSourcePath().getFileName().toString());
    }

    /**
     * Whether a file is one Lombok reads, of its own accord or because another file imports it. Only a file parsed as
     * text can be read.
     */
    static boolean isConfig(SourceFile sourceFile) {
        return !(sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) &&
                sourceFile.getSourcePath().getFileName().toString().endsWith(CONFIG_EXTENSION);
    }

    /**
     * A key as Lombok compares it. {@code ConfigurationKey.registeredKeys()} is ordered by
     * {@link String#CASE_INSENSITIVE_ORDER}, so {@code lombok.val.flagUsage} and {@code Lombok.Val.FlagUsage} are the
     * same key.
     */
    static String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    static List<Line> parse(String text) {
        List<Line> lines = new ArrayList<>();
        for (String line : text.split("\r?\n")) {
            lines.add(classify(line.trim()));
        }
        return lines;
    }

    /**
     * Reads one line the way Lombok reads it, trying {@code import} and {@code clear} before an assignment so that
     * neither is mistaken for a key.
     */
    static Line classify(String trimmed) {
        if (trimmed.isEmpty()) {
            return new Line(Kind.BLANK, trimmed, null, null);
        }
        if (trimmed.charAt(0) == '#') {
            return new Line(Kind.COMMENT, trimmed, null, null);
        }
        Matcher anImport = IMPORT.matcher(trimmed);
        if (anImport.matches()) {
            return new Line(Kind.IMPORT, trimmed, anImport.group(1).trim(), null);
        }
        Matcher clear = CLEAR.matcher(trimmed);
        if (clear.matches()) {
            return new Line(Kind.CLEAR, trimmed, clear.group(1).trim(), null);
        }
        Matcher assignment = ASSIGNMENT.matcher(trimmed);
        if (assignment.matches()) {
            Kind kind = "+=".equals(assignment.group(2)) ? Kind.ADD :
                    "-=".equals(assignment.group(2)) ? Kind.REMOVE : Kind.ASSIGN;
            return new Line(kind, trimmed, assignment.group(1), assignment.group(3).trim());
        }
        return new Line(Kind.INVALID, trimmed, null, null);
    }

    /**
     * The file an {@code import} names, resolved against the directory of the file that declares it the way Lombok
     * resolves it; {@code null} when the line does not name a path this file system can name.
     */
    static @Nullable Path resolveImport(Path config, Line line) {
        try {
            Path path = Paths.get(requireNonNull(line.key));
            Path directory = config.getParent();
            return (directory == null ? path : directory.resolve(path)).normalize();
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * The given lines with every {@code import} replaced, where it stands, by the lines of the file it names, so that
     * a later line still has the last word. {@code null} when an imported file is not among the sources or the
     * imports form a cycle, as there is then no telling what the file declares.
     */
    static @Nullable List<Line> expandImports(Path config, List<Line> lines, Map<Path, List<Line>> configs,
                                              Set<Path> beingRead) {
        if (!beingRead.add(config)) {
            return null;
        }
        List<Line> expanded = new ArrayList<>();
        for (Line line : lines) {
            if (line.kind != Kind.IMPORT) {
                expanded.add(line);
                continue;
            }
            Path path = resolveImport(config, line);
            List<Line> imported = path == null ? null : configs.get(path);
            if (imported == null) {
                return null;
            }
            List<Line> importedExpanded = expandImports(path, imported, configs, beingRead);
            if (importedExpanded == null) {
                return null;
            }
            expanded.addAll(importedExpanded);
        }
        beingRead.remove(config);
        return expanded;
    }

    static boolean declaresStopBubbling(List<Line> lines) {
        for (Line line : lines) {
            if (STOP_BUBBLING_KEY.equals(line.normalizedKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The given text with the given lines appended, preserving its line endings and trailing newline.
     */
    static String append(String text, List<String> additions) {
        String newLine = text.contains("\r\n") ? "\r\n" : "\n";
        boolean endsWithNewLine = text.isEmpty() || text.endsWith("\n");
        StringBuilder merged = new StringBuilder(text);
        if (!endsWithNewLine) {
            merged.append(newLine);
        }
        merged.append(String.join(newLine, additions));
        if (endsWithNewLine) {
            merged.append(newLine);
        }
        return merged.toString();
    }
}
