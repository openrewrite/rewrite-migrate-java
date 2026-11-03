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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaFilesToString extends Recipe {
    private static final MethodMatcher FILES_TO_STRING = new MethodMatcher("com.google.common.io.Files toString(java.io.File, java.nio.charset.Charset)");
    private static final MethodMatcher FILES_AS_CHAR_SOURCE = new MethodMatcher("com.google.common.io.Files asCharSource(java.io.File, java.nio.charset.Charset)");
    private static final MethodMatcher CHAR_SOURCE_READ = new MethodMatcher("com.google.common.io.CharSource read()");

    @Getter
    final String displayName = "Prefer `new String(Files.readAllBytes(path), charset)`";

    @Getter
    final String description = "Replace Guava's `Files.toString(File, Charset)` and `Files.asCharSource(File, Charset).read()` " +
            "with `new String(java.nio.file.Files.readAllBytes(file.toPath()), charset)`. Like Guava, it replaces malformed " +
            "input with the replacement character, where `Files.readString(Path, Charset)` throws a `MalformedInputException`.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(FILES_TO_STRING), new UsesMethod<>(FILES_AS_CHAR_SOURCE)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        List<Expression> fileAndCharset;
                        if (FILES_TO_STRING.matches(m)) {
                            fileAndCharset = m.getArguments();
                        } else if (CHAR_SOURCE_READ.matches(m) && FILES_AS_CHAR_SOURCE.matches(m.getSelect())) {
                            fileAndCharset = ((J.MethodInvocation) m.getSelect()).getArguments();
                        } else {
                            return m;
                        }
                        maybeRemoveImport("com.google.common.io.Files");
                        J readAllBytes = JavaTemplate.builder("new String(java.nio.file.Files.readAllBytes(#{any(java.io.File)}.toPath()), #{any(java.nio.charset.Charset)})")
                                .build()
                                .apply(getCursor(), m.getCoordinates().replace(), fileAndCharset.get(0), fileAndCharset.get(1));
                        doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(readAllBytes));
                        return readAllBytes;
                    }
                }
        );
    }
}
