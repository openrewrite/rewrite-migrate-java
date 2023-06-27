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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveFinalizeFromFileStream extends Recipe {

    private static final String JAVA_IO_FILEINPUTSTREAM = "java.io.FileInputStream";
    private static final String JAVA_IO_FILEOUTPUTSTREAM = "java.io.FileOutputStream";

    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.lang.Object finalize()");

    @Override
    public String getDisplayName() {
        return "Replace invocations of deprecated invocations from FileInputStream, FileOutputStream";
    }

    @Override
    public String getDescription() {
        return "Remove invocations of finalize() deprecated invocations from FileInputStream, FileOutputStream.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>(JAVA_IO_FILEINPUTSTREAM, false),
                        new UsesType<>(JAVA_IO_FILEOUTPUTSTREAM, false)),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);

                        if (METHOD_MATCHER.matches(mi)) {
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
                        return TypeUtils.isAssignableTo(JAVA_IO_FILEINPUTSTREAM, type)
                                || TypeUtils.isAssignableTo(JAVA_IO_FILEOUTPUTSTREAM, type);
                    }

                });
    }
}