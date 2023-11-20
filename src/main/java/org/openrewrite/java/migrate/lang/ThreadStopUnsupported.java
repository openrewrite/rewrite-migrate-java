/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.marker.Markers;

import java.util.Collections;

public class ThreadStopUnsupported extends Recipe {
    private static final MethodMatcher THREAD_STOP_MATCHER = new MethodMatcher("java.lang.Thread stop()");

    @Override
    public String getDisplayName() {
        return "Replace `Thread.stop()` with `throw new UnsupportedOperationException()`";
    }

    @Override
    public String getDescription() {
        return "`Thread.stop()` always throws a `new UnsupportedOperationException` in Java 21+. " +
               "This recipe makes that explicit, as the migration is more complicated." +
               "See https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html .";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J j = super.visitMethodInvocation(method, executionContext);
                if (THREAD_STOP_MATCHER.matches(method)) {
                    if (usesJava21(executionContext)) {
                        JavaTemplate template = JavaTemplate.builder("throw new UnsupportedOperationException()")
                                .contextSensitive().build();
                        j = template.apply(getCursor(), method.getCoordinates().replace());
                    }
                    if (j.getComments().isEmpty()) {
                        j = getWithComment(j);
                    }
                }
                return j;
            }

            private boolean usesJava21(ExecutionContext executionContext) {
                JavaSourceFile javaSourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                return javaSourceFile != null && new UsesJavaVersion<>(21).visit(javaSourceFile, executionContext) != javaSourceFile;
            }

            private J getWithComment(J j) {
                String prefixWhitespace = j.getPrefix().getWhitespace();
                String commentText =
                        prefixWhitespace + " * `Thread.stop()` always throws a `new UnsupportedOperationException()` in Java 21+." +
                        prefixWhitespace + " * For detailed migration instructions see the migration guide available at" +
                        prefixWhitespace + " * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html" +
                        prefixWhitespace + " ";
                return j.withComments(Collections.singletonList(new TextComment(true, commentText, prefixWhitespace, Markers.EMPTY)));
            }
        };
    }
}
