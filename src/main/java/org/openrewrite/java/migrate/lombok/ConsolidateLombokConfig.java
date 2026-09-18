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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.java.migrate.lombok.LombokConfig.LOMBOK_CONFIG;
import static org.openrewrite.java.migrate.lombok.LombokConfig.STOP_BUBBLING_KEY;
import static org.openrewrite.java.migrate.lombok.LombokConfig.append;
import static org.openrewrite.java.migrate.lombok.LombokConfig.expandImports;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isConfig;
import static org.openrewrite.java.migrate.lombok.LombokConfig.isLombokConfig;
import static org.openrewrite.java.migrate.lombok.LombokConfig.parse;
import static org.openrewrite.java.migrate.lombok.LombokConfig.resolveImport;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConsolidateLombokConfig extends ScanningRecipe<ConsolidateLombokConfig.Accumulator> {

    String displayName = "Consolidate `lombok.config` files";

    String description = "Merge the directives of every nested `lombok.config` into the root `lombok.config` and " +
            "delete the nested files, so that a project has a single place where Lombok is configured. Directives " +
            "are appended to the root file; what it already declares, itself or through an `import`, is left as " +
            "written and not repeated. Note that hoisting a directive widens its scope from the directory that " +
            "declared it to the whole project, so a directive only some directories can satisfy, such as " +
            "`lombok.val.flagUsage = error`, will start to apply to all of them. A nested file is left in place when " +
            "moving its directives would change what Lombok does: when it declares `config.stopBubbling`, `import`, " +
            "`clear` or `-=`, when a `lombok.config` between it and the root would outrank the root once the " +
            "directive moved there, when another `lombok.config` imports it, or when no Java source sits at or below " +
            "it. No changes are made at all when two files assign conflicting values to the same key, or when the " +
            "root imports a file that is not among the sources.";

    public static class Accumulator {
        @Nullable
        Path rootConfig;

        List<LombokConfig.Line> rootLines = new ArrayList<>();

        /**
         * Every nested config, keyed by path so that they are merged in a predictable order.
         */
        final SortedMap<Path, List<LombokConfig.Line>> nested = new TreeMap<>();

        /**
         * The directory of every Java source, so that a config Lombok never reads can be told apart from one it does.
         */
        final Set<Path> javaSourceDirectories = new HashSet<>();

        /**
         * Every file an {@code import} could name, so that what a config pulls in can be read rather than guessed at.
         */
        final Map<Path, List<LombokConfig.Line>> configs = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                Path path = sourceFile.getSourcePath();
                if (isJavaSource(path)) {
                    Path directory = path.getParent();
                    acc.javaSourceDirectories.add(directory == null ? Paths.get("") : directory);
                    return sourceFile;
                }
                if (!isConfig(sourceFile)) {
                    return sourceFile;
                }
                List<LombokConfig.Line> lines = parse(PlainTextParser.convert(sourceFile).getText());
                acc.configs.put(path, lines);
                if (isLombokConfig(sourceFile)) {
                    if (path.getParent() == null) {
                        acc.rootConfig = path;
                        acc.rootLines = lines;
                    } else {
                        acc.nested.put(path, lines);
                    }
                }
                return sourceFile;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        if (acc.rootConfig == null) {
            return TreeVisitor.noop();
        }
        SortedMap<Path, List<LombokConfig.Line>> hoistable = hoistable(configuration(acc), importedConfigs(acc));
        if (hoistable.isEmpty()) {
            return TreeVisitor.noop();
        }
        List<LombokConfig.Line> rootLines = expandImports(acc.rootConfig, acc.rootLines, acc.configs, new HashSet<>());
        if (rootLines == null || hasConflictingDirectives(rootLines, hoistable)) {
            return TreeVisitor.noop();
        }
        List<String> additions = additions(rootLines, hoistable);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (!isLombokConfig(sourceFile)) {
                    return sourceFile;
                }
                if (hoistable.containsKey(sourceFile.getSourcePath())) {
                    return null;
                }
                if (additions.isEmpty() || !sourceFile.getSourcePath().equals(acc.rootConfig)) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);
                return plainText.withText(append(plainText.getText(), additions));
            }
        };
    }

    /**
     * Judged on the name, so that a source Lombok compiles but this recipe did not parse counts all the same.
     */
    private static boolean isJavaSource(Path path) {
        return path.toString().endsWith(".java");
    }

    /**
     * The nested configs that configure Lombok for this project. Lombok reads a config by walking up from the
     * directory of the Java file it is compiling, so one with no Java source at or below it, such as a fixture under
     * {@code src/test/resources}, is not configuration at all and is left where it is. When there is no Java source
     * to be seen there is no layout to judge against, so each config is taken at its word.
     */
    private static SortedMap<Path, List<LombokConfig.Line>> configuration(Accumulator acc) {
        if (acc.javaSourceDirectories.isEmpty()) {
            return acc.nested;
        }
        SortedMap<Path, List<LombokConfig.Line>> configuration = new TreeMap<>();
        for (Map.Entry<Path, List<LombokConfig.Line>> config : acc.nested.entrySet()) {
            if (governsJavaSources(config.getKey(), acc.javaSourceDirectories)) {
                configuration.put(config.getKey(), config.getValue());
            }
        }
        return configuration;
    }

    /**
     * Whether a Java source lives in the directory that declares the given config, or in one below it.
     */
    private static boolean governsJavaSources(Path config, Set<Path> javaSourceDirectories) {
        Path directory = requireNonNull(config.getParent());
        for (Path javaSourceDirectory : javaSourceDirectories) {
            if (javaSourceDirectory.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The nested configs whose directives can be moved into the root. A directory that stops bubbling puts
     * everything under it out of reach of the root, and a config that stays behind can shadow a directive hoisted
     * out of a directory below it, which is why configs are decided shallowest first: whether one stays behind is
     * settled before the configs it could shadow are considered.
     */
    private static SortedMap<Path, List<LombokConfig.Line>> hoistable(SortedMap<Path, List<LombokConfig.Line>> nested,
                                                                      Set<Path> imported) {
        Set<Path> stopBubblingDirectories = new HashSet<>();
        for (Map.Entry<Path, List<LombokConfig.Line>> config : nested.entrySet()) {
            if (stopsBubbling(config.getValue())) {
                stopBubblingDirectories.add(config.getKey().getParent());
            }
        }

        SortedMap<Path, List<LombokConfig.Line>> hoistable = new TreeMap<>();
        for (Map.Entry<Path, List<LombokConfig.Line>> config : shallowestFirst(nested)) {
            Path path = config.getKey();
            if (isHoistable(config.getValue()) &&
                    !imported.contains(path) &&
                    !isUnder(path, stopBubblingDirectories) &&
                    !isShadowed(config.getValue(), leftInPlaceAbove(path, nested, hoistable))) {
                hoistable.put(path, config.getValue());
            }
        }
        return hoistable;
    }

    /**
     * The given configs shallowest first, which is the order Lombok reads them in, and which path order is not, as
     * that puts {@code a/b/lombok.config} before {@code a/lombok.config}. Configs of equal depth keep path order.
     */
    private static List<Map.Entry<Path, List<LombokConfig.Line>>> shallowestFirst(
            SortedMap<Path, List<LombokConfig.Line>> configs) {
        List<Map.Entry<Path, List<LombokConfig.Line>>> shallowestFirst = new ArrayList<>(configs.entrySet());
        shallowestFirst.sort(comparingInt(config -> config.getKey().getNameCount()));
        return shallowestFirst;
    }

    /**
     * The configs left in place between the given one and the root; the root itself is not among them.
     */
    private static List<List<LombokConfig.Line>> leftInPlaceAbove(Path path,
                                                                 SortedMap<Path, List<LombokConfig.Line>> nested,
                                                                 SortedMap<Path, List<LombokConfig.Line>> hoistable) {
        List<List<LombokConfig.Line>> leftInPlace = new ArrayList<>();
        for (Path directory = requireNonNull(path.getParent()).getParent(); directory != null; directory = directory.getParent()) {
            Path config = directory.resolve(LOMBOK_CONFIG);
            List<LombokConfig.Line> lines = nested.get(config);
            if (lines != null && !hoistable.containsKey(config)) {
                leftInPlace.add(lines);
            }
        }
        return leftInPlace;
    }

    /**
     * Whether one of the given configs would outrank a directive hoisted out of the given file. Lombok takes the
     * first file that speaks about a key as it walks up, so a file left standing between that directory and the root
     * now has the last word. A line saying exactly what the hoisted file says resolves to the same thing and is not
     * shadowing it; an {@code import} pulls in directives this recipe cannot see and is assumed to shadow.
     */
    private static boolean isShadowed(List<LombokConfig.Line> lines, List<List<LombokConfig.Line>> leftInPlaceAbove) {
        Set<String> keys = new HashSet<>();
        Set<String> canonical = new HashSet<>();
        for (LombokConfig.Line line : lines) {
            if (line.normalizedKey != null) {
                keys.add(line.normalizedKey);
                canonical.add(line.canonical());
            }
        }

        for (List<LombokConfig.Line> above : leftInPlaceAbove) {
            for (LombokConfig.Line line : above) {
                if (line.kind == LombokConfig.Kind.IMPORT) {
                    return true;
                }
                if (line.normalizedKey != null &&
                        keys.contains(line.normalizedKey) &&
                        !canonical.contains(line.canonical())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isUnder(Path file, Set<Path> directories) {
        for (Path directory = file.getParent(); directory != null; directory = directory.getParent()) {
            if (directories.contains(directory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The files that are imported; deleting one would leave the file that imports it pointing at nothing.
     */
    private static Set<Path> importedConfigs(Accumulator acc) {
        Set<Path> imported = new HashSet<>();
        for (Map.Entry<Path, List<LombokConfig.Line>> config : acc.configs.entrySet()) {
            for (LombokConfig.Line line : config.getValue()) {
                if (line.kind == LombokConfig.Kind.IMPORT) {
                    Path path = resolveImport(config.getKey(), line);
                    if (path != null) {
                        imported.add(path);
                    }
                }
            }
        }
        return imported;
    }

    /**
     * Whether a config stops bubbling. Only an explicit {@code false} keeps bubbling on; a value that cannot be read
     * as a boolean is treated as stopping it, so a directory is left alone rather than hoisted on a guess.
     */
    private static boolean stopsBubbling(List<LombokConfig.Line> lines) {
        boolean stopsBubbling = false;
        for (LombokConfig.Line line : lines) {
            if (STOP_BUBBLING_KEY.equals(line.normalizedKey)) {
                stopsBubbling = !"false".equalsIgnoreCase(line.value);
            }
        }
        return stopsBubbling;
    }

    /**
     * Whether a nested config can be merged into the root, judged on its own contents. {@code config.stopBubbling}
     * has opted its directory out of the root configuration, an {@code import} resolves relative to the file that
     * declares it, and a {@code clear} or {@code -=} only means anything in relation to the additions it undoes. A
     * line Lombok cannot read is left where it is rather than thrown away, as is a file with nothing to hoist.
     */
    private static boolean isHoistable(List<LombokConfig.Line> lines) {
        boolean hoistable = false;
        for (LombokConfig.Line line : lines) {
            switch (line.kind) {
                case IMPORT:
                case CLEAR:
                case REMOVE:
                case INVALID:
                    return false;
                case ASSIGN:
                case ADD:
                    if (STOP_BUBBLING_KEY.equals(line.normalizedKey)) {
                        return false;
                    }
                    hoistable = true;
                    break;
                default:
                    break;
            }
        }
        return hoistable;
    }

    /**
     * Whether the files that would be merged disagree, so that there is no single configuration to consolidate to.
     * Two {@code =} assignments of the same key to different values conflict outright, whereas several {@code +=} and
     * {@code -=} of one key are expected to coexist. A directive also conflicts with a root {@code clear} or
     * {@code -=} of the same key, as appending it after that line would put back what the root took away.
     */
    private static boolean hasConflictingDirectives(List<LombokConfig.Line> rootLines,
                                                    SortedMap<Path, List<LombokConfig.Line>> hoistable) {
        Set<String> undoneByRoot = new HashSet<>();
        for (LombokConfig.Line line : rootLines) {
            if (line.kind == LombokConfig.Kind.CLEAR || line.kind == LombokConfig.Kind.REMOVE) {
                undoneByRoot.add(line.normalizedKey);
            }
        }

        List<List<LombokConfig.Line>> files = new ArrayList<>();
        files.add(rootLines);
        files.addAll(hoistable.values());

        Map<String, String> assignments = new HashMap<>();
        for (List<LombokConfig.Line> lines : files) {
            for (Map.Entry<String, String> assignment : lastAssignments(lines).entrySet()) {
                String previous = assignments.put(assignment.getKey(), assignment.getValue());
                if (previous != null && !previous.equals(assignment.getValue())) {
                    return true;
                }
            }
        }

        for (List<LombokConfig.Line> lines : hoistable.values()) {
            for (LombokConfig.Line line : lines) {
                if (undoneByRoot.contains(line.normalizedKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * What each key of one file is assigned, taking the last assignment the way Lombok does.
     */
    private static Map<String, String> lastAssignments(List<LombokConfig.Line> lines) {
        Map<String, String> assignments = new LinkedHashMap<>();
        for (LombokConfig.Line line : lines) {
            if (line.kind == LombokConfig.Kind.ASSIGN) {
                assignments.put(line.normalizedKey, line.value);
            }
        }
        return assignments;
    }

    /**
     * Where in a file each key is assigned last, which is the only assignment worth carrying over: one the file
     * itself supersedes would become the last word once appended to the root.
     */
    private static Map<String, Integer> lastAssignmentIndexes(List<LombokConfig.Line> lines) {
        Map<String, Integer> indexes = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            LombokConfig.Line line = lines.get(i);
            if (line.kind == LombokConfig.Kind.ASSIGN) {
                indexes.put(line.normalizedKey, i);
            }
        }
        return indexes;
    }

    /**
     * The lines to append to the root {@code lombok.config}: whatever each nested file has to say that the root does
     * not, shallowest file first, so that a {@code +=} of a directory above another still adds to the list before it
     * does. A key is appended at most once, as by this point every file being merged agrees on what it is assigned,
     * whereas a {@code +=} is appended once per distinct value. Comments come along with the directive they precede
     * and are dropped along with a directive that is not carried over; a comment at the end of a file, documenting
     * no directive, is carried over as it stands, as the file is about to be deleted.
     */
    private static List<String> additions(List<LombokConfig.Line> rootLines,
                                          SortedMap<Path, List<LombokConfig.Line>> hoistable) {
        Set<String> present = new HashSet<>();
        for (LombokConfig.Line line : rootLines) {
            String canonical = line.canonical();
            if (canonical != null) {
                present.add(canonical);
            }
        }
        Set<String> assigned = new HashSet<>(lastAssignments(rootLines).keySet());

        List<String> additions = new ArrayList<>();
        for (Map.Entry<Path, List<LombokConfig.Line>> config : shallowestFirst(hoistable)) {
            List<LombokConfig.Line> lines = config.getValue();
            Map<String, Integer> lastAssignment = lastAssignmentIndexes(lines);
            List<String> comments = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                LombokConfig.Line line = lines.get(i);
                if (line.kind == LombokConfig.Kind.COMMENT) {
                    comments.add(line.text);
                    continue;
                }
                if (line.kind == LombokConfig.Kind.BLANK) {
                    continue;
                }
                if (isCarriedOver(line, i, lastAssignment, present, assigned)) {
                    additions.addAll(comments);
                    additions.add(line.text);
                }
                comments.clear();
            }
            additions.addAll(comments);
        }
        return additions;
    }

    /**
     * Whether the given line has to be appended for the root to say what the file says, remembering it as said so
     * that a later file does not repeat it.
     */
    private static boolean isCarriedOver(LombokConfig.Line line, int index, Map<String, Integer> lastAssignment,
                                         Set<String> present, Set<String> assigned) {
        if (line.kind == LombokConfig.Kind.ASSIGN) {
            Integer last = lastAssignment.get(line.normalizedKey);
            return last != null && last == index && assigned.add(line.normalizedKey);
        }
        String canonical = line.canonical();
        return canonical != null && present.add(canonical);
    }
}
