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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.UpdateJavaCompatibility;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.kotlin.tree.K;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeGradleJavaVersion extends Recipe {
    @Option(displayName = "Java version", description = "The requested Java version, bounded by the Kotlin compiler's supported target.", example = "25")
    Integer version;
    String displayName = "Align Gradle Java and Kotlin bytecode targets";
    String description = "Upgrade Java and Kotlin together using the actual Kotlin Gradle plugin version, preserving Kotlin 1.x and unmodeled nested build boundaries.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof G.CompilationUnit) && !(tree instanceof K.CompilationUnit)) {
                    return tree;
                }
                SourceFile source = (SourceFile) tree;
                String path = source.getSourcePath().toString().replace('\\', '/');
                if (!path.endsWith("build.gradle") && !path.endsWith("build.gradle.kts")) {
                    return tree;
                }
                if (path.contains("/") && !source.getMarkers().findFirst(GradleProject.class).isPresent() &&
                    !source.getMarkers().findFirst(JavaProject.class).isPresent() &&
                    !source.getMarkers().findFirst(JavaVersion.class).isPresent()) {
                    return tree;
                }
                int target = targetFor(source, version, ctx);
                Tree updated = new UpdateJavaCompatibility(target, null, null, false, null).getVisitor().visit(tree, ctx);
                return new UpgradeKotlinJvmTargetVersion(target).getVisitor().visit(updated, ctx);
            }
        };
    }

    static int targetFor(SourceFile source, int requested, ExecutionContext ctx) {
        String path = source.getSourcePath().toString().replace('\\', '/');
        if (path.contains("/") && !source.getMarkers().findFirst(GradleProject.class).isPresent() &&
            !source.getMarkers().findFirst(JavaProject.class).isPresent() &&
            !source.getMarkers().findFirst(JavaVersion.class).isPresent()) {
            return 8;
        }
        int[] target = {requested};
        new GradlePlugin.Matcher().pluginIdPattern("org.jetbrains.kotlin.*").asVisitor((plugin, p) -> {
            target[0] = Math.min(target[0], supportedTarget(plugin.getVersion()));
            return plugin.getTree();
        }).visit(source, ctx);
        return target[0];
    }

    private static final Pattern COMPILER_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?(?:[-+].*)?");

    private static int supportedTarget(@Nullable String compiler) {
        Matcher match = COMPILER_VERSION.matcher(compiler == null ? "" : compiler);
        if (!match.matches()) {
            // An unresolved compiler version is not evidence that it supports a new bytecode level.
            return 8;
        }
        int major = Integer.parseInt(match.group(1));
        int minor = Integer.parseInt(match.group(2));
        int patch = match.group(3) == null ? 0 : Integer.parseInt(match.group(3));
        if (major > 2 || major == 2 && minor >= 3) return 25;
        if (major == 2) return minor == 2 ? 24 : minor == 1 ? 23 : 22;
        if (major == 1 && minor >= 9) return patch >= 20 ? 21 : 20;
        if (major == 1 && minor == 8) return 19;
        return 8;
    }
}
