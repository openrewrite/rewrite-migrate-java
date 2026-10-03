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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeUnmodeledMavenJavaVersion extends Recipe {
    @Option(displayName = "Java version", description = "The minimum Java version.", example = "25")
    Integer version;

    String displayName = "Upgrade Java version in alternate Maven builds";
    String description = "Update explicit Java version properties and compiler settings in pom.xml files parsed as XML " +
            "without a Maven model, for example in a repository built with Gradle. Property references are left intact.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            private final XPathMatcher properties = new XPathMatcher("/project/properties/*");
            private final XPathMatcher compiler = new XPathMatcher("//plugin[artifactId='maven-compiler-plugin']/configuration/*");

            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (!"pom.xml".equals(document.getSourcePath().getFileName().toString()) ||
                        document.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                    return document;
                }
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                String name = t.getName();
                boolean javaProperty = properties.matches(getCursor()) &&
                        ("java.version".equals(name) || "maven.compiler.release".equals(name) ||
                         "maven.compiler.source".equals(name) || "maven.compiler.target".equals(name));
                boolean compilerSetting = compiler.matches(getCursor()) &&
                        ("release".equals(name) || "source".equals(name) || "target".equals(name));
                if (javaProperty || compilerSetting) {
                    String value = t.getValue().orElse("");
                    String major = value.startsWith("1.") ? value.substring(2) : value;
                    if (major.matches("[0-9]{1,3}") && Integer.parseInt(major) < version) {
                        return t.withValue(version.toString());
                    }
                }
                return t;
            }
        };
    }
}
