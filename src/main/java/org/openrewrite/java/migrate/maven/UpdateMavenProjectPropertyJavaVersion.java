/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.maven.AddProperty;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateMavenProjectPropertyJavaVersion extends Recipe {

    private static final List<String> JAVA_VERSION_PROPERTIES = Arrays.asList(
            "java.version",
            "jdk.version",
            "javaVersion",
            "jdkVersion",
            "maven.compiler.source",
            "maven.compiler.target",
            "maven.compiler.release",
            "release.version");

    private static final List<XPathMatcher> JAVA_VERSION_XPATH_MATCHERS =
            JAVA_VERSION_PROPERTIES.stream()
                    .map(property -> "/project/properties/" + property)
                    .map(XPathMatcher::new).collect(Collectors.toList());

    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build//plugins");

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Override
    public String getDisplayName() {
        return "Update Maven Java project properties";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "The Java version is determined by several project properties, including:\n\n" +
               " * `java.version`\n" +
               " * `jdk.version`\n" +
               " * `javaVersion`\n" +
               " * `jdkVersion`\n" +
               " * `maven.compiler.source`\n" +
               " * `maven.compiler.target`\n" +
               " * `maven.compiler.release`\n" +
               " * `release.version`\n\n" +
               "If none of these properties are in use and the maven compiler plugin is not otherwise configured adds the `maven.compiler.release` property.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            final Set<String> propertiesExplicitlyReferenced = new HashSet<>();
            boolean compilerPluginConfiguredExplicitly;

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // Update properties already defined in the current pom
                Xml.Document d = super.visitDocument(document, ctx);

                // Return early if the parent appears to be within the current repository, as properties defined there will be updated
                Optional<String> pathToLocalParent = d.getRoot().getChild("parent")
                        .flatMap(parent -> parent.getChild("relativePath"))
                        .flatMap(Xml.Tag::getValue);
                if (pathToLocalParent.isPresent()) {
                    return d;
                }

                // Otherwise override remote parent's properties locally
                MavenResolutionResult mrr = getResolutionResult();
                Map<String, String> currentProperties = mrr.getPom().getRequested().getProperties();
                for (String property : JAVA_VERSION_PROPERTIES) {
                    if (currentProperties.containsKey(property) || !propertiesExplicitlyReferenced.contains(property)) {
                        continue;
                    }
                    d = (Xml.Document) new AddProperty(property, String.valueOf(version), null, false)
                            .getVisitor()
                            .visitNonNull(d, ctx);
                }

                // When none of the relevant properties are explicitly configured Maven defaults to Java 8
                // The release option was added in 9
                // If no properties have yet been updated then set release explicitly
                if (version >= 9 &&
                    !compilerPluginConfiguredExplicitly &&
                    currentProperties.keySet()
                        .stream()
                        .noneMatch(JAVA_VERSION_PROPERTIES::contains)) {
                    d = (Xml.Document) new AddProperty("maven.compiler.release", String.valueOf(version), null, false)
                            .getVisitor()
                            .visitNonNull(d, ctx);
                    HashMap<String, String> updatedProps = new HashMap<>(currentProperties);
                    updatedProps.put("maven.compiler.release", version.toString());
                    mrr = mrr.withPom(mrr.getPom().withRequested(mrr.getPom().getRequested().withProperties(updatedProps)));

                    d = d.withMarkers(d.getMarkers().setByType(mrr));
                }
                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Optional<String> s = t.getValue()
                        .map(it -> it.replace("${", "").replace("}", "").trim())
                        .filter(JAVA_VERSION_PROPERTIES::contains);
                if (s.isPresent()) {
                    propertiesExplicitlyReferenced.add(s.get());
                } else if (JAVA_VERSION_XPATH_MATCHERS.stream().anyMatch(matcher -> matcher.matches(getCursor()))) {
                    Optional<Float> maybeVersion = t.getValue().flatMap(
                            value -> {
                                try {
                                    return Optional.of(Float.parseFloat(value));
                                } catch (NumberFormatException e) {
                                    return Optional.empty();
                                }
                            }
                    );

                    if (!maybeVersion.isPresent()) {
                        return t;
                    }
                    float currentVersion = maybeVersion.get();
                    if (currentVersion >= version) {
                        return t;
                    }
                    return t.withValue(String.valueOf(version));
                } else if (PLUGINS_MATCHER.matches(getCursor())) {
                    Optional<Xml.Tag> maybeCompilerPlugin = t.getChildren().stream()
                            .filter(plugin ->
                                    "plugin".equals(plugin.getName()) &&
                                    "org.apache.maven.plugins".equals(plugin.getChildValue("groupId").orElse("org.apache.maven.plugins")) &&
                                    "maven-compiler-plugin".equals(plugin.getChildValue("artifactId").orElse(null)))
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
                    if (source.isPresent()
                        || target.isPresent()
                        || release.isPresent()) {
                        compilerPluginConfiguredExplicitly = true;
                    }
                }
                return t;
            }
        };
    }
}
