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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddPluginDependency;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeImpsortForJava25 extends Recipe {
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    public String getDisplayName() {
        return "Use a Java 25 compatible parser for Impsort 1.13";
    }

    @Override
    public String getDescription() {
        return "Use JavaParser 3.28.2 with Impsort 1.13.0 to parse Java 25 yield statements. " +
               "Preserve newer parser versions, other Impsort versions, and unresolved declarations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindPlugin("net.revelc.code", "impsort-maven-plugin"),
                new MavenIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag plugin = super.visitTag(tag, ctx);
                        if (!isPluginTag("net.revelc.code", "impsort-maven-plugin") ||
                            !"1.13.0".equals(resolvedValue(plugin.getChildValue("version").orElse(null)))) {
                            return plugin;
                        }
                        Optional<Xml.Tag> dependencies = plugin.getChild("dependencies");
                        if (dependencies.isPresent()) {
                            for (Xml.Tag dependency : dependencies.get().getChildren()) {
                                String group = resolvedValue(dependency.getChildValue("groupId").orElse(null));
                                String artifact = resolvedValue(dependency.getChildValue("artifactId").orElse(null));
                                if (group == null || artifact == null || group.contains("${") || artifact.contains("${")) {
                                    // An opaque dependency could already provide the parser.
                                    return plugin;
                                }
                                if (!"com.github.javaparser".equals(group) || !"javaparser-core".equals(artifact)) {
                                    continue;
                                }
                                Optional<Xml.Tag> version = dependency.getChild("version");
                                String current = resolvedValue(version.flatMap(Xml.Tag::getValue).orElse(null));
                                if (current == null || !Semver.isVersion(current) ||
                                    new LatestRelease(null).compare(null, current, "3.28.2") >= 0) {
                                    return plugin;
                                }
                                // Keep a shared version property unchanged: this compatibility fix belongs
                                // to this plugin's dependency, not every use of that property in the project.
                                return (Xml.Tag) new ChangeTagValueVisitor<>(version.get(), "3.28.2")
                                        .visitNonNull(plugin, ctx, getCursor().getParentOrThrow());
                            }
                        }
                        if (hasOpaqueInheritedParser(getResolutionResult().getPom().getPlugins()) ||
                            hasOpaqueInheritedParser(getResolutionResult().getPom().getPluginManagement())) {
                            return plugin;
                        }
                        return (Xml.Tag) new AddPluginDependency("net.revelc.code", "impsort-maven-plugin",
                                "com.github.javaparser", "javaparser-core", "3.28.2")
                                .getVisitor().visitNonNull(plugin, ctx, getCursor().getParentOrThrow());
                    }

                    private boolean hasOpaqueInheritedParser(List<Plugin> plugins) {
                        for (Plugin plugin : plugins) {
                            if (!"net.revelc.code".equals(resolvedValue(plugin.getGroupId())) ||
                                !"impsort-maven-plugin".equals(resolvedValue(plugin.getArtifactId()))) {
                                continue;
                            }
                            for (Dependency dependency : plugin.getDependencies()) {
                                String group = resolvedValue(dependency.getGroupId());
                                String artifact = resolvedValue(dependency.getArtifactId());
                                if (group == null || artifact == null || group.contains("${") || artifact.contains("${")) {
                                    return true;
                                }
                                if ("com.github.javaparser".equals(group) && "javaparser-core".equals(artifact)) {
                                    String version = resolvedValue(dependency.getVersion());
                                    if (version == null || !Semver.isVersion(version)) {
                                        return true;
                                    }
                                }
                            }
                        }
                        return false;
                    }

                    private @Nullable String resolvedValue(@Nullable String value) {
                        if (value == null) {
                            return null;
                        }
                        Matcher property = PROPERTY.matcher(value);
                        if (property.matches()) {
                            Optional<Xml.Tag> profile = getCursor().getPathAsStream()
                                    .filter(Xml.Tag.class::isInstance).map(Xml.Tag.class::cast)
                                    .filter(t -> "profile".equals(t.getName())).findFirst();
                            value = profile.flatMap(t -> t.getChild("properties"))
                                    .flatMap(t -> t.getChildValue(property.group(1))).orElse(value);
                        }
                        return getResolutionResult().getPom().getValue(value);
                    }
                });
    }
}
