/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

/**
 * This imperative recipe will add the maven jar plugin to a maven project. The maven jar plugin will be configured to suppress
 * Illegal Reflection Warnings. In the case of a multi-module project, this recipe will attempt to add the plugin to only the top level project.
 */
@Incubating(since = "0.2.0")
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddSuppressionForIllegalReflectionWarningsPlugin extends Recipe {

    private static final XPathMatcher PACKAGING_MATCHER = new XPathMatcher("/project/packaging");

    @Option(displayName = "version",
            description = "An exact version number, or node-style semver selector used to select the version number.",
            required = false,
            example = "29.X")
    private final String version;

    @Override
    public String getDisplayName() {
        return "Add Maven Jar Plugin to suppress Illegal Reflection Warnings";
    }

    @Override
    public String getDescription() {
        return "Adds a maven jar plugin that's configured to suppress Illegal Reflection Warnings.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new AddMavenJarPluginVisitor();
    }

    private class AddMavenJarPluginVisitor extends MavenVisitor {

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
            if (PACKAGING_MATCHER.matches(getCursor())) {
                // TODO: add condition for SpringBoot-Maven-Plugin.
                if (t.getValue().isPresent() && (t.getValue().get().equals("ear") || t.getValue().get().equals("war"))) {
                    String groupId = "org.apache.maven.plugins";
                    String artifactId = "maven-jar-plugin";
                    // TODO: Prioritize managedPlugin version.
                    String version = StringUtils.isNullOrEmpty(getVersion()) ? "3.2.0" : getVersion();
                    String configuration =
                            "<configuration>\n" +
                                    "    <archive>\n" +
                                    "        <manifestEntries>\n" +
                                    "            <Add-Opens>java.base/java.lang java.base/java.util java.base/java.lang.reflect java.base/java.text java.desktop/java.awt.font</Add-Opens>\n" +
                                    "        </manifestEntries>\n" +
                                    "    </archive>\n" +
                                    "</configuration>";

                    doAfterVisit(new AddPlugin(groupId, artifactId, version, configuration, null, null));
                }
            }

            return t;
        }
    }
}
