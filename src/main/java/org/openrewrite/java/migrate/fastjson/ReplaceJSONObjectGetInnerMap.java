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
package org.openrewrite.java.migrate.fastjson;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

public class ReplaceJSONObjectGetInnerMap extends Recipe {

    private static final MethodMatcher GET_INNER_MAP = new MethodMatcher("com.alibaba.fastjson.JSONObject getInnerMap()", true);

    @Getter
    final String displayName = "Replace Fastjson `JSONObject.getInnerMap()` with the `JSONObject`";

    @Getter
    final String description = "Fastjson 1.x `JSONObject` wraps a map that `getInnerMap()` returns. fastjson2's `JSONObject` " +
            "has no `getInnerMap()`, since it extends `LinkedHashMap` and is the map itself, so the call is replaced " +
            "with the `JSONObject` it was made on.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_INNER_MAP), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation) || !GET_INNER_MAP.matches((J.MethodInvocation) j)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (m.getSelect() != null) {
                    return m.getSelect().withPrefix(m.getPrefix());
                }
                JavaType.FullyQualified declaringType = m.getMethodType() == null ? null : m.getMethodType().getDeclaringType();
                return new J.Identifier(Tree.randomId(), m.getPrefix(), Markers.EMPTY, emptyList(), "this", declaringType, null);
            }
        });
    }
}
