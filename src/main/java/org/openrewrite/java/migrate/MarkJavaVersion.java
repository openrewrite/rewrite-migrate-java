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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.marker.JavaVersion;

import java.util.List;
import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = true)
public class MarkJavaVersion extends Recipe {

    @Option(displayName = "Java version",
        description = "The Java version to upgrade to.",
        example = "11")
    Integer version;

    @Override
    public String getDisplayName() {
        return "Mark Java version for source files";
    }

    @Override
    public String getDescription() {
        return "Specify the Java version to be used for the source files by adding a marker.";
    }

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
}
