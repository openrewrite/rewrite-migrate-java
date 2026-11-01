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
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.groovy.GroovyTemplate;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.kotlin.KotlinTemplate;
import org.openrewrite.kotlin.tree.K;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddNebulaJavaToolchain extends Recipe {
    String displayName = "Add a Java 25 toolchain for Nebula Netflix OSS";

    String description = "Add a Java 25 toolchain to Nebula Netflix OSS projects that do not declare a compiler " +
            "level or toolchain. The plugin resets source compatibility to Java 8 after evaluation unless a toolchain is configured.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile source = (SourceFile) tree;
                String fileName = source.getSourcePath().getFileName().toString();
                if (!("build.gradle".equals(fileName) || "build.gradle.kts".equals(fileName))) {
                    return tree;
                }
                GradleProject project = source.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (project == null || project.getPlugins().stream().noneMatch(p ->
                        "nebula.netflixoss".equals(p.getId()) || "com.netflix.nebula.netflixoss".equals(p.getId()))) {
                    return tree;
                }
                boolean[] configured = {false};
                new JavaIsoVisitor<boolean[]>() {
                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, boolean[] found) {
                        Expression variable = assignment.getVariable();
                        String name = propertyName(variable);
                        if ("sourceCompatibility".equals(name) || "targetCompatibility".equals(name) ||
                                isLanguageVersion(variable) || isCompilerRelease(variable)) {
                            found[0] = true;
                        }
                        return super.visitAssignment(assignment, found);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, boolean[] found) {
                        String name = method.getSimpleName();
                        if ("setSourceCompatibility".equals(name) || "setTargetCompatibility".equals(name) ||
                                "sourceCompatibility".equals(name) || "targetCompatibility".equals(name) ||
                                ("jvmToolchain".equals(name) && method.getArguments().size() == 1 &&
                                        !(method.getArguments().get(0) instanceof J.Lambda)) ||
                                ("set".equals(name) && (isLanguageVersion(method.getSelect()) || isCompilerRelease(method.getSelect())))) {
                            found[0] = true;
                        }
                        return super.visitMethodInvocation(method, found);
                    }

                    private boolean isLanguageVersion(@Nullable Expression expression) {
                        if (!"languageVersion".equals(propertyName(expression))) {
                            return false;
                        }
                        if (expression instanceof J.FieldAccess &&
                                "toolchain".equals(propertyName(((J.FieldAccess) expression).getTarget()))) {
                            return true;
                        }
                        return getCursor().getPathAsStream().anyMatch(value -> value instanceof J.MethodInvocation &&
                                ("toolchain".equals(((J.MethodInvocation) value).getSimpleName()) ||
                                        "jvmToolchain".equals(((J.MethodInvocation) value).getSimpleName())));
                    }

                    private boolean isCompilerRelease(@Nullable Expression expression) {
                        if (!"release".equals(propertyName(expression))) {
                            return false;
                        }
                        if (expression instanceof J.FieldAccess &&
                                "options".equals(propertyName(((J.FieldAccess) expression).getTarget()))) {
                            return true;
                        }
                        return getCursor().getPathAsStream().anyMatch(value -> value instanceof J.MethodInvocation &&
                                "options".equals(((J.MethodInvocation) value).getSimpleName()));
                    }

                    private @Nullable String propertyName(@Nullable Expression expression) {
                        if (expression instanceof J.Identifier) {
                            return ((J.Identifier) expression).getSimpleName();
                        }
                        if (expression instanceof J.FieldAccess) {
                            return ((J.FieldAccess) expression).getSimpleName();
                        }
                        return null;
                    }
                }.visit(tree, configured);
                if (configured[0]) {
                    return tree;
                }
                Cursor scope = new Cursor(getCursor(), tree);
                if (tree instanceof G.CompilationUnit) {
                    G.CompilationUnit c = (G.CompilationUnit) tree;
                    if (c.getStatements().isEmpty()) {
                        return tree;
                    }
                    Statement last = c.getStatements().get(c.getStatements().size() - 1);
                    G.CompilationUnit updated = GroovyTemplate.builder("java {\n    toolchain {\n        languageVersion = JavaLanguageVersion.of(25)\n    }\n}")
                            .build().apply(scope, last.getCoordinates().after());
                    return updated.withStatements(ListUtils.mapLast(updated.getStatements(),
                            statement -> statement.withPrefix(Space.format("\n\n"))));
                }
                if (tree instanceof K.CompilationUnit) {
                    K.CompilationUnit c = (K.CompilationUnit) tree;
                    if (c.getStatements().isEmpty() || !(c.getStatements().get(0) instanceof J.Block)) {
                        return tree;
                    }
                    List<Statement> statements = ((J.Block) c.getStatements().get(0)).getStatements();
                    if (statements.isEmpty()) {
                        return tree;
                    }
                    Statement last = statements.get(statements.size() - 1);
                    K.CompilationUnit updated = KotlinTemplate.builder("java {\n    toolchain {\n        languageVersion.set(JavaLanguageVersion.of(25))\n    }\n}")
                            .build().apply(scope, last.getCoordinates().after());
                    return updated.withStatements(ListUtils.mapFirst(updated.getStatements(), first ->
                            ((J.Block) first).withStatements(ListUtils.mapLast(((J.Block) first).getStatements(),
                                    statement -> statement.withPrefix(Space.format("\n\n"))))));
                }
                return tree;
            }
        };
    }
}
