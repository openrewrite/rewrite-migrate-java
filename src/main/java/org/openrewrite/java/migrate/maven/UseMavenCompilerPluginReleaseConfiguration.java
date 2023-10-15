/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.search.FindProperties;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterTagChildren;

@Value
@EqualsAndHashCode(callSuper = true)
public class UseMavenCompilerPluginReleaseConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");

    @Option(
            displayName = "Release Version",
            description = "The new value for the release configuration. This recipe prefers ${java.version} if defined.",
            example = "11"
    )
    String releaseVersion;

    @Override
    public String getDisplayName() {
        return "Use Maven Compiler Plugin Release Configuration";
    }

    @Override
    public String getDescription() {
        return "Replaces any explicit `source` or `target` configuration (if present) on the maven-compiler-plugin with " +
                "`release`, and updates the `release` value if needed. Will not downgrade the java version if the current version is higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!PLUGINS_MATCHER.matches(getCursor())) {
                    return t;
                }
                Optional<Xml.Tag> maybeCompilerPlugin = t.getChildren().stream()
                        .filter(plugin -> "plugin".equals(plugin.getName())
                                && "org.apache.maven.plugins".equals(plugin.getChildValue("groupId").orElse(null))
                                && "maven-compiler-plugin".equals(plugin.getChildValue("artifactId").orElse(null)))
                        .findAny();
                Optional<Xml.Tag> maybeCompilerPluginConfig = maybeCompilerPlugin
                        .flatMap(it -> it.getChild("configuration"));
                if (!maybeCompilerPluginConfig.isPresent()) {
                    return t;
                }
                Xml.Tag compilerPluginConfig = maybeCompilerPluginConfig.get();
                Optional<String> source = compilerPluginConfig.getChildValue("source");
                Optional<String> target = compilerPluginConfig.getChildValue("target");
                Optional<String> release = compilerPluginConfig.getChildValue("release");
                if (!source.isPresent()
                        && !target.isPresent()
                        && !release.isPresent()
                        || currentNewerThanProposed(release)) {
                    return t;
                }
                Xml.Tag updated = filterTagChildren(t, compilerPluginConfig,
                        child -> !("source".equals(child.getName()) || "target".equals(child.getName())));
                if (updated != t
                        && source.map("${maven.compiler.source}"::equals).orElse(true)
                        && target.map("${maven.compiler.target}"::equals).orElse(true)) {
                    return filterTagChildren(updated, maybeCompilerPlugin.get(),
                            child -> !("configuration".equals(child.getName()) && child.getChildren().isEmpty()));
                }
                String releaseVersionValue = hasJavaVersionProperty(getCursor().firstEnclosingOrThrow(Xml.Document.class))
                        ? "${java.version}" : releaseVersion;
                updated = addOrUpdateChild(updated, compilerPluginConfig,
                        Xml.Tag.build("<release>" + releaseVersionValue + "</release>"), getCursor().getParentOrThrow());
                return updated;
            }

        };
    }

    private boolean currentNewerThanProposed(@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<String> maybeRelease) {
        if (!maybeRelease.isPresent()) {
            return false;
        }
        try {
            float currentVersion = Float.parseFloat(maybeRelease.get());
            float proposedVersion = Float.parseFloat(releaseVersion);
            return proposedVersion < currentVersion;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean hasJavaVersionProperty(Xml.Document xml) {
        return !FindProperties.find(xml, "java.version").isEmpty();
    }
}
