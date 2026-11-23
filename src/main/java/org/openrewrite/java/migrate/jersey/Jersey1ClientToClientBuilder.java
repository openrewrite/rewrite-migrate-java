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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ClientToClientBuilder extends Recipe {

    private static final String CLIENT = "com.sun.jersey.api.client.Client";

    private static final MethodMatcher CREATE = new MethodMatcher(CLIENT + " create()");
    private static final MethodMatcher CREATE_WITH_CONFIG =
            new MethodMatcher(CLIENT + " create(com.sun.jersey.api.client.config.ClientConfig)");
    private static final MethodMatcher NEW_CLIENT = new MethodMatcher(CLIENT + " <constructor>()");

    private static final MethodMatcher SET_CONNECT_TIMEOUT = new MethodMatcher(CLIENT + " setConnectTimeout(java.lang.Integer)");
    private static final MethodMatcher SET_READ_TIMEOUT = new MethodMatcher(CLIENT + " setReadTimeout(java.lang.Integer)");
    private static final MethodMatcher SET_FOLLOW_REDIRECTS = new MethodMatcher(CLIENT + " setFollowRedirects(java.lang.Boolean)");
    private static final MethodMatcher SET_CHUNKED_ENCODING_SIZE =
            new MethodMatcher(CLIENT + " setChunkedEncodingSize(java.lang.Integer)");

    String displayName = "Create Jersey 2 clients with `ClientBuilder`";

    String description = "Replaces `Client.create()`, `new Client()` and `Client.create(ClientConfig)` of Jersey 1 with " +
            "`ClientBuilder.newClient()` and `ClientBuilder.newClient(Configuration)`, leaving the configuration argument " +
            "as it is. Replaces the Jersey 1 `Client` setters `setConnectTimeout`, `setReadTimeout`, `setFollowRedirects` " +
            "and `setChunkedEncodingSize` with `property(..)` and the matching `ClientProperties` constant; since the default " +
            "Jersey 2 connector only streams chunked when told to, a chunk size also sets `REQUEST_ENTITY_PROCESSING` to " +
            "`CHUNKED`, as setting one did in Jersey 1. The constructors that take a `ClientHandler`, " +
            "`Client.create(ClientConfig, IoCComponentProviderFactory)` and the setters called on a subclass of `Client`, " +
            "such as the Apache HTTP client, are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(CLIENT, false), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J j = super.visitNewClass(newClass, ctx);
                if (!(j instanceof J.NewClass)) {
                    return j;
                }
                J.NewClass n = (J.NewClass) j;
                if (!NEW_CLIENT.matches(n) || n.getBody() != null) {
                    return n;
                }
                maybeRemoveImport(CLIENT);
                maybeAddImport("javax.ws.rs.client.ClientBuilder");
                return template("ClientBuilder.newClient()", ctx)
                        .apply(getCursor(), n.getCoordinates().replace());
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (CREATE.matches(m)) {
                    maybeRemoveImport(CLIENT);
                    maybeAddImport("javax.ws.rs.client.ClientBuilder");
                    return template("ClientBuilder.newClient()", ctx)
                            .apply(getCursor(), m.getCoordinates().replace());
                }
                if (CREATE_WITH_CONFIG.matches(m)) {
                    maybeRemoveImport(CLIENT);
                    maybeAddImport("javax.ws.rs.client.ClientBuilder");
                    return template("ClientBuilder.newClient(#{any(javax.ws.rs.core.Configuration)})", ctx)
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (m.getSelect() == null || !TypeUtils.isOfClassType(m.getSelect().getType(), CLIENT)) {
                    return m;
                }
                String property;
                if (SET_CONNECT_TIMEOUT.matches(m)) {
                    property = "CONNECT_TIMEOUT";
                } else if (SET_READ_TIMEOUT.matches(m)) {
                    property = "READ_TIMEOUT";
                } else if (SET_FOLLOW_REDIRECTS.matches(m)) {
                    property = "FOLLOW_REDIRECTS";
                } else if (SET_CHUNKED_ENCODING_SIZE.matches(m)) {
                    property = "CHUNKED_ENCODING_SIZE";
                } else {
                    return m;
                }
                Expression value = m.getArguments().get(0);
                maybeAddImport("org.glassfish.jersey.client.ClientProperties");
                String code = "#{any(javax.ws.rs.client.Client)}.property(ClientProperties." + property + ", #{any()})";
                if ("CHUNKED_ENCODING_SIZE".equals(property) && !isNullLiteral(value)) {
                    // Jersey 1 streamed chunked as soon as a chunk size was set, the HttpUrlConnector of Jersey 2
                    // only when the request entity processing says so.
                    maybeAddImport("org.glassfish.jersey.client.RequestEntityProcessing");
                    code += ".property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED)";
                }
                return template(code, ctx).apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), value);
            }
        });
    }

    private static boolean isNullLiteral(Expression e) {
        return e instanceof J.Literal && ((J.Literal) e).getType() == JavaType.Primitive.Null;
    }

    private static JavaTemplate template(String code, ExecutionContext ctx) {
        return Jersey2Templates.template(code, ctx,
                "javax.ws.rs.client.ClientBuilder",
                "org.glassfish.jersey.client.ClientProperties",
                "org.glassfish.jersey.client.RequestEntityProcessing");
    }
}
