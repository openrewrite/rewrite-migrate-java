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
import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.AddPropertyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.DependencyInsight;
import org.openrewrite.maven.search.FindPlugin;
import org.openrewrite.maven.trait.MavenPlugin;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

public class AddMockitoJavaAgentToMavenSurefirePlugin extends Recipe {

    private static final String MAVEN_PLUGINS_GROUP_ID = "org.apache.maven.plugins";
    private static final String MAVEN_SUREFIRE_PLUGIN_ARTIFACT_ID = "maven-surefire-plugin";
    private static final String MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID = "maven-dependency-plugin";

    private static final String PROPERTIES_GOAL = "properties";

    @Language("xml")
    private static final String MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG =
            "<execution><id>get-mockito-agent-path</id><goals><goal>" + PROPERTIES_GOAL + "</goal></goals></execution>";

    @Getter
    final String displayName = "Add Mockito Java Agent to Maven Surefire Plugin";

    @Getter
    final String description = "Mockito attaches its Byte Buddy agent to the running JVM at test time, which the JDK has " +
            "warned about since Java 21 and intends to disallow. This recipe instead loads the agent up front through the " +
            "Maven Surefire plugin, adding the `maven-dependency-plugin` `properties` goal to resolve the agent jar path, " +
            "to silence the warning and stay ahead of the JDK change.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new DependencyInsight("org.mockito", "mockito-core", "test", null, false), new MavenIsoVisitor<ExecutionContext>() {
            // The newline before the comment keeps auto-format from attaching it to a preceding sibling as a trailing comment
            private final String CONFIGURATION_TAG_TEMPLATE = "<configuration>\n<!--suppress MavenModelInspection --><argLine>%s</argLine></configuration>";

            private String getArgLineJavaAgentArgument() {
                String mockitoCoreVersion = getResolutionResult().getDependencies().getOrDefault(Scope.Test, emptyList()).stream()
                        .filter(dependency -> "org.mockito".equals(dependency.getGroupId()) && "mockito-core"
                                .equals(dependency.getArtifactId())).findFirst().map(ResolvedDependency::getVersion).get();

                return new LatestRelease(null).compare(null, mockitoCoreVersion, "5.14.0") >= 0 ?
                        "-javaagent:${org.mockito:mockito-core:jar}" : "-javaagent:${net.bytebuddy:byte-buddy-agent:jar}";
            }

            private Xml.Tag buildConfigurationTag(String argLineJavaAgentParam, boolean hasExistingArgLine) {
                return Xml.Tag.build(String.format(CONFIGURATION_TAG_TEMPLATE, hasExistingArgLine ? argLineJavaAgentParam : newArgLineValue(argLineJavaAgentParam)));
            }

            private String newArgLineValue(String argLineJavaAgentParam) {
                return usesRuntimeArgLineProperty() ? "@{argLine} " + argLineJavaAgentParam : argLineJavaAgentParam;
            }

            /**
             * The {@code @{argLine}} late replacement, and the empty {@code argLine} property it needs to not be
             * passed to the JVM verbatim, are only worth adding when something actually supplies that property.
             */
            private boolean usesRuntimeArgLineProperty() {
                return getResolutionResult().getPom().getProperties().containsKey("argLine") ||
                        getResolutionResult().getPom().getPlugins().stream().anyMatch(AddMockitoJavaAgentToMavenSurefirePlugin::isJacocoPlugin) ||
                        declaresJacocoPlugin(getCursor().firstEnclosingOrThrow(Xml.Document.class));
            }

            private @Nullable String surefireArgLineWithAgent(List<Plugin> plugins) {
                String agentArgument = getArgLineJavaAgentArgument();
                return plugins.stream()
                        .filter(plugin -> isMavenPlugin(plugin, MAVEN_SUREFIRE_PLUGIN_ARTIFACT_ID))
                        .map(plugin -> plugin.getConfigurationStringValue("argLine"))
                        .filter(argLine -> argLine != null && argLine.contains(agentArgument))
                        .findFirst().orElse(null);
            }

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (surefireArgLineWithAgent(getResolutionResult().getPom().getPluginManagement()) != null) {
                    return document;
                }
                // Nothing to add when the surefire agent, the properties goal, and any `@{argLine}` property
                // are already present, whether declared here or inherited from a parent's `build/plugins` (#1164).
                String pluginArgLine = surefireArgLineWithAgent(getResolutionResult().getPom().getPlugins());
                boolean mavenDependencyPluginRunsPropertiesGoalAtDefaultPhase = getResolutionResult().getPom().getPlugins().stream()
                        .anyMatch(plugin -> isMavenPlugin(plugin, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID) &&
                                plugin.getExecutions().stream()
                                        .anyMatch(execution -> execution.getPhase() == null &&
                                                execution.getGoals() != null && execution.getGoals().contains(PROPERTIES_GOAL)));
                if (pluginArgLine != null && mavenDependencyPluginRunsPropertiesGoalAtDefaultPhase &&
                        (!pluginArgLine.contains("@{argLine}") || getResolutionResult().getPom().getProperties().containsKey("argLine"))) {
                    return document;
                }

