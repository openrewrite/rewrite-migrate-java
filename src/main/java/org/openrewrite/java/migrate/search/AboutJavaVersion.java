/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.search;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionPerSourceSet;
import org.openrewrite.java.migrate.table.JavaVersionRow;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class AboutJavaVersion extends Recipe {
    transient JavaVersionPerSourceSet javaVersionPerSourceSet = new JavaVersionPerSourceSet(this);
    transient Set<ProjectSourceSet> seenSourceSets = new HashSet<>();

    @Option(required = false,
            description = "Only mark the Java version when this type is in use.",
            example = "lombok.val")
    @Nullable
    String whenUsesType;

    @Override
    public String getDisplayName() {
        return "Find which Java version is in use";
    }

    @Override
    public String getDescription() {
        return "A diagnostic for studying the distribution of Java language version levels " +
               "(both source and target compatibility across files and source sets).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {


        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree cu, ExecutionContext ctx) {
                if (!(cu instanceof JavaSourceFile)) {
                    return cu;
                }
                return cu.getMarkers().findFirst(JavaVersion.class)
                        .map(version -> {
                            JavaProject project = cu.getMarkers().findFirst(JavaProject.class)
                                    .orElse(null);
                            String sourceSet = cu.getMarkers().findFirst(JavaSourceSet.class).map(JavaSourceSet::getName)
                                    .orElse("");
                            if (seenSourceSets.add(new ProjectSourceSet(project, sourceSet))) {
                                javaVersionPerSourceSet.insertRow(ctx, new JavaVersionRow(
                                        project == null ? "" : project.getProjectName(),
                                        sourceSet,
                                        version.getCreatedBy(),
                                        version.getVmVendor(),
                                        version.getSourceCompatibility(),
                                        Integer.toString(version.getMajorReleaseVersion()),
                                        version.getTargetCompatibility()
                                ));
                            }
                            return SearchResult.found(cu, "Java version: " + version.getMajorVersion());
                        })
                        .orElse(cu);
            }
        };
        if (StringUtils.isNotBlank(whenUsesType)) {
            visitor = Preconditions.check(new UsesType<>(whenUsesType, false), visitor);
        }
        return visitor;
    }

    @Value
    static class ProjectSourceSet {
        @Nullable
        JavaProject javaProject;

        String javaSourceSet;
    }
}
