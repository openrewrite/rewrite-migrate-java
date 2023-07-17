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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveFinalizeFromFileStream extends Recipe {

    private static final MethodMatcher JAVA_IO_FILEINPUTSTREAM = new MethodMatcher("java.io.FileInputStream finalize()", true);
    private static final MethodMatcher JAVA_IO_FILEOUTPUTSTREAM = new MethodMatcher("java.io.FileOutputStream finalize()", true);

    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.lang.Object finalize()");

    @Override
    public String getDisplayName() {
        return "Replace invocations of `finalize()` on `FileInputStream` and `FileOutputStream` with `close()`";
    }

    @Override
    public String getDescription() {
        return "Replace invocations of the deprecated finalize() method on FileInputStream and FileOutputStream with close().";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("JDK-8212050");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(9,11),
                    Preconditions.or(
                        new UsesMethod<>(JAVA_IO_FILEINPUTSTREAM),
                        new UsesMethod<>(JAVA_IO_FILEOUTPUTSTREAM))),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);

                        if (JAVA_IO_FILEINPUTSTREAM.matches(mi) || JAVA_IO_FILEOUTPUTSTREAM.matches(mi)) {
                            Expression select = mi.getSelect();
                            if (select == null) {
                                J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                                if (shouldRemoveFinalize(cd.getType())) {
                                    return mi.withName(mi.getName().withSimpleName("close"));
                                }
                            } else {
                                if (shouldRemoveFinalize(select.getType())) {
                                    List<J> sideEffects = select.getSideEffects();
                                    if (sideEffects.isEmpty()) {
                                        return mi.withName(mi.getName().withSimpleName("close"));
                                    }
                                    if (sideEffects.size() == 1) {
                                        return sideEffects.get(0).withPrefix(mi.getPrefix());
                                    }
                                }
                            }
                        }

                        return mi;
                    }

                    private boolean shouldRemoveFinalize(JavaType type) {
                        return TypeUtils.isAssignableTo(JAVA_IO_FILEINPUTSTREAM.toString(), type)
                                || TypeUtils.isAssignableTo(JAVA_IO_FILEOUTPUTSTREAM.toString(), type);
                    }

                });
    }
}