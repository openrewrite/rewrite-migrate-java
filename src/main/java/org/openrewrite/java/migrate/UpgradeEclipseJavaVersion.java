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
import org.openrewrite.Recipe;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeEclipseJavaVersion extends Recipe {
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
            "levels in JDT preferences. Preserve newer versions, custom VM names, and other project settings.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
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
                if (!classpath && !path.endsWith(JDT_PREFS)) {
                    return source;
                }

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
        };
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
            Matcher environment = EXECUTION_ENVIRONMENT.matcher(path.substring(JRE_CONTAINER.length()));
            if (!environment.matches()) {
                return attr;
            }
            String current = environment.group(1);
            if (majorVersion(current.substring(current.indexOf('-') + 1)) >= version) {
                return attr;
            }
            String label = (version <= 5 ? "J2SE-" : "JavaSE-") + compilerLevel();
            return attr.withValue(attr.getValue().withValue(JRE_CONTAINER + label + environment.group(2)));
        }));
    }

    private Properties.Entry upgradePreference(Properties.Entry entry) {
        if (!COMPILER_KEYS.contains(entry.getKey())) {
            return entry;
        }
        Properties.Value value = entry.getValue();
        Matcher level = COMPILER_LEVEL.matcher(value.getSource());
        return level.matches() && majorVersion(level.group(2)) < version ?
                entry.withValue(value.withText(level.group(1) + compilerLevel() + level.group(3))) : entry;
    }

    private static int majorVersion(String level) {
        return Integer.parseInt(level.startsWith("1.") ? level.substring(2) : level);
    }

    private String compilerLevel() {
        return version <= 8 ? "1." + version : version.toString();
    }
}
