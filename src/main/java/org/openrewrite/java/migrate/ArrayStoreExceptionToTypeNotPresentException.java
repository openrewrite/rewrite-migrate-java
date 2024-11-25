/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class ArrayStoreExceptionToTypeNotPresentException extends Recipe {

    private static final String ARRAY_STORE_EXCEPTION = "java.lang.ArrayStoreException";
    private static final String TYPE_NOT_PRESENT_EXCEPTION = "java.lang.TypeNotPresentException";

    @Override
    public String getDisplayName() {
        return "Catch `TypeNotPresentException` thrown by `Class.getAnnotation()`";
    }

    @Override
    public String getDescription() {
        return "Replace catch blocks for `ArrayStoreException` around `Class.getAnnotation()` with `TypeNotPresentException` to ensure compatibility with Java 11+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String classGetAnnotationPattern = "java.lang.Class getAnnotation(java.lang.Class)";
        return Preconditions.check(new UsesMethod<>(classGetAnnotationPattern), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Try visitTry(J.Try tryStatement, ExecutionContext ctx) {
                J.Try try_ = super.visitTry(tryStatement, ctx);
                if (FindMethods.find(try_, classGetAnnotationPattern).isEmpty()) {
                    return try_;
                }
                return try_.withCatches(ListUtils.map(try_.getCatches(), catch_ -> {
                    if (TypeUtils.isOfClassType(catch_.getParameter().getType(), ARRAY_STORE_EXCEPTION)) {
                        return (J.Try.Catch) new ChangeType(ARRAY_STORE_EXCEPTION, TYPE_NOT_PRESENT_EXCEPTION, true)
                                .getVisitor().visit(catch_, ctx);
                    }
                    return catch_;
                }));
            }
        });
    }
}
