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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddLombokMapstructBindingMavenDependencyOnly extends Recipe {
    String displayName = "Add `lombok-mapstruct-binding` dependency for Maven when both MapStruct and Lombok are used";

    String description = "Add the `lombok-mapstruct-binding` Maven dependency when this module uses both Lombok and MapStruct " +
            "and the binding is not already a dependency or configured on the compiler annotation processor path.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (getResolutionResult().findDependencies("org.projectlombok", "lombok", null).isEmpty() ||
                        getResolutionResult().findDependencies("org.mapstruct", "mapstruct", null).isEmpty() ||
                        !getResolutionResult().findDependencies("org.projectlombok", "lombok-mapstruct-binding", null).isEmpty()) {
                    return document;
                }
                ResolvedPom pom = getResolutionResult().getPom();
                // The resolved model can omit even locally declared plugins with inherited=false.
                List<Plugin> plugins = ListUtils.concatAll(pom.getPlugins(), nonInheritedPlugins(pom.getRequested().getPlugins()));
                List<Plugin> managedPlugins = ListUtils.concatAll(pom.getPluginManagement(), nonInheritedPlugins(pom.getRequested().getPluginManagement()));
                if (hasBindingProcessor(pom, plugins) ||
                        !overridesManagedProcessorPaths(plugins) && hasBindingProcessor(pom, managedPlugins)) {
                    return document;
                }
                return (Xml.Document) new AddDependencyVisitor("org.projectlombok", "lombok-mapstruct-binding", "0.2.0",
                        null, null, null, null, null, null, null).visitNonNull(document, ctx);
            }

            private List<Plugin> nonInheritedPlugins(List<Plugin> plugins) {
                return ListUtils.map(plugins, plugin -> "false".equals(plugin.getInherited()) ? plugin : null);
            }

            private boolean overridesManagedProcessorPaths(List<Plugin> plugins) {
                for (Plugin plugin : plugins) {
                    if (!"org.apache.maven.plugins".equals(plugin.getGroupId()) ||
                            !"maven-compiler-plugin".equals(plugin.getArtifactId())) {
                        continue;
                    }
                    JsonNode configuration = plugin.getConfiguration();
                    if (configuration != null &&
                            ("override".equals(configuration.path("combine.self").asText()) ||
                             "override".equals(configuration.path("annotationProcessorPaths").path("combine.self").asText()))) {
                        return true;
                    }
                }
                return false;
            }

            private boolean hasBindingProcessor(ResolvedPom pom, List<Plugin> plugins) {
                for (Plugin plugin : plugins) {
                    if (!"org.apache.maven.plugins".equals(plugin.getGroupId()) ||
                            !"maven-compiler-plugin".equals(plugin.getArtifactId())) {
                        continue;
                    }
                    JsonNode configuration = plugin.getConfiguration();
                    if (configuration == null) {
                        continue;
                    }
                    JsonNode paths = configuration.path("annotationProcessorPaths").path("path");
                    if (paths.isArray()) {
                        for (JsonNode path : paths) {
                            if (isBinding(pom, path)) {
                                return true;
                            }
                        }
                    } else if (isBinding(pom, paths)) {
                        return true;
                    }
                }
                return false;
            }

            private boolean isBinding(ResolvedPom pom, JsonNode path) {
                return "org.projectlombok".equals(pom.getValue(path.path("groupId").asText(""))) &&
                        "lombok-mapstruct-binding".equals(pom.getValue(path.path("artifactId").asText("")));
            }
        };
    }
}
