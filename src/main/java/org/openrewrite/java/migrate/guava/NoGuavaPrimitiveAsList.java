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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.template.internal.AbstractRefasterJavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.Collections;

public class NoGuavaPrimitiveAsList extends Recipe {

    static final MethodMatcher METHOD_MATCHER = new MethodMatcher("com.google.common.primitives.* asList(..)");

    @Override
    public String getDisplayName() {
        return "`com.google.common.primitives asList` to `Arrays.asList`";
    }

    @Override
    public String getDescription() {
        return "Migrate from Guava `com.google.common.primitives asList` to `Arrays.asList`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaVisitor<ExecutionContext> javaVisitor = new AbstractRefasterJavaVisitor() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation elem, ExecutionContext ctx) {
                if (METHOD_MATCHER.matches(elem)) {
                    maybeRemoveImport("com.google.common.primitives.Booleans");
                    maybeRemoveImport("com.google.common.primitives.Chars");
                    maybeRemoveImport("com.google.common.primitives.Doubles");
                    maybeRemoveImport("com.google.common.primitives.Floats");
                    maybeRemoveImport("com.google.common.primitives.Longs");
                    maybeRemoveImport("com.google.common.primitives.Ints");
                    maybeRemoveImport("com.google.common.primitives.Shorts");
                    maybeRemoveImport("com.google.common.primitives.Bytes");
                    maybeAddImport("java.util.Arrays");

                    String sb = "Arrays.asList(" +
                            String.join(",", Collections.nCopies(elem.getArguments().size(), "#{any()}")) +
                            ")";

                    return JavaTemplate
                            .builder(sb)
                            .imports("java.util.Arrays")
                            .build()
                            .apply(getCursor(), elem.getCoordinates().replace(), elem.getArguments().toArray());
                }
                return super.visitMethodInvocation(elem, ctx);
            }

        };
        return Preconditions.check(
                new UsesMethod<>("com.google.common.primitives.* asList(..)"),
                javaVisitor
        );
    }
}
