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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.AddToTagVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

public class UpgradeIanalGroovyForJava25 extends Recipe {
    private static final String GROUP = "org.codehaus.mojo";
    private static final String ARTIFACT = "ianal-maven-plugin";
    private static final String VERSION = "1.0-alpha-1";
    private static final String DEPENDENCIES =
            "<dependencies>" +
            "<dependency>" +
            "<groupId>org.codehaus.groovy.maven</groupId>" +
            "<artifactId>gmaven-mojo</artifactId>" +
            "<version>1.0-rc-3</version>" +
            "<exclusions><exclusion>" +
            "<groupId>org.codehaus.groovy</groupId>" +
            "<artifactId>groovy-all-minimal</artifactId>" +
            "</exclusion></exclusions>" +
            "</dependency>" +
            groovyDependency("groovy") + groovyDependency("groovy-ant") +
            "</dependencies>";

    @Getter
    final String displayName = "Upgrade the ianal Maven plugin's Groovy runtime for Java 25";

    @Getter
    final String description = "Run the ianal Maven plugin's legal file verification on Java 25 using a plugin-scoped Groovy 3 runtime. " +
            "Preserve the goal, configuration and project dependencies.";

    private static String groovyDependency(String artifact) {
        return "<dependency><groupId>org.codehaus.groovy</groupId><artifactId>" + artifact +
                "</artifactId><version>3.0.25</version></dependency>";
    }

    private static boolean isIanal(Plugin plugin, ResolvedPom pom) {
        return GROUP.equals(pom.getValue(plugin.getGroupId())) && ARTIFACT.equals(pom.getValue(plugin.getArtifactId()));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                ResolvedPom pom = getResolutionResult().getPom();
                Optional<Plugin> active = pom.getPlugins().stream().filter(p -> isIanal(p, pom)).findFirst();
                if (!active.isPresent() || !active.get().getDependencies().isEmpty() ||
                    active.get().getExecutions().stream().noneMatch(e -> e.getGoals() != null && e.getGoals().contains("verify-legal-files"))) {
                    return d;
                }
                Optional<Plugin> managed = pom.getPluginManagement().stream().filter(p -> isIanal(p, pom)).findFirst();
                String version = active.get().getVersion();
                if (version == null && managed.isPresent()) {
                    version = managed.get().getVersion();
                }
                if (version == null || !VERSION.equals(pom.getValue(version)) ||
                    managed.filter(p -> !p.getDependencies().isEmpty()).isPresent() ||
                    pom.getRequested().getProfiles().stream().anyMatch(profile ->
                            profile.getPlugins().stream().anyMatch(p -> isIanal(p, pom)) ||
                            profile.getPluginManagement().stream().anyMatch(p -> isIanal(p, pom)))) {
                    return d;
                }

                Xml.Tag root = d.getRoot();
                Optional<Xml.Tag> build = root.getChild("build");
                Optional<Xml.Tag> plugins = build.flatMap(b -> b.getChild("plugins"));
                Optional<Xml.Tag> local = plugins.flatMap(ps -> ps.getChildren("plugin").stream()
                        .filter(p -> GROUP.equals(pom.getValue(p.getChildValue("groupId").orElse(""))) &&
                                     ARTIFACT.equals(pom.getValue(p.getChildValue("artifactId").orElse(""))))
                        .findFirst());

                if (local.isPresent()) {
                    if (local.get().getChild("dependencies").isPresent()) {
                        return d;
                    }
                    root = add(root, local.get(), DEPENDENCIES, ctx);
                } else {
                    // Adding only a dependency override leaves the parent's executions and configuration in effect.
                    String plugin = "<plugin><groupId>" + GROUP + "</groupId><artifactId>" + ARTIFACT +
                            "</artifactId><version>" + VERSION + "</version>" + DEPENDENCIES + "</plugin>";
                    if (plugins.isPresent()) {
                        root = add(root, plugins.get(), plugin, ctx);
                    } else {
                        String pluginList = "<plugins>" + plugin + "</plugins>";
                        root = build.isPresent() ? add(root, build.get(), pluginList, ctx) :
                                add(root, root, "<build>" + pluginList + "</build>", ctx);
                    }
                }
                maybeUpdateModel();
                return d.withRoot(root);
            }

            private Xml.Tag add(Xml.Tag root, Xml.Tag scope, String child, ExecutionContext ctx) {
                return (Xml.Tag) new AddToTagVisitor<ExecutionContext>(scope, Xml.Tag.build(child))
                        .visitNonNull(root, ctx, getCursor());
            }
        };
    }
}
