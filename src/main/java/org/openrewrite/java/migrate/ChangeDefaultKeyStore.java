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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;

public class ChangeDefaultKeyStore extends Recipe {
    private static final MethodMatcher KEYSTORE_METHOD_REF = new MethodMatcher("java.security.KeyStore getDefaultType()", true);

    @Getter
    final String displayName = "Return String `jks` when  `KeyStore.getDefaultType()` is called";

    @Getter
    final String description = "In Java 11 the default keystore was updated from JKS to PKCS12. " +
            "As a result, applications relying on KeyStore.getDefaultType() may encounter issues after migrating, " +
            "unless their JKS keystore has been converted to PKCS12. " +
            "This recipe returns default key store of `jks` when `KeyStore.getDefaultType()` method is called to " +
            "use the pre Java 11 default keystore.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(11, 11),
                        new UsesMethod<>(KEYSTORE_METHOD_REF)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (KEYSTORE_METHOD_REF.matches(method)) {
                            return new J.Literal(randomId(), Space.EMPTY, Markers.EMPTY, "\"jks\"", "\"jks\"", null, JavaType.Primitive.String);
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                });
    }
}
