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
package org.openrewrite.java.migrate.apache.commons.io;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ApacheFileUtilsToJavaFiles extends Recipe {
    @Override
    public String getDisplayName() {
        return "Prefer `java.nio.file.Files`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library's `java.nio.file.Files` over third-party usage of apache's `apache.commons.io.FileUtils`.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("apache", "commons"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.apache.commons.io.FileUtils", false), new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher readFileToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readFileToByteArray(java.io.File)");
            private final MethodMatcher readLinesToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File)");
            private final MethodMatcher readLinesWithCharsetToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File, java.nio.charset.Charset)");
            private final MethodMatcher readLinesWithCharsetIdToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File, String)");

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                if (c != cu) {
                    maybeAddImport("java.nio.file.Files");
                    maybeRemoveImport("org.apache.commons.io.FileUtils");
                }
                return c;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (readFileToByteArrayMatcher.matches(mi)) {
                    return mi.withTemplate(JavaTemplate.builder(this::getCursor, "Files.readAllBytes(#{any(java.io.File)}.toPath())")
                            .imports("java.nio.file.Files").build(), mi.getCoordinates().replace(), mi.getArguments().get(0));
                } else if (readLinesToByteArrayMatcher.matches(mi)) {
                    return mi.withTemplate(JavaTemplate.builder(this::getCursor, "Files.readAllLines(#{any(java.io.File)}.toPath())")
                            .imports("java.nio.file.Files").build(), mi.getCoordinates().replace(), mi.getArguments().get(0));
                } else if (readLinesWithCharsetToByteArrayMatcher.matches(mi)) {
                    return mi.withTemplate(JavaTemplate.builder(this::getCursor, "Files.readAllLines(#{any(java.io.File)}.toPath(), #{any(java.nio.charset.Charset)})")
                            .imports("java.nio.file.Files", "java.nio.charset.Charset").build(), mi.getCoordinates().replace(), mi.getArguments().get(0), mi.getArguments().get(1));
                } else if (readLinesWithCharsetIdToByteArrayMatcher.matches(mi)) {
                    return mi.withTemplate(JavaTemplate.builder(this::getCursor, "Files.readAllLines(#{any(java.io.File)}.toPath(), Charset.forName(#{any(String)}))")
                            .imports("java.nio.file.Files", "java.nio.charset.Charset").build(), mi.getCoordinates().replace(), mi.getArguments().get(0), mi.getArguments().get(1));
                }
                return mi;
            }
        });
    }
}
