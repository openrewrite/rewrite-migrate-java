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
               " These project properties are not added if they are not currently set, but only updated in place.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext executionContext) {
                // Update properties already defined in the current pom
                Xml.Document d = super.visitDocument(document, executionContext);

                // Return early if the parent appears to be within the current repository, as properties defined there will be updated
                Optional<String> pathToLocalParent = d.getRoot().getChild("parent")
                        .flatMap(parent -> parent.getChild("relativePath"))
                        .flatMap(Xml.Tag::getValue);
                if(pathToLocalParent.isPresent()) {
                    return d;
                }

                // Otherwise override remote parent's properties locally
                Map<String, String> currentProperties = getResolutionResult().getPom().getRequested().getProperties();
                for (String property : JAVA_VERSION_PROPERTIES) {
                    if(currentProperties.containsKey(property)) {
                        continue;
                    }
                    d = (Xml.Document) new AddProperty(property, String.valueOf(version), null, false)
                            .getVisitor()
                            .visitNonNull(d, executionContext);
                }

                return d;
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                tag = super.visitTag(tag, ctx);

                if (JAVA_VERSION_XPATH_MATCHERS.stream().anyMatch(matcher -> matcher.matches(getCursor()))) {
                    Optional<Float> maybeVersion = tag.getValue().flatMap(
                            value -> {
                                try {
                                    return Optional.of(Float.parseFloat(value));
                                } catch (NumberFormatException e) {
                                    return Optional.empty();
                                }
                            }
                    );

                    if (!maybeVersion.isPresent()) {
                        return tag;
                    }
                    float currentVersion = maybeVersion.get();
                    if (currentVersion >= version) {
                        return tag;
                    }
                    return tag.withValue(String.valueOf(version));
                }

                return tag;
            }
        };
    }
}
