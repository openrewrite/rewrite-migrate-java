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

package org.openrewrite.java.migrate.io;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceFileInOrOutputStreamFinalizeWithClose extends Recipe {

    private static final String JAVA_IO_FILE_INPUT_STREAM = "java.io.FileInputStream";
    private static final String JAVA_IO_FILE_OUTPUT_STREAM = "java.io.FileOutputStream";
    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.lang.Object finalize()");

    @Override
    public String getDisplayName() {
        return "Replace invocations of `finalize()` on `FileInputStream` and `FileOutputStream` with `close()`";
    }

    @Override
    public String getDescription() {
        return "Replace invocations of the deprecated `finalize()` method on `FileInputStream` and `FileOutputStream` with `close()`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("JDK-8212050");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesJavaVersion<>(9, 11), new UsesMethod<>(METHOD_MATCHER)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (METHOD_MATCHER.matches(mi)) {
                            Expression select = mi.getSelect();
                            JavaType type = select != null ? select.getType() : getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class).getType();
                            if (TypeUtils.isAssignableTo(JAVA_IO_FILE_INPUT_STREAM, type) ||
                                TypeUtils.isAssignableTo(JAVA_IO_FILE_OUTPUT_STREAM, type)) {
                                return mi.withName(mi.getName().withSimpleName("close"));
                            }
                        }
                        return mi;
                    }
                }
        );
    }
}
