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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class StreamFindFirst extends Recipe {
    private static final MethodMatcher COLLECTION_STREAM_MATCHER = new MethodMatcher("java.util.Collection stream()", true);
    private static final MethodMatcher STREAM_FIND_FIRST_MATCHER = new MethodMatcher("java.util.stream.Stream findFirst()", true);
    private static final MethodMatcher OPTIONAL_OR_ELSE_THROW_MATCHER = new MethodMatcher("java.util.Optional orElseThrow()", true);

    @Getter
    final String displayName = "Use `getFirst()` instead of `stream().findFirst().orElseThrow()`";

    @Getter
    final String description = "For SequencedCollections, use `collection.getFirst()` instead of `collection.stream().findFirst().orElseThrow()`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> javaIsoVisitor = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                if (!OPTIONAL_OR_ELSE_THROW_MATCHER.matches(mi) || !(mi.getSelect() instanceof J.MethodInvocation)) {
                    return mi;
                }
                J.MethodInvocation optional = (J.MethodInvocation) mi.getSelect();
                if (!STREAM_FIND_FIRST_MATCHER.matches(optional) || !(optional.getSelect() instanceof J.MethodInvocation)) {
                    return mi;
                }
                J.MethodInvocation stream = (J.MethodInvocation) optional.getSelect();
                if (!COLLECTION_STREAM_MATCHER.matches(stream) ||
                    !TypeUtils.isOfClassType(stream.getSelect().getType(), "java.util.SequencedCollection")) {
                    return mi;
                }
                JavaType.Method methodType = stream.getMethodType().withName("getFirst");
                return stream
                        .withName(stream.getName().withSimpleName("getFirst").withType(methodType))
                        .withMethodType(methodType)
                        .withPrefix(mi.getPrefix());
            }


        };
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(21),
                        new UsesMethod<>(COLLECTION_STREAM_MATCHER),
                        new UsesMethod<>(STREAM_FIND_FIRST_MATCHER),
                        new UsesMethod<>(OPTIONAL_OR_ELSE_THROW_MATCHER)
                ),
                javaIsoVisitor);
    }
}
