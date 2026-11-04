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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NoGuavaCreateTempDir extends Recipe {
    private static final MethodMatcher GUAVA_CREATE_TEMP_DIR = new MethodMatcher("com.google.common.io.Files createTempDir()");

    @Getter
    final String displayName = "Prefer `Files#createTempDirectory()`";

    @Getter
    final String description = "Replaces Guava `Files#createTempDir()` with Java `Files#createTempDirectory(..)`. Transformations are limited to scopes throwing or catching `java.io.IOException`.";

    @Getter
    final Set<String> tags = new HashSet<>( Arrays.asList( "RSPEC-S4738", "guava" ) );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GUAVA_CREATE_TEMP_DIR), new NoGuavaTempDirVisitor());
    }

    private static class NoGuavaTempDirVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            maybeRemoveImport("com.google.common.io.Files");
            maybeAddImport("java.nio.file.Files");
            maybeAddImport("java.io.IOException");
            maybeAddImport("java.io.File");
            return c;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (GUAVA_CREATE_TEMP_DIR.matches(mi) && isIOExceptionHandled()) {
                mi = toFilesCreateTempDir(mi);
            }
            return mi;
        }

        /**
         * A {@code try} handles what its body throws, not what its catch or finally blocks throw.
         * Lambdas and class bodies end the search, as nothing around them handles what they throw.
         */
        private boolean isIOExceptionHandled() {
            J child = null;
            for (Iterator<Object> path = getCursor().getPath(); path.hasNext(); ) {
                Object value = path.next();
                if (!(value instanceof J)) {
                    continue;
                }
                if (value instanceof J.Lambda || value instanceof J.ClassDeclaration ||
                    value instanceof J.NewClass && ((J.NewClass) value).getBody() == child) {
                    return false;
                }
                if (value instanceof J.MethodDeclaration) {
                    J.MethodDeclaration md = (J.MethodDeclaration) value;
                    return md.getThrows() != null && md.getThrows().stream().anyMatch(n -> coversIOException(n.getType()));
                }
                if (value instanceof J.Try && ((J.Try) value).getBody() == child &&
                    ((J.Try) value).getCatches().stream().anyMatch(c -> coversIOException(c.getParameter().getType()))) {
                    return true;
                }
                child = (J) value;
            }
            return false;
        }

        private boolean coversIOException(@Nullable JavaType type) {
            if (type instanceof JavaType.MultiCatch) {
                return ((JavaType.MultiCatch) type).getThrowableTypes().stream().anyMatch(this::coversIOException);
            }
            return TypeUtils.isOfClassType(type, "java.io.IOException") ||
                   TypeUtils.isOfClassType(type, "java.lang.Exception") ||
                   TypeUtils.isOfClassType(type, "java.lang.Throwable");
        }

        private J.MethodInvocation toFilesCreateTempDir(J.MethodInvocation methodInvocation) {
            return JavaTemplate.builder("Files.createTempDirectory(null).toFile()")
                    .imports("java.nio.file.Files", "java.io.File")
                    .build()
                    .apply(updateCursor(methodInvocation), methodInvocation.getCoordinates().replace());
        }
    }
}
