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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static org.openrewrite.java.migrate.jersey.Jersey2Templates.typeName;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ClientResponseToResponse extends Recipe {

    private static final String CLIENT_RESPONSE = "com.sun.jersey.api.client.ClientResponse";
    private static final String STATUS = CLIENT_RESPONSE + "$Status";

    private static final MethodMatcher GET_ENTITY_INPUT_STREAM = new MethodMatcher(CLIENT_RESPONSE + " getEntityInputStream()");
    private static final MethodMatcher GET_CLIENT_RESPONSE_STATUS = new MethodMatcher(CLIENT_RESPONSE + " getClientResponseStatus()");
    private static final MethodMatcher GET_RESPONSE_STATUS = new MethodMatcher(CLIENT_RESPONSE + " getResponseStatus()");
    private static final MethodMatcher GET_LANGUAGE = new MethodMatcher(CLIENT_RESPONSE + " getLanguage()");
    private static final MethodMatcher GET_COOKIES = new MethodMatcher(CLIENT_RESPONSE + " getCookies()");
    private static final MethodMatcher GET_FAMILY_BY_STATUS_CODE = new MethodMatcher(STATUS + " getFamilyByStatusCode(int)");

    String displayName = "Replace Jersey 1 `ClientResponse` methods that Jersey 2 `Response` lacks";

    String description = "Rewrites the calls on a Jersey 1 `ClientResponse` that a rename to JAX-RS 2 `Response` does not " +
            "cover. `getEntityInputStream()` becomes `readEntity(InputStream.class)`. `getClientResponseStatus()` and the " +
            "deprecated `getResponseStatus()` become `Response.Status.fromStatusCode(getStatus())`, which keeps an enum so " +
            "that comparisons and `switch` statements keep compiling, and is `null` for a code without a constant, as it was " +
            "in Jersey 1. `getLanguage()`, a `String` in Jersey 1 and a `Locale` in Jersey 2, becomes the `Content-Language` " +
            "header, `getCookies()`, a `List` in Jersey 1 and a `Map` by name in Jersey 2, becomes a list of the map's values, " +
            "`ClientResponse.Status.getFamilyByStatusCode(int)` becomes `Response.Status.Family.familyOf(int)`, and the " +
            "misspelt `REQUESTED_RANGE_NOT_SATIFIABLE` becomes `REQUESTED_RANGE_NOT_SATISFIABLE`. " +
            "`NON_AUTHORITIVE_INFORMATION` has no JAX-RS constant and is left alone, as are calls without an explicit receiver.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(CLIENT_RESPONSE, true), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = (J.Identifier) super.visitIdentifier(identifier, ctx);
                JavaType.Variable field = id.getFieldType();
                if (field != null && "REQUESTED_RANGE_NOT_SATIFIABLE".equals(field.getName()) &&
                    TypeUtils.isOfClassType(field.getOwner(), STATUS)) {
                    return id.withSimpleName("REQUESTED_RANGE_NOT_SATISFIABLE")
                            .withFieldType(field.withName("REQUESTED_RANGE_NOT_SATISFIABLE"));
                }
                return id;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                if (GET_FAMILY_BY_STATUS_CODE.matches(m)) {
                    String response = typeName(this, cu, "javax.ws.rs.core.Response");
                    return Jersey2Templates.template(response + ".Status.Family.familyOf(#{any(int)})", ctx, "javax.ws.rs.core.Response")
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (m.getSelect() == null) {
                    return m;
                }
                String code;
                if (GET_ENTITY_INPUT_STREAM.matches(m)) {
                    code = "#{any(javax.ws.rs.core.Response)}.readEntity(" + typeName(this, cu, "java.io.InputStream") + ".class)";
                } else if (GET_CLIENT_RESPONSE_STATUS.matches(m) || GET_RESPONSE_STATUS.matches(m)) {
                    code = typeName(this, cu, "javax.ws.rs.core.Response") +
                           ".Status.fromStatusCode(#{any(javax.ws.rs.core.Response)}.getStatus())";
                } else if (GET_LANGUAGE.matches(m)) {
                    code = "#{any(javax.ws.rs.core.Response)}.getHeaderString(" +
                           typeName(this, cu, "javax.ws.rs.core.HttpHeaders") + ".CONTENT_LANGUAGE)";
                } else if (GET_COOKIES.matches(m)) {
                    code = "new " + typeName(this, cu, "java.util.ArrayList") + "<" +
                           typeName(this, cu, "javax.ws.rs.core.NewCookie") +
                           ">(#{any(javax.ws.rs.core.Response)}.getCookies().values())";
                } else {
                    return m;
                }
                return Jersey2Templates.template(code, ctx, "java.io.InputStream", "java.util.ArrayList",
                                "javax.ws.rs.core.HttpHeaders", "javax.ws.rs.core.NewCookie", "javax.ws.rs.core.Response")
                        .apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
            }
        });
    }
}
