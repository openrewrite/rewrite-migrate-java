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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionTable;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.GitProvenance;
import org.openrewrite.marker.Markers;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class FindJavaVersion extends ScanningRecipe<Map<Object, JavaVersionTable.Row>> {

    transient JavaVersionTable table = new JavaVersionTable(this);

    @Getter
    final String displayName = "Find Java versions in use";

    @Getter
    final String description = "Finds Java versions in use, emitting one row per git repository " +
            "(the lowest source/target compatibility across modules in that repository).";

    @Override
    public Map<Object, JavaVersionTable.Row> getInitialValue(ExecutionContext ctx) {
        return new LinkedHashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<Object, JavaVersionTable.Row> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    Markers markers = tree.getMarkers();
                    markers.findFirst(JavaVersion.class).ifPresent(jv -> {
                        // Prefer the git origin as the dedup key: every module in a multi-module repo
                        // shares one GitProvenance, so they collapse to a single row. Fall back to the
                        // JavaProject UUID (one row per module) when no git provenance is available,
                        // and finally to the JavaVersion UUID so disconnected source files still
                        // produce one row per file rather than silently merging.
                        Optional<GitProvenance> gp = markers.findFirst(GitProvenance.class);
                        Object key;
                        if (gp.isPresent() && gp.get().getOrigin() != null) {
                            key = gp.get().getOrigin();
                        } else {
                            key = markers.findFirst(JavaProject.class)
                                    .<Object>map(JavaProject::getId)
                                    .orElseGet(jv::getId);
                        }

                        JavaVersionTable.Row candidate = new JavaVersionTable.Row(
                                Integer.toString(jv.getMajorVersion()),
                                Integer.toString(jv.getMajorReleaseVersion()));
                        acc.merge(key, candidate, FindJavaVersion::lower);
                    });
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Map<Object, JavaVersionTable.Row> acc, ExecutionContext ctx) {
        for (JavaVersionTable.Row row : acc.values()) {
            table.insertRow(ctx, row);
        }
        return emptyList();
    }

    // Lower target compatibility wins; tiebreak on lower source compatibility.
    // The retained row is the migration floor for the repository.
    private static JavaVersionTable.Row lower(JavaVersionTable.Row a, JavaVersionTable.Row b) {
        int aTarget = Integer.parseInt(a.getTargetVersion());
        int bTarget = Integer.parseInt(b.getTargetVersion());
        if (aTarget != bTarget) {
            return aTarget < bTarget ? a : b;
        }
        int aSource = Integer.parseInt(a.getSourceVersion());
        int bSource = Integer.parseInt(b.getSourceVersion());
        return aSource <= bSource ? a : b;
    }
}
