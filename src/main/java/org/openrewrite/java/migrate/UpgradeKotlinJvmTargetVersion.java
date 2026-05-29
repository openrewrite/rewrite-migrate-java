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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.KotlinIsoVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.Iterator;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeKotlinJvmTargetVersion extends Recipe {

    // Gradle build scripts usually lack type attribution, so match by name + arity (matchUnknownTypes=true at call sites).
    private static final MethodMatcher KOTLIN_OPTIONS = new MethodMatcher("* kotlinOptions(..)");
    private static final MethodMatcher COMPILER_OPTIONS = new MethodMatcher("* compilerOptions(..)");
    private static final MethodMatcher JVM_TARGET_SET = new MethodMatcher("* set(..)");

    @Option(displayName = "Java version",
            description = "The Java version to align Kotlin's `jvmTarget` with.",
            example = "21")
    Integer version;

    String displayName = "Upgrade Kotlin `jvmTarget` to match the Java version";

    String description = "Align the Kotlin `jvmTarget` with the project's Java version so the Kotlin compiler emits " +
            "bytecode at the same level as `javac`. Covers `kotlin-maven-plugin` `<jvmTarget>` configuration and the " +
            "Gradle `kotlinOptions { jvmTarget = ... }` / `compilerOptions { jvmTarget = ... }` blocks (Groovy and " +
            "Kotlin DSL). Will not downgrade if the existing Kotlin target is higher than the requested version.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String newVersion = version.toString();
        int target = version;
        // Each language visitor already accepts only its own source kind (the Maven visitor even narrows to actual
        // Maven poms), so route on `isAcceptable` rather than re-checking compilation-unit types here.
        TreeVisitor<?, ExecutionContext> maven = mavenVisitor(target);
        TreeVisitor<?, ExecutionContext> groovy = gradleGroovyVisitor(newVersion, target);
        TreeVisitor<?, ExecutionContext> kotlin = gradleKotlinDslVisitor(newVersion, target);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    if (maven.isAcceptable(sourceFile, ctx)) {
                        return maven.visit(tree, ctx);
                    }
                    if (groovy.isAcceptable(sourceFile, ctx)) {
                        return groovy.visit(tree, ctx);
                    }
                    if (kotlin.isAcceptable(sourceFile, ctx)) {
                        return kotlin.visit(tree, ctx);
                    }
                }
                return tree;
            }
        };
    }

    private static MavenIsoVisitor<ExecutionContext> mavenVisitor(int target) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isPluginTag("org.jetbrains.kotlin", "kotlin-maven-plugin")) {
                    return t;
                }
                Xml.Tag jvmTarget = t.getChild("configuration")
                        .flatMap(config -> config.getChild("jvmTarget"))
                        .orElse(null);
                if (jvmTarget == null) {
                    return t;
                }
                Integer current = parseJvmTarget(jvmTarget.getValue().orElse(null));
                if (current == null || current >= target) {
                    return t;
                }
                return (Xml.Tag) new ChangeTagValueVisitor<>(jvmTarget, Integer.toString(target)).visitNonNull(t, ctx);
            }
        };
    }

    private static GroovyIsoVisitor<ExecutionContext> gradleGroovyVisitor(String newVersion, int target) {
        return new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = super.visitAssignment(assignment, ctx);
                if (!isInsideKotlinCompilerBlock(getCursor())) {
                    return a;
                }
                Expression variable = a.getVariable();
                if (!(variable instanceof J.Identifier)
                        || !"jvmTarget".equals(((J.Identifier) variable).getSimpleName())) {
                    return a;
                }
                Expression rhs = a.getAssignment();
                if (!(rhs instanceof J.Literal)) {
                    return a;
                }
                J.Literal literal = (J.Literal) rhs;
                Object value = literal.getValue();
                if (!(value instanceof String)) {
                    return a;
                }
                Integer current = parseJvmTarget((String) value);
                if (current == null || current >= target) {
                    return a;
                }
                // Preserve the original quote style (Groovy allows both ' and ").
                String original = literal.getValueSource();
                char quote = original != null && !original.isEmpty() ? original.charAt(0) : '\'';
                return a.withAssignment(literal
                        .withValue(newVersion)
                        .withValueSource(quote + newVersion + quote));
            }
        };
    }

    private static KotlinIsoVisitor<ExecutionContext> gradleKotlinDslVisitor(String newVersion, int target) {
        return new KotlinIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = super.visitAssignment(assignment, ctx);
                if (!isInsideKotlinCompilerBlock(getCursor())) {
                    return a;
                }
                Expression variable = a.getVariable();
                if (!(variable instanceof J.Identifier)
                        || !"jvmTarget".equals(((J.Identifier) variable).getSimpleName())) {
                    return a;
                }
                Expression rhs = a.getAssignment();
                if (rhs instanceof J.Literal) {
                    return bumpLiteralAssignment(a, (J.Literal) rhs, newVersion, target);
                }
                if (rhs instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) rhs;
                    J.FieldAccess bumped = bumpedJvmTargetFieldAccess(fa, newVersion, target);
                    return bumped == fa ? a : a.withAssignment(bumped);
                }
                return a;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                // Match the Provider-style setter `jvmTarget.set(JvmTarget.JVM_X)`.
                if (!JVM_TARGET_SET.matches(mi, true) || !(mi.getSelect() instanceof J.Identifier)) {
                    return mi;
                }
                if (!"jvmTarget".equals(((J.Identifier) mi.getSelect()).getSimpleName())) {
                    return mi;
                }
                if (!isInsideKotlinCompilerBlock(getCursor())) {
                    return mi;
                }
                if (mi.getArguments().size() != 1) {
                    return mi;
                }
                Expression arg = mi.getArguments().get(0);
                if (arg instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) arg;
                    J.FieldAccess bumped = bumpedJvmTargetFieldAccess(fa, newVersion, target);
                    if (bumped != fa) {
                        return mi.withArguments(Collections.singletonList(bumped));
                    }
                }
                return mi;
            }
        };
    }

    // Gradle build scripts lack type attribution, so there is no trait that models "inside the compilerOptions
    // block" — walk the cursor ancestry and match the enclosing DSL block by name (matchUnknownTypes=true), the
    // same approach `org.openrewrite.gradle.UpdateJavaCompatibility` uses for `java { }` / `sourceCompatibility`.
    private static boolean isInsideKotlinCompilerBlock(Cursor cursor) {
        Iterator<Object> path = cursor.getPath();
        while (path.hasNext()) {
            Object o = path.next();
            if (o instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) o;
                if (KOTLIN_OPTIONS.matches(mi, true) || COMPILER_OPTIONS.matches(mi, true)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static J.Assignment bumpLiteralAssignment(J.Assignment a, J.Literal literal, String newVersion, int target) {
        Object value = literal.getValue();
        if (!(value instanceof String)) {
            return a;
        }
        Integer current = parseJvmTarget((String) value);
        if (current == null || current >= target) {
            return a;
        }
        return a.withAssignment(literal
                .withValue(newVersion)
                .withValueSource("\"" + newVersion + "\""));
    }

    /**
     * If the FieldAccess is a {@code JvmTarget.JVM_X} reference with X less than {@code target},
     * return a new FieldAccess with the bumped enum name. Returns the original instance unchanged otherwise.
     */
    private static J.FieldAccess bumpedJvmTargetFieldAccess(J.FieldAccess fa, String newVersion, int target) {
        if (!(fa.getTarget() instanceof J.Identifier)
                || !"JvmTarget".equals(((J.Identifier) fa.getTarget()).getSimpleName())) {
            return fa;
        }
        String enumName = fa.getSimpleName();
        if (!enumName.startsWith("JVM_")) {
            return fa;
        }
        // Enum constants spell the version with underscores ("JVM_1_8", "JVM_11"); normalize to the dotted/plain
        // form parseJvmTarget understands ("1.8", "11").
        Integer current = parseJvmTarget(enumName.substring("JVM_".length()).replace('_', '.'));
        if (current == null || current >= target) {
            return fa;
        }
        return fa.withName(fa.getName().withSimpleName("JVM_" + newVersion));
    }

    /**
     * Parse a Kotlin {@code jvmTarget} string ("1.8", "11", "21") to the corresponding major Java version.
     * Returns {@code null} if the value cannot be parsed (do-no-harm).
     */
    private static @Nullable Integer parseJvmTarget(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if ("1.8".equals(trimmed)) {
            return 8;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
