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
 * The parts of Lombok's configuration format that more than one recipe has to agree on: how a
 * {@code lombok.config} is told apart from a file that merely looks like one, how a line of it is read, and how lines
 * are written back to it.
 */
final class LombokConfig {

    static final String LOMBOK_CONFIG = "lombok.config";

    private static final String CONFIG_EXTENSION = ".config";

    /**
     * {@code config.stopBubbling} tells Lombok to stop looking at parent directories, so it is scoped to the directory
     * that declares it and must not be hoisted.
     */
    static final String STOP_BUBBLING = "config.stopBubbling";

    /**
     * {@code config.stopBubbling} as it is compared, since Lombok reads a key without regard to its case.
     */
    static final String STOP_BUBBLING_KEY = normalizeKey(STOP_BUBBLING);

    /**
     * The line grammar of {@code lombok.core.configuration.ConfigurationParser}: a line is a comment starting with
     * {@code #}, an {@code import} of another file, a {@code clear} of a list, or an assignment with {@code =},
     * {@code +=} or {@code -=}. Anything else Lombok rejects as an invalid line.
     */
    private static final Pattern IMPORT = Pattern.compile("import\\s+(.+)");

    private static final Pattern CLEAR = Pattern.compile("clear\\s+([^=]+)");

    private static final Pattern ASSIGNMENT = Pattern.compile("(\\S+?)\\s*([+-]?=)\\s*(.*)");

    private LombokConfig() {
    }

    /**
     * How Lombok reads one line of a {@code lombok.config}.
     */
    enum Kind {
        BLANK, COMMENT, IMPORT, CLEAR, ASSIGN, ADD, REMOVE, INVALID
    }

    /**
     * One line of a {@code lombok.config}, classified the way Lombok classifies it and kept as it was written so that
     * merging never has to reformat it.
     */
    static class Line {
        final Kind kind;

        final String text;

        /**
         * The key the line applies to, or {@code null} for a line that does not name one.
         */
        final @Nullable String key;

        /**
         * The value assigned, added or removed, or {@code null} for a line that does not carry one.
         */
        final @Nullable String value;

        /**
         * The key as Lombok compares it, or {@code null} for a line that does not name one. An {@code import} names a
         * path rather than a key, so it does not have one either.
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
         * The directive stripped of the whitespace Lombok ignores and with its key spelled the way Lombok compares
         * it, so that {@code key=value}, {@code key = value} and {@code KEY = value} are recognized as the same
         * directive. {@code null} for a line that is not a directive, and so cannot be deduplicated against one.
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
     * Whether a file is one Lombok reads, either of its own accord or because another file imports it. Only the
     * text of such a file can be read, so one that was not parsed as text is not one of them.
     */
    static boolean isConfig(SourceFile sourceFile) {
        return !(sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) &&
                sourceFile.getSourcePath().getFileName().toString().endsWith(CONFIG_EXTENSION);
    }

    /**
     * A key as Lombok compares it. {@code ConfigurationKey.registeredKeys()} is ordered by
     * {@link String#CASE_INSENSITIVE_ORDER}, so {@code lombok.val.flagUsage} and {@code Lombok.Val.FlagUsage} name
     * the same key to Lombok and have to name the same key here.
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
     * The file an {@code import} names, resolved the way Lombok resolves it: against the directory of the file that
     * declares it. {@code null} when the line does not name something this file system can name, and so does not
     * name a file that could be found among the sources.
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
     * The given lines with every {@code import} replaced by the lines of the file it names, the way Lombok reads
     * them: inline, where the import stands, so that a later line still has the last word over an imported one.
     * {@code null} when a file an import names is not among the sources, or when the imports lead back around to a
     * file already being read, as there is then no telling what the file as a whole declares.
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

    /**
     * Whether {@code config.stopBubbling} is declared, whatever value it is assigned; a file that turns it off is
     * doing so deliberately and is left alone.
     */
    static boolean declaresStopBubbling(List<Line> lines) {
        for (Line line : lines) {
            if (STOP_BUBBLING_KEY.equals(line.normalizedKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The given file with the given lines appended. Appending rather than rewriting the file keeps its comments next
     * to the directives they document, keeps an {@code import} at the top of the file where Lombok requires it, keeps
     * {@code clear} and {@code -=} in the order they were written in, and leaves anything Lombok cannot read for
     * someone to look at rather than quietly dropping it.
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
