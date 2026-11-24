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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ApacheClientToApacheConnector extends Recipe {

    private static final String APACHE4_CLIENT = "com.sun.jersey.client.apache4.ApacheHttpClient4";
    private static final String APACHE_CLIENT = "com.sun.jersey.client.apache.ApacheHttpClient";
    private static final String CLIENT_CONFIG = "com.sun.jersey.api.client.config.ClientConfig";

    private static final MethodMatcher APACHE4_CREATE = new MethodMatcher(APACHE4_CLIENT + " create()");
    private static final MethodMatcher APACHE4_NEW = new MethodMatcher(APACHE4_CLIENT + " <constructor>()");
    private static final MethodMatcher APACHE4_CREATE_WITH_CONFIG =
            new MethodMatcher(APACHE4_CLIENT + " create(" + CLIENT_CONFIG + ")");
    private static final MethodMatcher APACHE_CREATE = new MethodMatcher(APACHE_CLIENT + " create()");
    private static final MethodMatcher APACHE_NEW = new MethodMatcher(APACHE_CLIENT + " <constructor>()");
    private static final MethodMatcher APACHE_CREATE_WITH_CONFIG =
            new MethodMatcher(APACHE_CLIENT + " create(" + CLIENT_CONFIG + ")");

    String displayName = "Create Jersey 1 Apache HTTP clients as Jersey 2 clients with the Apache connector";

    String description = "Replaces `ApacheHttpClient4.create()`, `ApacheHttpClient.create()` and their constructors " +
            "without arguments with `ClientBuilder.newClient(..)` given a `ClientConfig` whose connector provider is " +
            "the `ApacheConnectorProvider` of `jersey-apache-connector`, and `create(ClientConfig)` with " +
            "`ClientBuilder.newClient(..)` given that configuration with the Apache connector provider set. The " +
            "clients built from an `ApacheHttpClient4Handler` or an `ApacheHttpClientHandler` are left alone, since " +
            "Jersey 2 builds the Apache HTTP client itself.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new UsesType<>(APACHE4_CLIENT, false), new UsesType<>(APACHE_CLIENT, false)),
                new JavaVisitor<ExecutionContext>() {

                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J j = super.visitNewClass(newClass, ctx);
                        if (j instanceof J.NewClass && ((J.NewClass) j).getBody() == null &&
                            (APACHE4_NEW.matches((J.NewClass) j) || APACHE_NEW.matches((J.NewClass) j))) {
                            return newClient((J.NewClass) j, ctx);
                        }
                        return j;
                    }

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J j = super.visitMethodInvocation(method, ctx);
                        if (!(j instanceof J.MethodInvocation)) {
                            return j;
                        }
                        J.MethodInvocation m = (J.MethodInvocation) j;
                        if (APACHE4_CREATE.matches(m) || APACHE_CREATE.matches(m)) {
                            return newClient(m, ctx);
                        }
                        if (APACHE4_CREATE_WITH_CONFIG.matches(m) || APACHE_CREATE_WITH_CONFIG.matches(m)) {
                            removeImports();
                            return template("ClientBuilder.newClient(#{any(org.glassfish.jersey.client.ClientConfig)}" +
                                            ".connectorProvider(new ApacheConnectorProvider()))", ctx)
                                    .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
                        }
                        return m;
                    }

                    private J newClient(J.NewClass n, ExecutionContext ctx) {
                        removeImports();
                        maybeAddImport("org.glassfish.jersey.client.ClientConfig");
                        return template("ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))", ctx)
                                .apply(getCursor(), n.getCoordinates().replace());
                    }

                    private J newClient(J.MethodInvocation m, ExecutionContext ctx) {
                        removeImports();
                        maybeAddImport("org.glassfish.jersey.client.ClientConfig");
                        return template("ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))", ctx)
                                .apply(getCursor(), m.getCoordinates().replace());
                    }

                    private void removeImports() {
                        maybeRemoveImport(APACHE4_CLIENT);
                        maybeRemoveImport(APACHE_CLIENT);
                        maybeAddImport("javax.ws.rs.client.ClientBuilder");
                        maybeAddImport("org.glassfish.jersey.apache.connector.ApacheConnectorProvider");
                    }
                });
    }

    private static JavaTemplate template(String code, ExecutionContext ctx) {
        return JavaTemplate.builder(code)
                .imports("javax.ws.rs.client.ClientBuilder",
                        "org.glassfish.jersey.client.ClientConfig",
                        "org.glassfish.jersey.apache.connector.ApacheConnectorProvider")
                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx,
                        "jersey-client-2", "jersey-common-2", "javax.ws.rs-api-2", "jersey-apache-connector-2"))
                .build();
    }
}
