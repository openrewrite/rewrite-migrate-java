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
import org.openrewrite.Cursor;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.UpdateJavaCompatibility;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.GradlePlugin;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.kotlin.KotlinIsoVisitor;

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
        if (source instanceof G.CompilationUnit) {
            new GroovyIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    recordCompilerVersion(method, getCursor(), target);
                    return super.visitMethodInvocation(method, ctx);
                }
            }.visit(source, ctx);
        } else if (source instanceof K.CompilationUnit) {
            new KotlinIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    recordCompilerVersion(method, getCursor(), target);
                    return super.visitMethodInvocation(method, ctx);
                }
            }.visit(source, ctx);
        }
        return target[0];
    }

    private static void recordCompilerVersion(J.MethodInvocation method, Cursor cursor, int[] target) {
        Cursor declaration = pluginDeclaration(cursor);
        if (declaration == null || !topLevelPlugins(declaration)) {
            return;
        }
        if (isMainCompiler(method)) {
            // A versionless compiler can be supplied by settings or an external convention: its target is unknown.
            if (isAppliedDeclaration(declaration)) {
                target[0] = Math.min(target[0], 8);
            }
            return;
        }
        // Check the literal DSL shape before invoking the trait: custom DSLs may have non-string id/version arguments.
        if (!"version".equals(method.getSimpleName()) || method.getArguments().size() != 1 ||
            !(method.getSelect() instanceof J.MethodInvocation)) {
            return;
        }
        J.MethodInvocation id = (J.MethodInvocation) method.getSelect();
        if (id.getSelect() != null || !("id".equals(id.getSimpleName()) || "kotlin".equals(id.getSimpleName())) ||
            id.getArguments().size() != 1 || !(id.getArguments().get(0) instanceof J.Literal) ||
            !(((J.Literal) id.getArguments().get(0)).getValue() instanceof String)) {
            return;
        }
        if (!(method.getArguments().get(0) instanceof J.Literal) ||
            !(((J.Literal) method.getArguments().get(0)).getValue() instanceof String)) {
            String pluginId = (String) ((J.Literal) id.getArguments().get(0)).getValue();
            if (("org.jetbrains.kotlin.jvm".equals(pluginId) || "kotlin".equals(id.getSimpleName()) && "jvm".equals(pluginId)) &&
                isAppliedDeclaration(declaration)) {
                target[0] = Math.min(target[0], 8);
            }
            return;
        }
        new GradlePlugin.Matcher().pluginIdPattern("org.jetbrains.kotlin.*").get(declaration)
                .filter(plugin -> plugin.isApplied() &&
                        ("org.jetbrains.kotlin.jvm".equals(plugin.getPluginId()) ||
                         "org.jetbrains.kotlin.android".equals(plugin.getPluginId())))
                .ifPresent(plugin -> target[0] = Math.min(target[0], supportedTarget(plugin.getVersion())));
    }

    private static @Nullable Cursor pluginDeclaration(Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        if (parent.getValue() instanceof J.MethodInvocation) {
            J.MethodInvocation apply = parent.getValue();
            J.MethodInvocation method = cursor.getValue();
            if ("apply".equals(apply.getSimpleName()) && apply.getSelect() != null &&
                apply.getSelect().getId().equals(method.getId())) {
                // The trait's applied flag belongs to the outer apply invocation, not its inner version/id.
                if (apply.getArguments().size() != 1 || !(apply.getArguments().get(0) instanceof J.Literal) ||
                    !(((J.Literal) apply.getArguments().get(0)).getValue() instanceof Boolean)) {
                    return null;
                }
                return parent;
            }
        }
        return cursor;
    }

    private static boolean isAppliedDeclaration(Cursor cursor) {
        J.MethodInvocation method = cursor.getValue();
        return !"apply".equals(method.getSimpleName()) ||
               Boolean.TRUE.equals(((J.Literal) method.getArguments().get(0)).getValue());
    }

    private static boolean isMainCompiler(J.MethodInvocation method) {
        if (method.getSelect() != null || method.getArguments().size() != 1 ||
            !(method.getArguments().get(0) instanceof J.Literal)) {
            return false;
        }
        Object id = ((J.Literal) method.getArguments().get(0)).getValue();
        return "id".equals(method.getSimpleName()) && "org.jetbrains.kotlin.jvm".equals(id) ||
               "kotlin".equals(method.getSimpleName()) && "jvm".equals(id);
    }

    private static boolean topLevelPlugins(Cursor cursor) {
        java.util.Iterator<Object> path = cursor.getParentOrThrow().getPath();
        boolean found = false;
        while (path.hasNext()) {
            Object value = path.next();
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) value;
                if (found || !"plugins".equals(method.getSimpleName()) || method.getSelect() != null) {
                    return false;
                }
                found = true;
            }
        }
        return found;
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
