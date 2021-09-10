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
        return "Use java.io.Files.createTempDirectory instead of Guava";
    }

    @Override
    public String getDescription() {
        return "Replaces Guava `com.google.common.io.Files.createTempDir()` with `java.io.Files.createTempDirectory(..)`.  Note, java.io.Files.createTempDirectory() throws an IOException, if necessary a private method will be added to the class for invoking java.io.Files.createTempDirectory() and rethrowing the IOException as a new RuntimeException.";
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
                Cursor parent = getCursor().dropParentUntil(j -> j instanceof J.ClassDeclaration
                        || j instanceof J.MethodDeclaration
                        || j instanceof J.MethodInvocation);
                J parentValue = parent.getValue();
                if (parentValue instanceof J.ClassDeclaration) {
                    mi = toGeneratedCreateTempDirMethod(mi);
                } else if (parentValue instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) parentValue;
                    if (methodThrowsIOException(md)) {
                        mi = toFilesCreateTempDir(mi);
                    } else {
                        mi = toGeneratedCreateTempDirMethod(mi);
                    }
                } else if (parentValue instanceof J.MethodInvocation) {
                    mi = toGeneratedCreateTempDirMethod(mi);
                }
            }
            return mi;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
            if (getCursor().pollMessage("NEEDS_CREATE_DIR") != null) {
                JavaTemplate createDirTemplate = JavaTemplate.builder(this::getCursor,
                        "private static File createTempDirectory() {" +
                        "    try {" +
                        "        return Files.createTempDirectory(null).toFile();" +
                        "    }catch (IOException exception) {" +
                        "        throw new RuntimeException(exception);" +
                        "    }" +
                        "}").imports("java.nio.file.Files", "java.io.File", "java.io.IOException").build();
                cd = cd.withTemplate(createDirTemplate, cd.getBody().getCoordinates().lastStatement());
            }
            return cd;
        }

        private J.MethodInvocation toFilesCreateTempDir(J.MethodInvocation methodInvocation) {
            JavaTemplate t = JavaTemplate.builder(this::getCursor, "Files.createTempDirectory(null).toFile()")
                    .imports("java.nio.file.Files", "java.io.File").build();
            return methodInvocation.withTemplate(t, methodInvocation.getCoordinates().replace());
        }

        private J.MethodInvocation toGeneratedCreateTempDirMethod(J.MethodInvocation methodInvocation) {
            if (!createTempDirectoryExists()) {
                getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage("NEEDS_CREATE_DIR", Boolean.TRUE);
            }
            JavaTemplate t = JavaTemplate.builder(this::getCursor, "createTempDirectory()")
                    .imports("java.nio.file.Files", "java.io.File").build();
            methodInvocation = methodInvocation.withTemplate(t, methodInvocation.getCoordinates().replace());
            return methodInvocation;
        }

        private boolean createTempDirectoryExists() {
            J.ClassDeclaration cd = getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).getValue();
            for (Statement statement : cd.getBody().getStatements()) {
                if (statement instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration)statement;
                    if ("createTempDirectory".equals(md.getName().getSimpleName()) && md.getType() != null && md.getType().getResolvedSignature() != null) {
                        JavaType.FullyQualified fqReturn = TypeUtils.asFullyQualified(md.getType().getResolvedSignature().getReturnType());
                        if (fqReturn != null && "java.io.File".equals(fqReturn.getFullyQualifiedName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean methodThrowsIOException(J.MethodDeclaration md) {
            return md.getThrows() != null && md.getThrows().stream().anyMatch(n -> {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(n.getType());
                return fq != null && fq.getFullyQualifiedName().matches("java.io.IOException");
            });
        }
    }
}