                if (!mavenDependencyPluginRunsPropertiesGoalAtDefaultPhase) {
                    // AddPlugin no-ops when the plugin exists; the visitor below appends only to an already declared plugin.
                    doAfterVisit(new AddPlugin(MAVEN_PLUGINS_GROUP_ID, MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID, null, null, null,
                            "<executions>" + MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG + "</executions>", "**/pom.xml").getVisitor());
                    doAfterVisit(new AddPropertiesGoalExecutionVisitor());
                }

                if (usesRuntimeArgLineProperty()) {
                    doAfterVisit(new AddPropertyVisitor("argLine", "", true));
                }

                if (FindPlugin.find(document, "org.apache.maven.plugins", "maven-surefire-plugin").isEmpty()) {
                    doAfterVisit(new AddPlugin("org.apache.maven.plugins", "maven-surefire-plugin", null,
                            String.format(CONFIGURATION_TAG_TEMPLATE, newArgLineValue(getArgLineJavaAgentArgument())), null,
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
                    String existingArgLineValue = argLineTag.getValue().orElse("");

                    if (!existingArgLineValue.contains(argLineJavaAgentParam)) {
                        // An empty argLine carries nothing to preserve, so it is filled in as if it were absent
                        String mergedArgLine = StringUtils.isBlank(existingArgLineValue) ?
                                newArgLineValue(argLineJavaAgentParam) :
                                existingArgLineValue + " " + argLineJavaAgentParam;
                        List<Content> nonArgLineTags = ListUtils.filter(configContents, content -> content != argLineTag);
                        Xml.Tag mergedConfiguration = buildConfigurationTag(mergedArgLine, true);
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

    private static boolean isJacocoPlugin(Plugin plugin) {
        return "jacoco-maven-plugin".equals(plugin.getArtifactId()) && "org.jacoco".equals(plugin.getGroupId());
    }

    /**
     * Scans the raw document, as JaCoCo declared in a profile or in {@code pluginManagement} is absent from
     * {@link org.openrewrite.maven.tree.ResolvedPom#getPlugins()}.
     */
    private static boolean declaresJacocoPlugin(Xml.Document document) {
        return new XmlIsoVisitor<AtomicBoolean>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, AtomicBoolean found) {
                if ("plugin".equals(tag.getName()) &&
                        "jacoco-maven-plugin".equals(tag.getChildValue("artifactId").orElse(null)) &&
                        "org.jacoco".equals(tag.getChildValue("groupId").orElse(null))) {
                    found.set(true);
                    return tag;
                }
                return super.visitTag(tag, found);
            }
        }.reduce(document, new AtomicBoolean()).get();
    }

    // Matching the plugin, not each execution, keeps this to one new execution however many the plugin already has.
    private static class AddPropertiesGoalExecutionVisitor extends XmlIsoVisitor<ExecutionContext> {
        private static final XPathMatcher PLUGIN_MATCHER = new XPathMatcher("/project/build/plugins/plugin");

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            // A missing groupId is Maven's own; the matcher excludes pluginManagement.
            if (!"plugin".equals(t.getName()) ||
                    !MAVEN_DEPENDENCY_PLUGIN_ARTIFACT_ID.equals(t.getChildValue("artifactId").orElse(null)) ||
                    !MAVEN_PLUGINS_GROUP_ID.equals(t.getChildValue("groupId").orElse(MAVEN_PLUGINS_GROUP_ID)) ||
                    !PLUGIN_MATCHER.matches(getCursor())) {
                return t;
            }

            Optional<Xml.Tag> executions = t.getChild("executions");
            if (executions.isPresent() && executions.get().getChildren("execution").stream()
                    .anyMatch(execution -> !execution.getChild("phase").isPresent() &&
                            execution.getChildren("goals").stream()
                                    .flatMap(goals -> goals.getChildren("goal").stream())
                                    .anyMatch(goal -> PROPERTIES_GOAL.equals(goal.getValue().orElse(null))))) {
                return t;
            }

            Xml.Tag scope = executions.orElse(t);
            Xml.Tag tagToAdd = executions.isPresent() ?
                    Xml.Tag.build(MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG) :
                    Xml.Tag.build("<executions>" + MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG + "</executions>");
            return (Xml.Tag) new AddToTagVisitor<ExecutionContext>(scope, tagToAdd)
                    .visitNonNull(t, ctx, getCursor().getParentOrThrow());
        }
    }
}
