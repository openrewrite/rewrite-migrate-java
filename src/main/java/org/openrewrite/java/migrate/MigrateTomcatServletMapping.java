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
package org.openrewrite.java.migrate;

import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

public class MigrateTomcatServletMapping extends Recipe {
    private static final MethodMatcher LEGACY_MAPPING = new MethodMatcher(
            "org.apache.catalina.Context addServletMapping(..)", true);
    private static final String UDECODER = "org.apache.tomcat.util.buf.UDecoder";

    @Getter
    final String displayName = "Migrate Tomcat servlet mappings to the decoded pattern API";

    @Getter
    final String description = "Use Tomcat 9's addServletMappingDecoded API. Decode patterns containing escape sequences as the old API did.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(LEGACY_MAPPING), new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate decode = JavaTemplate.builder("UDecoder.URLDecode(#{any(java.lang.String)}, StandardCharsets.ISO_8859_1)")
                    .imports(UDECODER, "java.nio.charset.StandardCharsets")
                    .javaParser(JavaParser.fromJavaVersion().dependsOn(
                            "package org.apache.tomcat.util.buf; public class UDecoder { " +
                            "public static String URLDecode(String pattern, java.nio.charset.Charset charset) { return pattern; } }"))
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method methodType = m.getMethodType();
                if (methodType == null || !LEGACY_MAPPING.matches(m)) {
                    return m;
                }

                Expression pattern = m.getArguments().get(0);
                if (!(pattern instanceof J.Literal) || !(((J.Literal) pattern).getValue() instanceof String) ||
                        !((String) ((J.Literal) pattern).getValue()).chars().allMatch(c -> c < 128 && c != '%')) {
                    maybeAddImport(UDECODER);
                    maybeAddImport("java.nio.charset.StandardCharsets");
                    List<Expression> arguments = new ArrayList<>(m.getArguments());
                    J.MethodInvocation decoded = decode.apply(new Cursor(updateCursor(m), pattern), pattern.getCoordinates().replace(), pattern);
                    arguments.set(0, decoded);
                    m = m.withArguments(arguments);
                }

                JavaType.Method migratedType = methodType.withName("addServletMappingDecoded");
                return m.withMethodType(migratedType)
                        .withName(m.getName().withSimpleName(migratedType.getName()).withType(migratedType));
            }
        });
    }
}
