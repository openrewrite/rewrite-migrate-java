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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * This imperative recipe will add the jdeprscan plugin to a maven project. In the case of a multi-module project,
 * this recipe will attempt to add the plugin to only the top level project.
 */
@Incubating(since = "0.2.0")
@RequiredArgsConstructor
@Getter
public class AddJDeprScanPlugin extends Recipe {

    @Option(displayName = "release", description = "Specifies the Java SE release that provides the set of deprecated APIs for scanning.", required = false, example = "11")
    private final String release;

    @Override
    public String getDisplayName() {
        return "Add `JDeprScan` Maven Plug-in";
    }

    @Override
    public String getDescription() {
        return "Add the `JDeprScan` Maven plugin to scan class files for uses of deprecated APIs.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable Xml preVisit(Xml tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return new AddJDeprScanPluginVisitor().visit(tree, ctx);
            }
        };
    }

    private class AddJDeprScanPluginVisitor extends MavenVisitor<ExecutionContext> {

        @Override
        public Xml visitDocument(Xml.Document document, ExecutionContext ctx) {
            doAfterVisit(new AddPlugin(
                    "org.apache.maven.plugins",
                    "maven-jdeprscan-plugin",
                    "3.0.0-alpha-1",
                    String.format("<configuration>%n   <release>%s</release>%n</configuration>",
                            StringUtils.isNullOrEmpty(getRelease()) ? "11" : getRelease()),
                    null,
                    null,
                    null).getVisitor());
            return document;
        }
    }
}
