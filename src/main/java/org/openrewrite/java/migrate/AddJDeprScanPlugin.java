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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.AddPlugin;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

/**
 * This imperative recipe will add the jdeprsacn plugin to a maven project. In the case of a multi-module project,
 * this recipe will attempt to add the plugin to only the top level project.
 */
@Incubating(since = "0.2.0")
public class AddJDeprScanPlugin extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add JDeprScan Maven Plug-in";
    }

    @Override
    public String getDescription() {
        return "JDeprScan scans class files for uses of deprecated APIs.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        return ListUtils.map(before, s -> {
            if ("pom.xml".equals(s.getSourcePath().toString()) && s.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                return (SourceFile) new AddJDeprScanPluginVisitor().visit(s, ctx);
            }
            return s;
        });
    }

    private static class AddJDeprScanPluginVisitor extends MavenVisitor<ExecutionContext> {

        @Override
        public Xml visitDocument(Xml.Document document, ExecutionContext o) {
            doAfterVisit(new AddPlugin(
                    "org.apache.maven.plugins",
                    "maven-jdeprscan-plugin",
                    "3.0.0-alpha-1",
                    "" +
                            "<configuration>\n" +
                            "   <release>11</release>\n" +
                            "</configuration>",
                    null,
                    null));
            return document;
        }
    }
}
