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
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

import java.time.Duration;

public class ApacheFileUtilsToJavaFiles extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use java.nio.file.Files";
    }

    @Override
    public String getDescription() {
        return "Migrate `apache.commons.io.FileUtils` to `java.nio.file.Files`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("org.apache.commons.io.FileUtils");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher readFileToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readFileToByteArray(java.io.File)");
            private final MethodMatcher readLinesToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File)");
            private final MethodMatcher readLinesWithCharsetToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File, java.nio.charset.Charset)");
            private final MethodMatcher readLinesWithCharsetIdToByteArrayMatcher = new MethodMatcher("org.apache.commons.io.FileUtils readLines(java.io.File, String)");

            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                JavaSourceFile sf = super.visitJavaSourceFile(cu, executionContext);
                if (sf != cu) {
                    maybeAddImport("java.nio.file.Files");
                    maybeRemoveImport("org.apache.commons.io.FileUtils");
                }
                return sf;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
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
        };
    }
}
