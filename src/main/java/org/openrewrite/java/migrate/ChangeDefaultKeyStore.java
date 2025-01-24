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
package org.openrewrite.java.migrate;


import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;


public class ChangeDefaultKeyStore extends Recipe {
    private static final MethodMatcher KEYSTORE_METHOD_REF = new MethodMatcher("java.security.KeyStore getDefaultType()", true);

    @Override
    public String getDisplayName() {
        return "Replace `java.lang.ref.Reference.clone()` with constructor call";
    }

    @Override
    public String getDescription() {
        return "This recipe returns default keystore value of jks when getDefaultKeyStore is called.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (KEYSTORE_METHOD_REF.matches(method)) {
                    return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "\"JKS\"", "\"JKS\"", null, JavaType.Primitive.String);
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
