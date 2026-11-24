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
import org.jspecify.annotations.Nullable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ClientToClientBuilder extends Recipe {

    private static final String CLIENT = "com.sun.jersey.api.client.Client";
    private static final String CLIENT_CONFIG = "com.sun.jersey.api.client.config.ClientConfig";
    private static final String DEFAULT_CLIENT_CONFIG = "com.sun.jersey.api.client.config.DefaultClientConfig";

    private static final MethodMatcher CREATE = new MethodMatcher(CLIENT + " create()");
    private static final MethodMatcher CREATE_WITH_CONFIG = new MethodMatcher(CLIENT + " create(" + CLIENT_CONFIG + ")");
    private static final MethodMatcher NEW_CLIENT = new MethodMatcher(CLIENT + " <constructor>()");

    private static final MethodMatcher SET_CONNECT_TIMEOUT = new MethodMatcher(CLIENT + " setConnectTimeout(java.lang.Integer)");
    private static final MethodMatcher SET_READ_TIMEOUT = new MethodMatcher(CLIENT + " setReadTimeout(java.lang.Integer)");
    private static final MethodMatcher SET_FOLLOW_REDIRECTS = new MethodMatcher(CLIENT + " setFollowRedirects(java.lang.Boolean)");
    private static final MethodMatcher SET_CHUNKED_ENCODING_SIZE =
            new MethodMatcher(CLIENT + " setChunkedEncodingSize(java.lang.Integer)");

    private static final MethodMatcher MAP_PUT = new MethodMatcher("java.util.Map put(..)", true);
    private static final MethodMatcher COLLECTION_ADD = new MethodMatcher("java.util.Collection add(..)", true);
    private static final MethodMatcher CLIENT_GET_PROPERTIES = new MethodMatcher(CLIENT + " getProperties()");
    private static final MethodMatcher CONFIG_GET_PROPERTIES =
            new MethodMatcher("com.sun.jersey.core.util.FeaturesAndProperties getProperties()", true);
    private static final MethodMatcher CONFIG_GET_CLASSES = new MethodMatcher(CLIENT_CONFIG + " getClasses()", true);
    private static final MethodMatcher CONFIG_GET_SINGLETONS = new MethodMatcher(CLIENT_CONFIG + " getSingletons()", true);

    /**
     * The Jersey 1 client properties by the {@code ClientConfig} constant and its value, and the {@code ClientProperties}
     * constant of Jersey 2 that means the same.
     */
    private static final Map<String, String> PROPERTIES = new HashMap<>();

    static {
        PROPERTIES.put("PROPERTY_FOLLOW_REDIRECTS", "FOLLOW_REDIRECTS");
        PROPERTIES.put("PROPERTY_READ_TIMEOUT", "READ_TIMEOUT");
        PROPERTIES.put("PROPERTY_CONNECT_TIMEOUT", "CONNECT_TIMEOUT");
        PROPERTIES.put("PROPERTY_CHUNKED_ENCODING_SIZE", "CHUNKED_ENCODING_SIZE");
        PROPERTIES.put("PROPERTY_THREADPOOL_SIZE", "ASYNC_THREADPOOL_SIZE");
        PROPERTIES.put("com.sun.jersey.client.property.followRedirects", "FOLLOW_REDIRECTS");
        PROPERTIES.put("com.sun.jersey.client.property.readTimeout", "READ_TIMEOUT");
        PROPERTIES.put("com.sun.jersey.client.property.connectTimeout", "CONNECT_TIMEOUT");
        PROPERTIES.put("com.sun.jersey.client.property.chunkedEncodingSize", "CHUNKED_ENCODING_SIZE");
        PROPERTIES.put("com.sun.jersey.client.property.threadpoolSize", "ASYNC_THREADPOOL_SIZE");
    }

    String displayName = "Create and configure Jersey 2 clients with `ClientBuilder` and `property(..)`";

    String description = "Replaces `Client.create()`, `new Client()` and `Client.create(ClientConfig)` of Jersey 1 with " +
            "`ClientBuilder.newClient()` and `ClientBuilder.newClient(Configuration)`, leaving the configuration argument " +
            "as it is. Replaces the Jersey 1 `Client` setters `setConnectTimeout`, `setReadTimeout`, `setFollowRedirects` " +
            "and `setChunkedEncodingSize` with `property(..)` and the matching `ClientProperties` constant; since the default " +
            "Jersey 2 connector only streams chunked when told to, a chunk size also sets `REQUEST_ENTITY_PROCESSING` to " +
            "`CHUNKED`, as setting one did in Jersey 1. Statements that put into the property bag of a `Client`, " +
            "`ClientConfig` or `DefaultClientConfig` with `getProperties().put(..)` become `property(..)`, translating the " +
            "Jersey 1 property names, and `getClasses().add(..)` and `getSingletons().add(..)` of a client configuration " +
            "become `register(..)`, since what Jersey 2 returns for these is unmodifiable. The constructors that take a " +
            "`ClientHandler`, `Client.create(ClientConfig, IoCComponentProviderFactory)`, the property " +
            "`PROPERTY_BUFFER_RESPONSE_ENTITY_ON_EXCEPTION`, which Jersey 2 has no counterpart for, and calls on a subclass " +
            "of `Client` or `ClientConfig`, such as those of the Apache HTTP client, are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.or(
                new UsesType<>(CLIENT, true),
                new UsesType<>(CLIENT_CONFIG, true));
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {

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
                if (m.getSelect() instanceof J.MethodInvocation) {
                    J.MethodInvocation configured = configurationStatement(m, (J.MethodInvocation) m.getSelect(), ctx);
                    if (configured != m) {
                        return configured;
                    }
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
                return setProperty(m, "javax.ws.rs.client.Client", m.getSelect(), property, null, m.getArguments().get(0), ctx);
            }

            /**
             * {@code x.getProperties().put(key, value)}, {@code x.getClasses().add(c)} and
             * {@code x.getSingletons().add(o)} as statements, since what they return differs from what replaces them.
             */
            private J.MethodInvocation configurationStatement(J.MethodInvocation m, J.MethodInvocation getter, ExecutionContext ctx) {
                Expression owner = getter.getSelect();
                if (owner == null || !(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                    return m;
                }
                String target;
                if (TypeUtils.isOfClassType(owner.getType(), CLIENT)) {
                    target = "javax.ws.rs.client.Client";
                } else if (TypeUtils.isOfClassType(owner.getType(), CLIENT_CONFIG) ||
                           TypeUtils.isOfClassType(owner.getType(), DEFAULT_CLIENT_CONFIG)) {
                    target = "org.glassfish.jersey.client.ClientConfig";
                } else {
                    return m;
                }
                if (MAP_PUT.matches(m) && (CLIENT_GET_PROPERTIES.matches(getter) || CONFIG_GET_PROPERTIES.matches(getter))) {
                    Expression key = m.getArguments().get(0);
                    String constant = jersey1Property(key);
                    if ("".equals(constant)) {
                        return m;
                    }
                    return setProperty(m, target, owner, constant, constant == null ? key : null, m.getArguments().get(1), ctx);
                }
                if (COLLECTION_ADD.matches(m) && target.startsWith("org.") &&
                    (CONFIG_GET_CLASSES.matches(getter) || CONFIG_GET_SINGLETONS.matches(getter))) {
                    return template("#{any(" + target + ")}.register(#{any(java.lang.Object)})", ctx)
                            .apply(getCursor(), m.getCoordinates().replace(), owner, m.getArguments().get(0));
                }
                return m;
            }

            private J.MethodInvocation setProperty(J.MethodInvocation m, String target, Expression owner,
                                                   @Nullable String constant, @Nullable Expression key,
                                                   Expression value, ExecutionContext ctx) {
                List<Object> parameters = new ArrayList<>();
                parameters.add(owner);
                String name;
                if (constant != null) {
                    maybeAddImport("org.glassfish.jersey.client.ClientProperties");
                    name = "ClientProperties." + constant;
                } else {
                    name = "#{any(java.lang.String)}";
                    parameters.add(key);
                }
                parameters.add(value);
                String code = "#{any(" + target + ")}.property(" + name + ", #{any()})";
                if ("CHUNKED_ENCODING_SIZE".equals(constant) && !isNullLiteral(value)) {
                    // Jersey 1 streamed chunked as soon as a chunk size was set, the HttpUrlConnector of Jersey 2
                    // only when the request entity processing says so.
                    maybeAddImport("org.glassfish.jersey.client.RequestEntityProcessing");
                    code += ".property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED)";
                }
                return template(code, ctx).apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
            }
        });
    }

    /**
     * @return the {@code ClientProperties} constant for a Jersey 1 client property, an empty string for a Jersey 1
     * client property that Jersey 2 has no counterpart for, or {@code null} for a key that is no Jersey 1 client
     * property, which stays as it is.
     */
    private static @Nullable String jersey1Property(Expression key) {
        String name = null;
        if (key instanceof J.Literal && ((J.Literal) key).getValue() instanceof String) {
            name = (String) ((J.Literal) key).getValue();
        } else {
            JavaType.Variable field = key instanceof J.Identifier ? ((J.Identifier) key).getFieldType() :
                    key instanceof J.FieldAccess ? ((J.FieldAccess) key).getName().getFieldType() : null;
            if (field != null && TypeUtils.isOfClassType(field.getOwner(), CLIENT_CONFIG)) {
                name = field.getName();
            }
        }
        if (name == null || !name.startsWith("PROPERTY_") && !name.startsWith("com.sun.jersey.client.property.")) {
            return null;
        }
        String constant = PROPERTIES.get(name);
        return constant == null ? "" : constant;
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
