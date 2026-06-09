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

    private static final XPathMatcher MAVEN_SUREFIRE_PLUGIN_MATCHER = new XPathMatcher(
            "/project/build/plugins/plugin[artifactId='maven-surefire-plugin']");

    private static final XPathMatcher MAVEN_SUREFIRE_PLUGIN_CONFIGURATION_MATCHER = new XPathMatcher(
            "/project/build/plugins/plugin[artifactId='maven-surefire-plugin']/configuration");

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
                        .filter(dependency -> dependency.getGroupId().equals("org.mockito") && dependency.getArtifactId()
                                .equals("mockito-core")).findFirst().map(ResolvedDependency::getVersion).get();

                return new LatestRelease(null).compare(null, mockitoCoreVersion, "5.14.0") >= 0 ?
                        "-javaagent:${org.mockito:mockito-core:jar}" : "-javaagent:${net.bytebuddy:byte-buddy-agent:jar}";
            }

            private Xml.Tag buildConfigurationTag(String argLineJavaAgentParam, boolean hasExistingArgLine) {
                return Xml.Tag.build(String.format(CONFIGURATION_TAG_TEMPLATE, hasExistingArgLine ? argLineJavaAgentParam : "@{argLine} " + argLineJavaAgentParam));
            }

            private void maybeAddMavenDependencyPluginWithPropertiesGoal() {
                Optional<Plugin> mavenDependencyPlugin = getResolutionResult().getPom().getPlugins().stream()
                        .filter(plugin -> plugin.getGroupId().equals("org.apache.maven.plugins")
                                && plugin.getArtifactId().equals("maven-dependency-plugin")).findFirst();

                if (!mavenDependencyPlugin.isPresent()) {
                    doAfterVisit(new AddPlugin("org.apache.maven.plugins", "maven-dependency-plugin", null, null, null,
                            "<executions>" + MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG + "</executions>", "**/pom.xml").getVisitor());
                } else if (mavenDependencyPlugin.get().getExecutions().isEmpty()) {
                    doAfterVisit(new ChangePluginExecutions("org.apache.maven.plugins", "maven-dependency-plugin", MAVEN_DEPENDENCY_PLUGIN_EXECUTION_TAG).getVisitor());
                } else if (mavenDependencyPlugin.get().getExecutions().stream().noneMatch(execution -> execution.getGoals() != null)) {
                    doAfterVisit(new AddOrUpdateChildTag(MAVEN_DEPENDENCY_PLUGIN_EXECUTION_MATCHER, "<goals>" + MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL + "</goals>", false).getVisitor());
                } else if (mavenDependencyPlugin.get().getExecutions().stream().noneMatch(execution -> execution.getGoals() != null && execution.getGoals().contains("properties"))) {
                    doAfterVisit(new AppendChildTagToParentVisitor(MAVEN_DEPENDENCY_PLUGIN_EXECUTION_MATCHER + "/goals", MAVEN_DEPENDENCY_PLUGIN_PROPERTIES_GOAL));
                }
            }

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (getResolutionResult().getPom().getPluginManagement().stream().anyMatch(
                        plugin -> plugin.getGroupId().equals("org.apache.maven.plugins") && plugin.getArtifactId()
                                .equals("maven-surefire-plugin") && plugin.getConfigurationStringValue("argLine") != null && plugin.getConfigurationStringValue("argLine").contains(getArgLineJavaAgentArgument()))) {
                    return document;
                }

                maybeAddMavenDependencyPluginWithPropertiesGoal();
                doAfterVisit(new AddPropertyVisitor("argLine", "", true));

                if (FindPlugin.find(document, "org.apache.maven.plugins", "maven-surefire-plugin").isEmpty()) {
                    doAfterVisit(new AddPlugin("org.apache.maven.plugins", "maven-surefire-plugin", null,
                            String.format(CONFIGURATION_TAG_TEMPLATE, "@{argLine} " + getArgLineJavaAgentArgument()), null,
                            null, null).getVisitor());
                    return document;
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                //noinspection unchecked
                final List<Content> tagContents = (List<Content>) t.getContent();
                if (MAVEN_SUREFIRE_PLUGIN_MATCHER.matches(getCursor()) && !t.getChild("configuration").isPresent()) {
                    return autoFormat(t.withContent(ListUtils.concat(tagContents,
                            buildConfigurationTag(getArgLineJavaAgentArgument(), false))), ctx);
                } else if (MAVEN_SUREFIRE_PLUGIN_CONFIGURATION_MATCHER.matches(getCursor())) {
                    String argLineJavaAgentParam = getArgLineJavaAgentArgument();
                    List<Xml.Tag> argLineTagChildren = t.getChildren("argLine");
                    if (argLineTagChildren.size() == 1) {
                        Xml.Tag argLineTag = argLineTagChildren.get(0);
                        String existingArgLineValue = argLineTag.getValue().orElse("@{argLine}");

                        if (!existingArgLineValue.contains(argLineJavaAgentParam)) {
                            List<Content> nonArgLineTags = ListUtils.filter(tagContents, content -> content != argLineTag);
                            Xml.Tag configurationTagWithExistingArgParams = buildConfigurationTag(existingArgLineValue + " " + argLineJavaAgentParam, true);
                            return autoFormat(t.withContent(
                                    ListUtils.concatAll(nonArgLineTags, configurationTagWithExistingArgParams.getContent())), ctx);
                        }
                    } else if(argLineTagChildren.isEmpty()) {
                        return autoFormat(t.withContent(
                                ListUtils.concatAll(tagContents, buildConfigurationTag(argLineJavaAgentParam, false).getContent())), ctx);
                    }
                }
                return t;
            }
        });
    }
    public static class AppendChildTagToParentVisitor extends XmlIsoVisitor<ExecutionContext> {
        private final XPathMatcher parentXPathMatcher;
        private final Xml.Tag newChildTag;

        public AppendChildTagToParentVisitor(String parentXPath, @Language("xml") String newChildTag) {
            this.parentXPathMatcher = new XPathMatcher(parentXPath);
            this.newChildTag = Xml.Tag.build(newChildTag);
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (parentXPathMatcher.matches(getCursor()) && !tag.print(getCursor()).contains(newChildTag.print(getCursor()))) {
                return autoFormat(AddToTagVisitor.addToTag(tag, newChildTag, getCursor()), ctx);
            }
            return super.visitTag(tag, ctx);
        }
    }
}
