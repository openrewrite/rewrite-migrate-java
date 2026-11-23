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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ServerFactoriesToJersey2 extends Recipe {

    private static final String JERSEY1_RESOURCE_CONFIG = "com.sun.jersey.api.core.ResourceConfig";
    private static final String HTTP_SERVER_FACTORY = "com.sun.jersey.api.container.httpserver.HttpServerFactory";
    private static final String GRIZZLY_SERVER_FACTORY = "com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory";
    private static final String JDK_HTTP_SERVER_FACTORY = "org.glassfish.jersey.jdkhttp.JdkHttpServerFactory";
    private static final String GRIZZLY_HTTP_SERVER_FACTORY = "org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory";

    private static final MethodMatcher HTTP_SERVER_CREATE = new MethodMatcher(
            HTTP_SERVER_FACTORY + " create(*, " + JERSEY1_RESOURCE_CONFIG + ")");
    private static final MethodMatcher GRIZZLY_SERVER_CREATE = new MethodMatcher(
            GRIZZLY_SERVER_FACTORY + " createHttpServer(*, " + JERSEY1_RESOURCE_CONFIG + ")");

    /**
     * Jersey 2's Grizzly container refers to Grizzly types that the type table does not hold.
     */
    static final String[] GRIZZLY_STUBS = {
            "package org.glassfish.grizzly.http.server; public class HttpServer { public void start() throws java.io.IOException {} public void shutdownNow() {} }",
            "package org.glassfish.grizzly.http.server; public abstract class HttpHandler {}",
            "package org.glassfish.grizzly.ssl; public class SSLEngineConfigurator {}"
    };

    String displayName = "Replace Jersey 1 server factories with their Jersey 2 counterparts";

    String description = "Replaces `HttpServerFactory.create(uri, resourceConfig)` with " +
            "`JdkHttpServerFactory.createHttpServer(uri, resourceConfig, false)`, and " +
            "`GrizzlyServerFactory.createHttpServer(uri, resourceConfig)` with " +
            "`GrizzlyHttpServerFactory.createHttpServer(uri, resourceConfig)`, wrapping a `String` URI in " +
            "`URI.create(..)`. The JDK HTTP server stays unstarted and the Grizzly server is started, as in Jersey 1. " +
            "Overloads without a `ResourceConfig`, for which Jersey 1 scanned the class path, and those that take an " +
            "`IoCComponentProviderFactory` or a handler are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new UsesMethod<>(HTTP_SERVER_CREATE), new UsesMethod<>(GRIZZLY_SERVER_CREATE)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        boolean jdk = HTTP_SERVER_CREATE.matches(m);
                        if (!jdk && !GRIZZLY_SERVER_CREATE.matches(m)) {
                            return m;
                        }
                        Expression uri = m.getArguments().get(0);
                        boolean string = TypeUtils.isString(uri.getType());
                        if (!string && !TypeUtils.isOfClassType(uri.getType(), "java.net.URI")) {
                            return m;
                        }
                        String uriTemplate = string ? "URI.create(#{any(java.lang.String)})" : "#{any(java.net.URI)}";
                        String configTemplate = "#{any(org.glassfish.jersey.server.ResourceConfig)}";
                        JavaParser.Builder<?, ?> parser = JavaParser.fromJavaVersion().classpathFromResources(ctx,
                                "javax.ws.rs-api-2", "jersey-common-2", "jersey-server-2",
                                "jersey-container-jdk-http-2", "jersey-container-grizzly2-http-2");
                        JavaTemplate template;
                        if (jdk) {
                            template = JavaTemplate.builder("JdkHttpServerFactory.createHttpServer(" + uriTemplate + ", " + configTemplate + ", false)")
                                    .imports(JDK_HTTP_SERVER_FACTORY, "java.net.URI")
                                    .javaParser(parser)
                                    .build();
                            maybeAddImport(JDK_HTTP_SERVER_FACTORY);
                            maybeRemoveImport(HTTP_SERVER_FACTORY);
                        } else {
                            template = JavaTemplate.builder("GrizzlyHttpServerFactory.createHttpServer(" + uriTemplate + ", " + configTemplate + ")")
                                    .imports(GRIZZLY_HTTP_SERVER_FACTORY, "java.net.URI")
                                    .javaParser(parser.dependsOn(GRIZZLY_STUBS))
                                    .build();
                            maybeAddImport(GRIZZLY_HTTP_SERVER_FACTORY);
                            maybeRemoveImport(GRIZZLY_SERVER_FACTORY);
                        }
                        if (string) {
                            maybeAddImport("java.net.URI");
                        }
                        return template.apply(getCursor(), m.getCoordinates().replace(), uri, m.getArguments().get(1));
                    }
                });
    }
}
