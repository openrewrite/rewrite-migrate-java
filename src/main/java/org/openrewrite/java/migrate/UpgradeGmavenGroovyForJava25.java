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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

public class UpgradeGmavenGroovyForJava25 extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upgrade the GMaven Groovy 3 compiler for Java 25";
    }

    @Override
    public String getDescription() {
        return "Raise explicit Groovy 3.0 compiler dependencies in GMaven plugins to at least 3.0.25, which includes ASM support " +
               "for Java 25. Preserve other Groovy release lines and newer patches. Inherited version properties are overridden " +
               "on the plugin dependency, leaving the parent and its other consumers unchanged.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isPluginDependencyTag("org.codehaus.groovy", "groovy*") ||
                    getCursor().getPathAsStream().filter(Xml.Tag.class::isInstance).map(Xml.Tag.class::cast)
                        .filter(it -> "plugin".equals(it.getName()))
                        .noneMatch(it -> "org.codehaus.gmaven".equals(it.getChildValue("groupId").orElse("")) &&
                                "groovy-maven-plugin".equals(it.getChildValue("artifactId").orElse("")))) {
                    return t;
                }
                String requested = t.getChildValue("version").orElse("");
                String resolved = getResolutionResult().getPom().getValue(requested);
                if (resolved == null || !resolved.matches("3\\.0\\.\\d+") ||
                    new LatestRelease(null).compare(null, resolved, "3.0.25") >= 0) {
                    return t;
                }
                if (requested.matches("\\$\\{[^}]+}") &&
                    !getResolutionResult().getPom().getRequested().getProperties()
                        .containsKey(requested.substring(2, requested.length() - 1))) {
                    // The parent may not declare GMaven and must not be changed for its unrelated consumers.
                    doAfterVisit(new ChangeTagValueVisitor<>(t.getChild("version").get(), "3.0.25"));
                } else {
                    t = changeChildTagValue(t, "version", "3.0.25", ctx);
                }
                maybeUpdateModel();
                return t;
            }
        };
    }
}
