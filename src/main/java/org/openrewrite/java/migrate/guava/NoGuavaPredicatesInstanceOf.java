/*
 * Copyright 2025 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

public class NoGuavaPredicatesInstanceOf extends Recipe {
    private static final MethodMatcher PREDICATES_INSTANCE_OF = new MethodMatcher("com.google.common.base.Predicates instanceOf(..)");

    @Override
    public String getDisplayName() {
        return "Prefer `A.class::isInstance`";
    }

    @Override
    public String getDescription() {
        return "Prefer `A.class::isInstance` over `Predicates.instanceOf(A.class)`.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(PREDICATES_INSTANCE_OF),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (PREDICATES_INSTANCE_OF.matches(method)) {
                            maybeRemoveImport("com.google.common.base.Predicates");
                            Expression clazz = method.getArguments().get(0);
                            // XXX `JavaTemplate.builder("#{any()}::isInstance")` failed here
                            // TODO Add type information for reference and identifier
                            return new J.MemberReference(
                                    Tree.randomId(),
                                    method.getPrefix(),
                                    Markers.EMPTY,
                                    JRightPadded.build(clazz),
                                    null,
                                    JLeftPadded.build(new J.Identifier(
                                            Tree.randomId(),
                                            Space.EMPTY,
                                            Markers.EMPTY,
                                            emptyList(),
                                            "isInstance",
                                            null,
                                            null)),
                                    null,
                                    null,
                                    null);
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
