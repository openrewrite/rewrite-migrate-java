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
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeJavaVersion extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upgrade Java version";
    }

    @Override
    public String getDescription() {
        return "Upgrade build plugin configuration to use the specified Java version. " +
               "This recipe changes maven-compiler-plugin target version and related settings. " +
               "Will not downgrade if the version is newer than the specified version.";
    }

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        String newVersion = this.version.toString();

        // Create a new JavaVersion marker with the new version
        Optional<JavaVersion> currentMarker = before.stream()
                .map(it -> it.getMarkers().findFirst(JavaVersion.class))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
        if (!currentMarker.isPresent() || currentMarker.get().getMajorVersion() >= version) {
            return before;
        }
        JavaVersion updatedMarker = currentMarker.get()
                .withSourceCompatibility(newVersion)
                .withTargetCompatibility(newVersion);

        return ListUtils.map(before, sourceFile -> sourceFile.getMarkers().findFirst(JavaVersion.class)
                .map(version -> (SourceFile) sourceFile.withMarkers(sourceFile.getMarkers().computeByType(version,
                        (v, acc) -> updatedMarker)))
                .orElse(sourceFile));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, executionContext);
                if (!isPropertyTag()) {
                    return t;
                }
                if (!"java.version".equals(t.getName()) && !"maven.compiler.source".equals(t.getName()) && !"maven.compiler.target".equals(t.getName()) ||
                    (tag.getValue().isPresent() && tag.getValue().get().startsWith("${"))) {
                    return t;
                }
                float value = tag.getValue().map(Float::parseFloat).orElse(0f);
                if (value >= version) {
                    return t;
                }
                return t.withValue(String.valueOf(version));
            }
        };
    }
}
