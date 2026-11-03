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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.xml.tree.Xml;

public class AddApisForJaxwsCodegen extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add APIs for javax JAX-WS generated sources";
    }

    @Override
    public String getDescription() {
        return "Add compile APIs for an explicitly configured MojoHaus 2.6 wsimport or Codecentric 2.3.0.RELEASE generate execution, " +
               "even when generated sources are absent from the LST. Customized tool dependencies, external executables and test generation are excluded.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                // A modern project API is evidence that this module needs a namespace migration instead.
                if (getResolutionResult().getDependencies().values().stream().flatMap(java.util.Collection::stream)
                        .anyMatch(d -> modernApi(d.getGroupId(), d.getVersion())) ||
                    getResolutionResult().getPom().getDependencyManagement().stream()
                        .anyMatch(d -> modernApi(d.getGroupId(), d.getVersion()))) {
                    return document;
                }
                // Managed or inherited tool overrides can change the generator behind the same plugin version.
                if (getResolutionResult().getPom().getPluginManagement().stream().anyMatch(this::supportedPlugin) ||
                    getResolutionResult().getPom().getPlugins().stream().filter(this::supportedPlugin)
                        .anyMatch(p -> p.getDependencies() != null && !p.getDependencies().isEmpty() ||
                                p.getConfiguration() != null && (p.getConfiguration().has("executable") ||
                                        p.getConfiguration().has("useJdkToolchainExecutable")) ||
                                p.getExecutions() != null && p.getExecutions().stream().anyMatch(execution ->
                                        execution.getConfiguration() != null &&
                                        (execution.getConfiguration().has("executable") ||
                                         execution.getConfiguration().has("useJdkToolchainExecutable"))))) {
                    return document;
                }
                Xml.Tag build = document.getRoot().getChild("build").orElse(null);
                Xml.Tag plugins = build == null ? null : build.getChild("plugins").orElse(null);
                if (plugins == null || plugins.getChildren("plugin").stream().noneMatch(this::generatesJavax)) {
                    return document;
                }
                Xml.Document d = document;
                for (String[] api : new String[][]{
                        {"jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.3"},
                        {"jakarta.xml.ws", "jakarta.xml.ws-api", "2.3.3"},
                        {"jakarta.jws", "jakarta.jws-api", "2.1.0"}}) {
                    d = (Xml.Document) new AddDependencyVisitor(api[0], api[1], api[2], null,
                            "compile", null, null, null, null, null).visitNonNull(d, ctx);
                }
                return d;
            }

            private boolean supportedPlugin(Plugin plugin) {
                return "org.codehaus.mojo".equals(plugin.getGroupId()) &&
                       "jaxws-maven-plugin".equals(plugin.getArtifactId()) ||
                       "de.codecentric".equals(plugin.getGroupId()) &&
                       "cxf-spring-boot-starter-maven-plugin".equals(plugin.getArtifactId());
            }

            private boolean modernApi(String group, String version) {
                return ("jakarta.xml.bind".equals(group) || "jakarta.xml.ws".equals(group) ||
                        "jakarta.jws".equals(group)) && (version == null || !version.startsWith("2."));
            }

            private boolean generatesJavax(Xml.Tag plugin) {
                boolean mojoHaus = "org.codehaus.mojo".equals(plugin.getChildValue("groupId").orElse("")) &&
                        "jaxws-maven-plugin".equals(plugin.getChildValue("artifactId").orElse("")) &&
                        "2.6".equals(plugin.getChildValue("version").orElse(""));
                // The published wrapper descriptor binds generate to generate-sources and delegates to javax tools.
                boolean codecentric = "de.codecentric".equals(plugin.getChildValue("groupId").orElse("")) &&
                        "cxf-spring-boot-starter-maven-plugin".equals(plugin.getChildValue("artifactId").orElse("")) &&
                        "2.3.0.RELEASE".equals(plugin.getChildValue("version").orElse(""));
                if (!(mojoHaus || codecentric) ||
                    plugin.getChild("dependencies").isPresent() || externalExecutable(plugin)) {
                    return false;
                }
                return plugin.getChild("executions").map(executions -> executions.getChildren("execution").stream()
                        .anyMatch(execution -> !externalExecutable(execution) &&
                                "generate-sources".equals(execution.getChildValue("phase").orElse("generate-sources")) &&
                                execution.getChild("goals").map(goals -> goals.getChildren("goal").stream()
                                        .anyMatch(goal -> (mojoHaus ? "wsimport" : "generate").equals(goal.getValue().orElse("")))).orElse(false)))
                        .orElse(false);
            }

            private boolean externalExecutable(Xml.Tag tag) {
                return tag.getChild("configuration").map(configuration ->
                        configuration.getChild("executable").isPresent() ||
                        configuration.getChild("useJdkToolchainExecutable").isPresent()).orElse(false);
            }
        };
    }
}
