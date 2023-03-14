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
package org.openrewrite.java.migrate.search;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionPerFile;
import org.openrewrite.java.migrate.table.JavaVersionPerSourceSet;
import org.openrewrite.java.migrate.table.JavaVersionRow;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class AboutJavaVersion extends Recipe {
    transient JavaVersionPerFile javaVersionPerFile = new JavaVersionPerFile(this);
    transient JavaVersionPerSourceSet javaVersionPerSourceSet = new JavaVersionPerSourceSet(this);

    @Option(required = false,
            description = "Only mark the Java version when this type is in use.",
            example = "lombok.val")
    @Nullable
    String whenUsesType;

    @Override
    public String getDisplayName() {
        return "List calculated information about Java version on source files";
    }

    @Override
    public String getDescription() {
        return "A diagnostic for studying the applicability of Java version constraints.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return StringUtils.isBlank(whenUsesType) ? null : new UsesType<>(whenUsesType);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        Map<ProjectSourceSet, JavaVersionRow> sourceSetVersion = new HashMap<>();

        for (SourceFile sourceFile : before) {
            sourceFile.getMarkers().findFirst(JavaProject.class).ifPresent(javaProject ->
                    sourceFile.getMarkers().findFirst(JavaSourceSet.class).ifPresent(sourceSet ->
                            sourceFile.getMarkers().findFirst(JavaVersion.class).ifPresent(version -> sourceSetVersion.put(new ProjectSourceSet(javaProject, sourceSet),
                                    new JavaVersionRow(
                                            javaProject.getProjectName(),
                                            sourceSet.getName(),
                                            version.getCreatedBy(),
                                            version.getVmVendor(),
                                            version.getSourceCompatibility(),
                                            Integer.toString(version.getMajorReleaseVersion()),
                                            version.getTargetCompatibility()
                                    ))
                            )
                    )
            );
        }

        for (JavaVersionRow row : sourceSetVersion.values()) {
            javaVersionPerSourceSet.insertRow(ctx, row);
        }

        return super.visit(before, ctx);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                return cu.getMarkers().findFirst(JavaVersion.class)
                        .map(version -> {
                            String projectName = cu.getMarkers().findFirst(JavaProject.class).map(JavaProject::getProjectName)
                                    .orElse("");
                            String sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class).map(JavaSourceSet::getName)
                                    .orElse("");

                            javaVersionPerFile.insertRow(ctx, new JavaVersionRow(
                                    projectName,
                                    sourceSet,
                                    version.getCreatedBy(),
                                    version.getVmVendor(),
                                    version.getSourceCompatibility(),
                                    Integer.toString(version.getMajorReleaseVersion()),
                                    version.getTargetCompatibility()
                            ));
                            return SearchResult.found(cu, "Java version: " + version.getMajorVersion());
                        })
                        .orElse(cu);
            }
        };
    }

    @Value
    private static class ProjectSourceSet {
        JavaProject javaProject;
        JavaSourceSet javaSourceSet;
    }
}
