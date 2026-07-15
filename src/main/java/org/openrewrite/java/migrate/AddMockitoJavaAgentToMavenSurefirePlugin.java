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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.AddPropertyVisitor;
import org.openrewrite.maven.ChangePluginExecutions;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.DependencyInsight;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.AddOrUpdateChildTag;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class AddMockitoJavaAgentToMavenSurefirePlugin extends Recipe {

    private static final String MAVEN_PLUGINS_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_SUREFIRE_PLUGIN_ARTIFACT_ID = "maven-surefire-plugin";
    private static final String MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";

    @Language("xpath")
    private static final String MAVEN_DEPENDENCY_PLUGIN_EXECUTION_MATCHER = "/project/build/plugins/plugin[artifactId='maven-dependency-plugin']/executions/execution";

    @Language("xml")
    private static final String MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL = "<goal>properties</goal>";

    @Language("xml")
    private static final String MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG = "<execution><goals>"+ MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL + "</goals></execution>";

    @Getter
    final String displayName = "Add Mockito Java Agent to Maven Surefire Plugin";

    @Getter
    final String description = "Adds required configuration to specifically enable the Mockito/Bytebuddy Java agent in the Maven Surefire plugin for Java 21 compatibility.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new DependencyInsight("org.mockito", "mockito-core", "test", null, false), new MavenIsoVisitor<ExecutionContext>() {
            private final String CONFIGURATION_TAG_TEMPLATE = "<configuration><!--suppress MavenModelInspection --><argLine>%s</argLine></configuration>";

            private String getArgLineJavaAgentArgument() {
                String mockitoCoreVersion = getResolutionResult().getDependencies().getOrDefault(Scope.Test, emptyList()).stream()
                        .filter(dependency -> "org.mockito".equals(dependency.getGroupId()) && "mockito-core"
                                .equals(dependency.getArtifactId())).findFirst().map(ResolvedDependency::getVersion).get();

                return new LatestRelease(null).compare(null, mockitoCoreVersion, "5.14.0") >= 0 ?
                        "-javaagent:${org.mockito:mockito-core:jar}" : "-javaagent:${net.bytebuddy:byte-buddy-agent:jar}";
            }

            private Xml.Tag buildConfigurationTag(String argLineJavaAgentParam, boolean hasExistingArgLine) {
                return Xml.Tag.build(String.format(CONFIGURATION_TAG_TEMPLATE, hasExistingArgLine ? argLineJavaAgentParam : "@{argLine} " + argLineJavaAgentParam));
            }

            private void maybeAddMavenDependencyPluginWithPropertiesGoal() {
                Optional<Plugin> mavenDependencyPlugin = getResolutionResult().getPom().getPlugins().stream()
                        .filter(plugin -> isMavenPlugin(plugin, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID)).findFirst();

                if (!mavenDependencyPlugin.isPresent()) {
                    doAfterVisit(new AddPlugin(MAVEN_PLUGINS_GROUP_ID, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID, null, null, null,
                            "<executions>" + MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG + "</executions>", "**/pom.xml").getVisitor());
                } else if (mavenDependencyPlugin.get().getExecutions().isEmpty()) {
                    doAfterVisit(new ChangePluginExecutions(MAVEN_PLUGINS_GROUP_ID, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID, MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG).getVisitor());
                } else if (mavenDependencyPlugin.get().getExecutions().stream().noneMatch(execution -> execution.getGoals() != null)) {
                    doAfterVisit(new AddOrUpdateChildTag(MAVEN_DEPENDENCY_PLUGIN_EXECUTION_MATCHER, "<goals>" + MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL + "</goals>", false).getVisitor());
                } else if (!hasPropertiesGoal(mavenDependencyPlugin.get())) {
                    doAfterVisit(new AppendChildTagToParentVisitor(
                            new XPathMatcher(MAVEN_DEPENDENCY_PLUGIN_EXECUTION_MATCHER + "/goals"),
                            Xml.Tag.build(MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL)));
                }
            }

            private @Nullable String surefireArgLineWithAgent(List<Plugin> plugins) {
                String agentArgument = getArgLineJavaAgentArgument();
                return plugins.stream()
                        .filter(plugin -> isMavenPlugin(plugin, MAVEN_SUREFIRE_PLUGIN_ARTIFACT_ID))
                        .map(plugin -> plugin.getConfigurationStringValue("argLine"))
                        .filter(argLine -> argLine != null && argLine.contains(agentArgument))
                        .findFirst().orElse(null);
            }

            private boolean mavenDependencyPluginHasPropertiesGoal() {
                return getResolutionResult().getPom().getPlugins().stream()
                        .anyMatch(plugin -> isMavenPlugin(plugin, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID) && hasPropertiesGoal(plugin));
            }

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (surefireArgLineWithAgent(getResolutionResult().getPom().getPluginManagement()) != null) {
                    return document;
                }
                // Nothing to add when the surefire agent, the properties goal, and any `@{argLine}` property
                // are already present, whether declared here or inherited from a parent's `build/plugins` (#1164).
                String pluginArgLine = surefireArgLineWithAgent(getResolutionResult().getPom().getPlugins());
                if (pluginArgLine != null && mavenDependencyPluginHasPropertiesGoal() &&
                        (!pluginArgLine.contains("@{argLine}") || getResolutionResult().getPom().getProperties().containsKey("argLine"))) {
                    return document;
                }

                maybeAddMavenDependencyPluginWithPropertiesGoal();
                doAfterVisit(new AddPropertyVisitor("argLine", "", true));

                if (FindPlugin.find(document, "org.apache.maven.plugins", "maven-surefire-plugin").isEmpty()) {
                    doAfterVisit(new AddPlugin("org.apache.maven.plugins", "maven-surefire-plugin", null,
                            String.format(CONFIGURATION_TAG_TEMPLATE, "@{argLine} " + getArgLineJavaAgentArgument()), null,
                            null, "**/pom.xml").getVisitor());
                    return document;
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                // `isPluginTag` matches any `plugin` under a `plugins` element, so this covers both
                // `build/plugins` and `build/pluginManagement/plugins` declarations.
                if (!isPluginTag(MAVEN_PLUGINS_GROUP_ID, MAVEN_SUREFIRE_PLUGIN_ARTIFACT_ID)) {
                    return t;
                }

                String argLineJavaAgentParam = getArgLineJavaAgentArgument();
                //noinspection unchecked
                List<Content> pluginContents = (List<Content>) t.getContent();
                Optional<Xml.Tag> configuration = t.getChild("configuration");
                if (!configuration.isPresent()) {
                    return autoFormat(t.withContent(ListUtils.concat(pluginContents,
                            buildConfigurationTag(argLineJavaAgentParam, false))), ctx);
                }

                Xml.Tag config = configuration.get();
                //noinspection unchecked
                List<Content> configContents = (List<Content>) config.getContent();
                List<Xml.Tag> argLineTagChildren = config.getChildren("argLine");
                if (argLineTagChildren.isEmpty()) {
                    Xml.Tag updatedConfig = config.withContent(ListUtils.concatAll(configContents,
                            buildConfigurationTag(argLineJavaAgentParam, false).getContent()));
                    return autoFormat(t.withContent(ListUtils.map(pluginContents, c -> c == config ? updatedConfig : c)), ctx);
                }
                if (argLineTagChildren.size() == 1) {
                    Xml.Tag argLineTag = argLineTagChildren.get(0);
                    String existingArgLineValue = argLineTag.getValue().orElse("@{argLine}");

                    if (!existingArgLineValue.contains(argLineJavaAgentParam)) {
                        List<Content> nonArgLineTags = ListUtils.filter(configContents, content -> content != argLineTag);
                        Xml.Tag mergedConfiguration = buildConfigurationTag(existingArgLineValue + " " + argLineJavaAgentParam, true);
                        Xml.Tag updatedConfig = config.withContent(ListUtils.concatAll(nonArgLineTags, mergedConfiguration.getContent()));
                        return autoFormat(t.withContent(ListUtils.map(pluginContents, c -> c == config ? updatedConfig : c)), ctx);
                    }
                }
                // No surefire change needed: the single argLine already contains the agent, or
                // there are multiple <argLine> tags (an invalid configuration, since surefire's
                // argLine is single-valued) that we leave untouched.
                return t;
            }
        });
    }

    /**
     * Matches a plugin under the {@code org.apache.maven.plugins} group, tolerating an implied (omitted) groupId,
     * which Maven defaults to that group for {@code maven-*-plugin} declarations.
     */
    private static boolean isMavenPlugin(Plugin plugin, String artifactId) {
        return artifactId.equals(plugin.getArtifactId()) &&
                (plugin.getGroupId() == null || MAVEN_PLUGINS_GROUP_ID.equals(plugin.getGroupId()));
    }

    private static boolean hasPropertiesGoal(Plugin plugin) {
        return plugin.getExecutions().stream()
                .anyMatch(execution -> execution.getGoals() != null && execution.getGoals().contains("properties"));
    }

    @RequiredArgsConstructor
    private static class AppendChildTagToParentVisitor extends XmlIsoVisitor<ExecutionContext> {
        private final XPathMatcher parentXPathMatcher;
        private final Xml.Tag newChildTag;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (parentXPathMatcher.matches(getCursor()) &&
                    tag.getChildren(newChildTag.getName()).stream().noneMatch(child -> child.getValue().equals(newChildTag.getValue()))) {
                return autoFormat(AddToTagVisitor.addToTag(tag, newChildTag, getCursor()), ctx);
            }
            return super.visitTag(tag, ctx);
        }
    }
}
