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
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;

public class ChangeDefaultKeyStore extends Recipe {
    private static final MethodMatcher KEYSTORE_METHOD_REF = new MethodMatcher("java.security.KeyStore getDefaultType()");

    @Getter
    final String displayName = "Replace `KeyStore.getDefaultType()` with `\"jks\"`";

    @Getter
    final String description = "In Java 9 the default keystore type changed from JKS to PKCS12. " +
            "As a result, applications relying on `KeyStore.getDefaultType()` may encounter issues after migrating, " +
            "unless their JKS keystore has been converted to PKCS12. " +
            "This recipe replaces `KeyStore.getDefaultType()` with `\"jks\"` in projects still on Java 8 or earlier, " +
            "so that they keep the pre Java 9 default keystore type once upgraded.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(1, 8),
                        new UsesMethod<>(KEYSTORE_METHOD_REF)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (KEYSTORE_METHOD_REF.matches(method)) {
                            maybeRemoveImport("java.security.KeyStore");
                            return new J.Literal(randomId(), method.getPrefix(), Markers.EMPTY, "jks", "\"jks\"", null, JavaType.Primitive.String);
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                });
    }
}
