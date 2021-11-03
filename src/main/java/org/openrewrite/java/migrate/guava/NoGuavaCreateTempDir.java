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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Collections;
import java.util.Set;

public class NoGuavaCreateTempDir extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `Files#createTempDirectory()` instead of Guava";
    }

    @Override
    public String getDescription() {
        return "Replaces Guava `Files#createTempDir()` with Java `Files#createTempDirectory(..)`. Transformations are limited to scopes throwing or catching `java.io.IOException`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-4738");
    }

    @Override
    protected UsesType<ExecutionContext> getApplicableTest() {
        return new UsesType<>("com.google.common.io.Files");
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("com.google.common.io.Files createTempDir()");
    }

    @Override
    protected NoGuavaTempDirVisitor getVisitor() {
        return new NoGuavaTempDirVisitor();
    }

    private static class NoGuavaTempDirVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher guavaCreateTempDirMatcher = new MethodMatcher("com.google.common.io.Files createTempDir()");

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
            maybeAddImport("java.nio.file.Files");
            maybeAddImport("java.io.IOException");
            maybeAddImport("java.io.File");
            maybeRemoveImport("com.google.common.io.Files");
            return c;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            if (guavaCreateTempDirMatcher.matches(mi)) {
                Cursor parent = getCursor().dropParentUntil(j -> j instanceof J.MethodDeclaration || j instanceof J.Try || j instanceof J.ClassDeclaration);
                J parentValue = parent.getValue();
                if (parentValue instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) parentValue;
                    if (md.getThrows() != null && md.getThrows().stream().anyMatch(n -> isIOExceptionOrException(TypeUtils.asFullyQualified(n.getType())))) {
                        mi = toFilesCreateTempDir(mi);
                    }
                } else if (parentValue instanceof J.Try) {
                    J.Try tr = (J.Try) parentValue;
                    if (tr.getCatches().stream().anyMatch(n -> isIOExceptionOrException(TypeUtils.asFullyQualified(n.getParameter().getTree().getType())))) {
                        mi = toFilesCreateTempDir(mi);
                    }
                }
            }
            return mi;
        }

        private boolean isIOExceptionOrException(@Nullable JavaType.FullyQualified fqCatch) {
            return fqCatch != null &&
                    ("java.io.IOException".matches(fqCatch.getFullyQualifiedName())
                            || "java.lang.Exception".matches(fqCatch.getFullyQualifiedName()));
        }

        private J.MethodInvocation toFilesCreateTempDir(J.MethodInvocation methodInvocation) {
            JavaTemplate t = JavaTemplate.builder(this::getCursor, "Files.createTempDirectory(null).toFile()")
                    .imports("java.nio.file.Files", "java.io.File").build();
            return methodInvocation.withTemplate(t, methodInvocation.getCoordinates().replace());
        }
    }
}
