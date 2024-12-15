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

import org.openrewrite.*;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionMigrationPlan;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.marker.Markers;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class PlanJavaMigration extends ScanningRecipe<JavaVersionMigrationPlan.Row.Builder> {
    transient JavaVersionMigrationPlan plan = new JavaVersionMigrationPlan(this);

    @Override
    public String getDisplayName() {
        return "Plan a Java version migration";
    }

    @Override
    public String getDescription() {
        return "Study the set of Java versions and associated tools in " +
               "use across many repositories.";
    }

    @Override
    public JavaVersionMigrationPlan.Row.Builder getInitialValue(ExecutionContext ctx) {
        return JavaVersionMigrationPlan.Row.builder();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(JavaVersionMigrationPlan.Row.Builder acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    File sourceFile = ((SourceFile) tree).getSourcePath().toFile();
                    if (sourceFile.getName().contains("build.gradle")) {
                        acc.hasGradleBuild(true);
                    } else if (sourceFile.getName().contains("pom.xml")) {
                        acc.hasMavenPom(true);
                    }
                }

                if (tree instanceof JavaSourceFile) {
                    acc.hasJava(true);
                    Markers markers = tree.getMarkers();
                    markers.findFirst(JavaVersion.class).ifPresent(javaVersion -> {
                        acc.sourceCompatibility(javaVersion.getSourceCompatibility());
                        acc.majorVersionSourceCompatibility(javaVersion.getMajorVersion());
                        acc.targetCompatibility(javaVersion.getTargetCompatibility());
                    });
                    markers.findFirst(BuildTool.class).ifPresent(buildTool -> {
                        switch (buildTool.getType()) {
                            case Gradle:
                                acc.gradleVersion(buildTool.getVersion());
                                break;
                            case Maven:
                                acc.mavenVersion(buildTool.getVersion());
                                break;
                        }
                    });
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(JavaVersionMigrationPlan.Row.Builder acc, ExecutionContext ctx) {
        plan.insertRow(ctx, acc.build());
        return Collections.emptyList();
    }
}
