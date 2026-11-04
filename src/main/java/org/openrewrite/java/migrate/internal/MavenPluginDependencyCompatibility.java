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
package org.openrewrite.java.migrate.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddPluginDependency;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MavenPluginDependencyCompatibility {
    private MavenPluginDependencyCompatibility() {
    }

    public static TreeVisitor<?, ExecutionContext> visitor(String pluginGroup, String pluginArtifact, String affectedVersion,
                                                          String dependencyGroup, String dependencyArtifact, String minimumVersion,
                                                          boolean resolveManagedVersion) {
        return Preconditions.check(new FindPlugin(pluginGroup, pluginArtifact), new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag plugin = super.visitTag(tag, ctx);
                if (!isPluginTag(pluginGroup, pluginArtifact) ||
                    !resolveManagedVersion && !plugin.getChild("version").isPresent()) {
                    return plugin;
                }
                ResolvedPom pom;
                try {
                    pom = declarationPom(ctx);
                } catch (MavenDownloadingException e) {
                    return e.warn(plugin);
                }
                if (!pluginGroup.equals(pom.getValue(plugin.getChildValue("groupId").orElse("org.apache.maven.plugins"))) ||
                    !pluginArtifact.equals(pom.getValue(plugin.getChildValue("artifactId").orElse(null))) ||
                    !affectedVersion.equals(pluginVersion(plugin, pom))) {
                    return plugin;
                }
                Optional<Xml.Tag> dependencies = plugin.getChild("dependencies");
                if (dependencies.isPresent()) {
                    for (Xml.Tag dependency : dependencies.get().getChildren()) {
                        String group = pom.getValue(dependency.getChildValue("groupId").orElse(null));
                        String artifact = pom.getValue(dependency.getChildValue("artifactId").orElse(null));
                        if (opaque(group) || opaque(artifact)) {
                            // An opaque dependency could already provide the parser.
                            return plugin;
                        }
                        if (!dependencyGroup.equals(group) || !dependencyArtifact.equals(artifact)) {
                            continue;
                        }
                        Optional<Xml.Tag> version = dependency.getChild("version");
                        String current = pom.getValue(version.flatMap(Xml.Tag::getValue).orElse(null));
                        if (current == null || !Semver.isVersion(current) ||
                            new LatestRelease(null).compare(null, current, minimumVersion) >= 0) {
                            return plugin;
                        }
                        // This fix belongs to this plugin's dependency, not every use of a shared property.
                        return (Xml.Tag) new ChangeTagValueVisitor<>(version.get(), minimumVersion)
                                .visitNonNull(plugin, ctx, getCursor().getParentOrThrow());
                    }
                }
                if (hasOpaqueInheritedDependency(pom.getPlugins(), pom) ||
                    hasOpaqueInheritedDependency(pom.getPluginManagement(), pom)) {
                    return plugin;
                }
                return (Xml.Tag) new AddPluginDependency(pluginGroup, pluginArtifact,
                        dependencyGroup, dependencyArtifact, minimumVersion)
                        .getVisitor().visitNonNull(plugin, ctx, getCursor().getParentOrThrow());
            }

            private @Nullable String pluginVersion(Xml.Tag plugin, ResolvedPom pom) {
                Optional<Xml.Tag> version = plugin.getChild("version");
                if (version.isPresent()) {
                    return pom.getValue(version.get().getValue().orElse(null));
                }
                if (!resolveManagedVersion) {
                    return null;
                }
                String effective = findVersion(pom.getPlugins(), pom);
                return effective == null ? findVersion(pom.getPluginManagement(), pom) : effective;
            }

            private @Nullable String findVersion(List<Plugin> plugins, ResolvedPom pom) {
                for (Plugin plugin : plugins) {
                    if (pluginGroup.equals(pom.getValue(plugin.getGroupId())) &&
                        pluginArtifact.equals(pom.getValue(plugin.getArtifactId()))) {
                        String version = pom.getValue(plugin.getVersion());
                        if (version != null) {
                            return version;
                        }
                    }
                }
                return null;
            }

            private boolean hasOpaqueInheritedDependency(List<Plugin> plugins, ResolvedPom pom) {
                for (Plugin plugin : plugins) {
                    if (!pluginGroup.equals(pom.getValue(plugin.getGroupId())) ||
                        !pluginArtifact.equals(pom.getValue(plugin.getArtifactId()))) {
                        continue;
                    }
                    for (Dependency dependency : plugin.getDependencies()) {
                        String group = pom.getValue(dependency.getGroupId());
                        String artifact = pom.getValue(dependency.getArtifactId());
                        if (opaque(group) || opaque(artifact)) {
                            return true;
                        }
                        if (dependencyGroup.equals(group) && dependencyArtifact.equals(artifact)) {
                            String version = pom.getValue(dependency.getVersion());
                            if (version == null || !Semver.isVersion(version)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            private boolean opaque(@Nullable String value) {
                return value == null || value.contains("${");
            }

            private ResolvedPom declarationPom(ExecutionContext ctx) throws MavenDownloadingException {
                MavenResolutionResult mrr = getResolutionResult();
                ResolvedPom pom = mrr.getPom();
                Optional<String> profile = getCursor().getPathAsStream()
                        .filter(Xml.Tag.class::isInstance).map(Xml.Tag.class::cast)
                        .filter(t -> "profile".equals(t.getName())).findFirst().flatMap(t -> t.getChildValue("id"));
                if (!profile.isPresent() || pom.getRequested().getProfiles().isEmpty()) {
                    return pom;
                }
                // Resolve only this declaration's profile. A future version in an inactive profile
                // must not be mistaken for the old version managed in the original model.
                String profileId = profile.get();
                List<String> activeProfiles = new ArrayList<>(mrr.getActiveProfiles());
                activeProfiles.removeIf(it -> {
                    String id = it.trim();
                    return (id.startsWith("!") || id.startsWith("-")) && profileId.equals(id.substring(1).trim());
                });
                if (!activeProfiles.contains(profileId)) {
                    activeProfiles.add(profileId);
                }
                MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), ctx,
                        MavenExecutionContextView.view(ctx).effectiveSettings(mrr), activeProfiles);
                return pom.getRequested().resolve(activeProfiles, downloader, ctx);
            }
        });
    }
}
