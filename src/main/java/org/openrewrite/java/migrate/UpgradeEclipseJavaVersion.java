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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Parser;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.properties.PropertiesParser;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.text.PlainText;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeEclipseJavaVersion extends ScanningRecipe<Set<Path>> {
    private static final Path MANIFEST = Paths.get("META-INF", "MANIFEST.MF");
    private static final Path JDT_PREFS = Paths.get(".settings", "org.eclipse.jdt.core.prefs");
    private static final String JRE_CONTAINER = "org.eclipse.jdt.launching.JRE_CONTAINER/" +
            "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/";
    private static final Pattern EXECUTION_ENVIRONMENT =
            Pattern.compile("(JRE-1\\.1|J2SE-1\\.[2-5]|JavaSE-(?:1\\.[6-8]|9|[1-9][0-9]{1,8}))(/?)");
    private static final Pattern COMPILER_LEVEL = Pattern.compile("([ \\t]*)(1\\.[1-8]|[1-9][0-9]{0,8})([ \\t\\r]*)");
    private static final List<String> COMPILER_KEYS = Arrays.asList(
            "org.eclipse.jdt.core.compiler.source",
            "org.eclipse.jdt.core.compiler.compliance",
            "org.eclipse.jdt.core.compiler.codegen.targetPlatform");

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "25")
    Integer version;

    String displayName = "Upgrade the Eclipse project Java version";

    String description = "Upgrade existing Java execution environments in Eclipse `.classpath` files and the Java compiler " +
            "levels in JDT preferences. Preserve newer versions, custom VM names, and other project settings. Align existing PDE runtime requirements in projects whose Java levels change.";

    @Override
    public Set<Path> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<Path> upgradedProjects) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    SourceFile source = (SourceFile) tree;
                    Path path = source.getSourcePath();
                    boolean classpath = ".classpath".equals(path.getFileName().toString());
                    if ((classpath || path.endsWith(JDT_PREFS)) && upgradeMetadata(source, classpath) != source) {
                        upgradedProjects.add(projectDirectory(path, classpath));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<Path> upgradedProjects) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile source = (SourceFile) tree;
                Path path = source.getSourcePath();
                boolean classpath = ".classpath".equals(path.getFileName().toString());
                if (classpath || path.endsWith(JDT_PREFS)) {
                    return upgradeMetadata(source, classpath);
                }
                if (source instanceof PlainText && path.endsWith(MANIFEST) &&
                        upgradedProjects.contains(projectDirectory(path, false))) {
                    PlainText manifest = (PlainText) source;
                    return manifest.withText(EclipseManifestJavaVersion.upgrade(manifest.getText(),
                            UpgradeEclipseJavaVersion.this::upgradeEnvironment, UpgradeEclipseJavaVersion.this::upgradeCompilerLevel));
                }
                return source;
            }
        };
    }

    private static Path projectDirectory(Path path, boolean classpath) {
        Path directory = classpath ? path.getParent() : path.getParent().getParent();
        return directory == null ? Paths.get("") : directory;
    }

    private SourceFile upgradeMetadata(SourceFile source, boolean classpath) {
        if (source instanceof PlainText) {
            // Hidden Eclipse files can enter the LST as text. Keep the original source type and metadata.
            PlainText text = (PlainText) source;
            Parser parser = classpath ? XmlParser.builder().build() : PropertiesParser.builder().build();
            return parser.parse(text.getText()).findFirst()
                    .filter(parsed -> parsed.printAll().equals(text.getText()))
                    .map(parsed -> text.withText(upgrade(parsed, classpath).printAll()))
                    .orElse(text);
        }
        return upgrade(source, classpath);
    }

    @SuppressWarnings("unchecked")
    private SourceFile upgrade(SourceFile source, boolean classpath) {
        if (classpath && source instanceof Xml.Document) {
            Xml.Document document = (Xml.Document) source;
            Xml.Tag root = document.getRoot();
            if (root != null && "classpath".equals(root.getName())) {
                return document.withRoot(root.withContent(ListUtils.map((List<Content>) root.getContent(), content ->
                        content instanceof Xml.Tag ? upgradeClasspathEntry((Xml.Tag) content) : content)));
            }
        } else if (!classpath && source instanceof Properties.File) {
            Properties.File prefs = (Properties.File) source;
            return prefs.withContent(ListUtils.map(prefs.getContent(), content ->
                    content instanceof Properties.Entry ? upgradePreference((Properties.Entry) content) : content));
        }
        return source;
    }

    private Xml.Tag upgradeClasspathEntry(Xml.Tag tag) {
        if (!"classpathentry".equals(tag.getName()) || tag.getAttributes().stream().noneMatch(
                attr -> "kind".equals(attr.getKeyAsString()) && "con".equals(attr.getValueAsString()))) {
            return tag;
        }

        return tag.withAttributes(ListUtils.map(tag.getAttributes(), attr -> {
            String path = attr.getValueAsString();
            if (!"path".equals(attr.getKeyAsString()) || !path.startsWith(JRE_CONTAINER)) {
                return attr;
            }
            String current = path.substring(JRE_CONTAINER.length());
            String updated = upgradeEnvironment(current);
            return current.equals(updated) ? attr : attr.withValue(attr.getValue().withValue(JRE_CONTAINER + updated));
        }));
    }

    private Properties.Entry upgradePreference(Properties.Entry entry) {
        if (!COMPILER_KEYS.contains(entry.getKey())) {
            return entry;
        }
        Properties.Value value = entry.getValue();
        String current = value.getSource();
        String updated = upgradeCompilerLevel(current);
        return current.equals(updated) ? entry : entry.withValue(value.withText(updated));
    }

    private String upgradeEnvironment(String current) {
        Matcher environment = EXECUTION_ENVIRONMENT.matcher(current);
        if (!environment.matches()) {
            return current;
        }
        String label = environment.group(1);
        if (majorVersion(label.substring(label.indexOf('-') + 1)) >= version) {
            return current;
        }
        return (version <= 5 ? "J2SE-" : "JavaSE-") + compilerLevel() + environment.group(2);
    }

    private String upgradeCompilerLevel(String current) {
        Matcher level = COMPILER_LEVEL.matcher(current);
        return level.matches() && majorVersion(level.group(2)) < version ?
                level.group(1) + compilerLevel() + level.group(3) : current;
    }

    private static int majorVersion(String level) {
        return Integer.parseInt(level.startsWith("1.") ? level.substring(2) : level);
    }

    private String compilerLevel() {
        return version <= 8 ? "1." + version : version.toString();
    }
}
