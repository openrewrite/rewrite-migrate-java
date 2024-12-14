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
package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveFinalizerFromZip extends Recipe {

    private static final String JAVA_UTIL_ZIP_DEFLATER = "java.util.zip.Deflater";
    private static final String JAVA_UTIL_ZIP_INFLATER = "java.util.zip.Inflater";
    private static final String JAVA_UTIL_ZIP_ZIP_FILE = "java.util.zip.ZipFile";

    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.lang.Object finalize()");

    @Override
    public String getDisplayName() {
        return "Remove invocations of deprecated invocations from Deflater, Inflater, ZipFile ";
    }

    @Override
    public String getDescription() {
        return "Remove invocations of finalize() deprecated invocations from Deflater, Inflater, ZipFile.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(
                        new UsesJavaVersion<>(12),
                        Preconditions.or(
                                new UsesType<>(JAVA_UTIL_ZIP_DEFLATER, false),
                                new UsesType<>(JAVA_UTIL_ZIP_INFLATER, false),
                                new UsesType<>(JAVA_UTIL_ZIP_ZIP_FILE, false))),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                        if (METHOD_MATCHER.matches(mi)) {
                            Expression select = mi.getSelect();
                            if (select == null) {
                                J.ClassDeclaration cd = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
                                if (shouldRemoveFinalize(cd.getType())) {
                                    return null;
                                }
                            } else {
                                if (shouldRemoveFinalize(select.getType())) {
                                    // Retain any side effects preceding the finalize() call
                                    List<J> sideEffects = select.getSideEffects();
                                    if (sideEffects.isEmpty()) {
                                        return null;
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
                        return TypeUtils.isAssignableTo(JAVA_UTIL_ZIP_DEFLATER, type) ||
                               TypeUtils.isAssignableTo(JAVA_UTIL_ZIP_INFLATER, type) ||
                               TypeUtils.isAssignableTo(JAVA_UTIL_ZIP_ZIP_FILE, type);
                    }
                });
    }

}
