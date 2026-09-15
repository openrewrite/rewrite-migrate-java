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
            "delete the nested files, so that a project has a single place where Lombok is configured. Directives are " +
            "appended to the root file, leaving whatever it already contains exactly as it was written, and " +
            "directives the root already declares are dropped rather than repeated. Note that hoisting a directive " +
            "widens its scope from the directory that declared it to the whole project, so a directive that only " +
            "some directories can satisfy, such as `lombok.val.flagUsage = error`, will start to apply to all of " +
            "them. A nested file is left in place when its directives cannot be moved without changing what Lombok " +
            "does: `config.stopBubbling` opts its directory, and everything under it, out of the root " +
            "configuration, `import` resolves relative to the file that declares it, and `clear` and `-=` depend on " +
            "the order they appear in relative to the additions they undo. A nested file is left in place as well " +
            "when a `lombok.config` between it and the root speaks about one of the same directives, or imports a " +
            "file that may, because that file would outrank the root once the directive moved there. " +
            "A `lombok.config` another one imports is left in place as well, as deleting it would leave that " +
            "import pointing at nothing, and so is " +
            "a `lombok.config` with no Java source at or below it, such as a fixture under " +
            "`src/test/resources`, as Lombok never reads it. " +
            "What the root `lombok.config` imports is read as part of it, so that a directive the imported file " +
            "already declares is neither repeated nor overridden. " +
            "No changes are made when two files assign conflicting values to the same directive, as there " +
            "is no way to tell which value the consolidated configuration should keep, nor when the root imports a " +
            "file that is not among the sources, as there is then no telling what that file declares.";

    public static class Accumulator {
        @Nullable
        Path rootConfig;

        List<LombokConfig.Line> rootLines = new ArrayList<>();

        /**
         * Every nested config, keyed by path so that they are merged in a predictable order. Whether one can be
         * hoisted depends on the configs of the directories above it, so that is decided once scanning has finished.
         */
        final SortedMap<Path, List<LombokConfig.Line>> nested = new TreeMap<>();

        /**
         * The directory of every Java source, so that a {@code lombok.config} can be told apart from a file that
         * merely shares its name.
         */
        final Set<Path> javaSourceDirectories = new HashSet<>();

        /**
         * Every file an {@code import} could name, keyed by path, so that what a config pulls in can be read rather
         * than guessed at. Lombok resolves an import to a file by path and puts no requirement on its name, but the
         * files it reads of its own accord are named {@code lombok.config}, and the convention an import follows is
         * to give the file it names the same extension.
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
     * Whether a file is a Java source, judged on its name so that one Lombok compiles but this recipe did not parse
     * counts all the same.
     */
    private static boolean isJavaSource(Path path) {
        return path.toString().endsWith(".java");
    }

    /**
     * The nested configs that configure Lombok for this project. Lombok reads a configuration by walking up from the
     * directory of the Java file it is compiling, so a config only has a say over directories that hold Java sources:
     * one with no Java source at or below it, such as a fixture under {@code src/test/resources}, is not
     * configuration at all and is left where it is rather than hoisted and deleted. When there is no Java source to
     * be seen there is no layout to judge the configs against, so each is taken at its word.
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
     * Whether Lombok would ever read the given config, which is to say whether a Java source lives in the directory
     * that declares it or in a directory below it.
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
     * The nested configs whose directives can be moved into the root. Beyond what each file says about itself, a
     * directory that stops bubbling puts everything under it out of reach of the root, so a config below such a
     * directory cannot be hoisted either: Lombok would never read the root file on its behalf. A config that stays
     * behind keeps its say over the directories under it and so can shadow a directive hoisted out of one of them,
     * which is why configs are decided shallowest first: whether one stays behind is settled before the configs it
     * could shadow are considered. A config another file imports stays where it is as well, as deleting it would
     * leave that file importing nothing.
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
     * The given configs shallowest first, which is the order Lombok reads them in: it resolves a key by walking up
     * from the directory it is compiling, and applies a list directive from the root down, so a file nearer the root
     * has its say first. Path order is not that order, as it puts {@code a/b/lombok.config} before
     * {@code a/lombok.config}. Configs of the same depth keep the path order they came in.
     */
    private static List<Map.Entry<Path, List<LombokConfig.Line>>> shallowestFirst(
            SortedMap<Path, List<LombokConfig.Line>> configs) {
        List<Map.Entry<Path, List<LombokConfig.Line>>> shallowestFirst = new ArrayList<>(configs.entrySet());
        shallowestFirst.sort(comparingInt(config -> config.getKey().getNameCount()));
        return shallowestFirst;
    }

    /**
     * The configs above the given one that this recipe leaves in place, and so keep standing between it and the root.
     * The root is not among them, as its directives are the ones the nested file is being merged into.
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
     * Whether one of the given configs would outrank a directive hoisted out of the given file. Lombok resolves a key
     * by walking up from the directory it is compiling and taking the first file that speaks about it, so a file left
     * standing between that directory and the root now has the last word: an assignment there takes precedence over
     * the root, a {@code clear} or a {@code -=} there undoes what the root adds, and a {@code +=} there is applied
     * after the root's rather than before it. A line that says exactly what the file being hoisted says is not
     * shadowing it, as either of them resolves to the same thing. An {@code import} pulls in directives this recipe
     * cannot see, so a file declaring one is assumed to shadow whatever is below it.
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

    /**
     * Whether the given file lives in one of the given directories, or in a directory below one of them.
     */
    private static boolean isUnder(Path file, Set<Path> directories) {
        for (Path directory = file.getParent(); directory != null; directory = directory.getParent()) {
            if (directories.contains(directory)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The files that are imported. Deleting one of these would leave the file that imports it pointing at nothing,
     * which Lombok reports as an error, so they are not hoisted.
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
     * Whether a {@code lombok.config} tells Lombok to stop looking at parent directories. Only an explicit
     * {@code false} keeps bubbling on; a value that cannot be read as a boolean is treated as stopping it, so that a
     * directory is left alone rather than hoisted on a guess. Lombok takes the last declaration of a key.
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
     * Whether a nested {@code lombok.config} can be merged into the root, judged on its own contents. A directory
     * that declares {@code config.stopBubbling} has opted out of the root configuration, so deleting its file would
     * silently opt it back in. An {@code import} resolves relative to the file that declares it, and Lombok only
     * accepts one at the top of a file. A {@code clear} or a {@code -=} only means anything in relation to the
     * additions it undoes, so it cannot be appended to a file that was written without it. A line Lombok cannot read
     * is left where it is rather than thrown away. A file with nothing to hoist is left alone, as there is nothing to
     * consolidate.
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
     * Two plain {@code =} assignments of the same key to different values conflict outright; {@code +=} and {@code -=}
     * add to and remove from a list, so several of them for the same key are expected to coexist. A directive also
     * conflicts with a root {@code clear} or {@code -=} of the same key, because appending it after that line would
     * put back what the root deliberately took away.
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
     * What each key of one file is assigned, resolved the way Lombok resolves it: a key assigned more than once in
     * the same file takes the value it is assigned last, which is not a conflict.
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
     * Where in a file each key is assigned last. Lombok takes the last assignment of a key in a file, so that is the
     * only one worth carrying over: an assignment the file itself supersedes would become the last word once
     * appended to the root, and so change what the key resolves to.
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
     * does. A key is appended at most once, taking the assignment that wins in the file it comes from, as by this
     * point every file that is being merged agrees on what the key is assigned. A {@code +=} is appended once per
     * distinct value, as each adds to the same list rather than replacing what came before. A nested file's comments
     * come along with the directive they precede, in the order they were written, so that they stay with what they
     * document; a comment whose directive the root already declares is dropped along with it, rather than left
     * behind documenting nothing. A comment at the end of a file, documenting no directive, is carried over as it
     * stands, as the file it was written in is about to be deleted.
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
     * Whether the given line of a nested file has to be appended to the root for the root to say what the file says,
     * remembering it as said so that a later file does not repeat it.
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
