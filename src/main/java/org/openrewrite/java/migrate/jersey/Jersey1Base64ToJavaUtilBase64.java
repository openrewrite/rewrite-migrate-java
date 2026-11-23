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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1Base64ToJavaUtilBase64 extends Recipe {

    private static final String BASE64 = "com.sun.jersey.core.util.Base64";

    private static final MethodMatcher ENCODE_BYTES = new MethodMatcher(BASE64 + " encode(byte[])");
    private static final MethodMatcher ENCODE_STRING = new MethodMatcher(BASE64 + " encode(java.lang.String)");
    private static final MethodMatcher DECODE_BYTES = new MethodMatcher(BASE64 + " decode(byte[])");
    private static final MethodMatcher DECODE_STRING = new MethodMatcher(BASE64 + " decode(java.lang.String)");
    private static final MethodMatcher BASE64_DECODE = new MethodMatcher(BASE64 + " base64Decode(java.lang.String)");

    String displayName = "Use `java.util.Base64` instead of the Jersey 1 `Base64`";

    String description = "Replaces the static methods of `com.sun.jersey.core.util.Base64` with `java.util.Base64`. " +
            "`encode(byte[])` and `encode(String)` become `Base64.getEncoder().encode(..)`, still returning bytes and still " +
            "turning a `String` into bytes with the platform charset. `decode(byte[])` and `decode(String)` become " +
            "`Base64.getDecoder().decode(..)`: Jersey 1 decoded those in groups of four without skipping anything, so input " +
            "it decoded correctly decodes the same, while input with line breaks or other characters outside the alphabet, " +
            "which Jersey 1 turned into wrong bytes, now throws an `IllegalArgumentException`. `base64Decode(String)` skipped " +
            "every character outside the alphabet and returned each byte as a character, so it becomes a `String` in " +
            "ISO-8859-1 of `Base64.getMimeDecoder().decode(..)`, which skips them too. `isBase64` and `isArrayByteBase64`, " +
            "which `java.util.Base64` has no counterpart for, are left alone, and `java.util.Base64` is then written fully " +
            "qualified.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(BASE64, false), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                String code;
                if (ENCODE_BYTES.matches(m)) {
                    code = "java.util.Base64.getEncoder().encode(#{anyArray(byte)})";
                } else if (ENCODE_STRING.matches(m)) {
                    code = "java.util.Base64.getEncoder().encode(#{any(java.lang.String)}.getBytes())";
                } else if (DECODE_BYTES.matches(m)) {
                    code = "java.util.Base64.getDecoder().decode(#{anyArray(byte)})";
                } else if (DECODE_STRING.matches(m)) {
                    code = "java.util.Base64.getDecoder().decode(#{any(java.lang.String)})";
                } else if (BASE64_DECODE.matches(m)) {
                    code = "new String(java.util.Base64.getMimeDecoder().decode(#{any(java.lang.String)}), " +
                           "java.nio.charset.StandardCharsets.ISO_8859_1)";
                } else {
                    return m;
                }
                maybeRemoveImport(BASE64);
                J replacement = JavaTemplate.builder(code)
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                // once the Jersey 1 import is gone, unless it is still needed
                doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(replacement));
                return replacement;
            }
        });
    }
}
