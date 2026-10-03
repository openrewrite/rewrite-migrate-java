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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class JettyConnectorGetPort extends Recipe {
    String displayName = "Access Jetty connector ports through `NetworkConnector`";

    String description = "Jetty 9 moved `getPort()` from `Connector` to `NetworkConnector`. " +
            "Cast legacy connector receivers to the network connector interface when upgrading Jetty.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher getPort = new MethodMatcher("org.eclipse.jetty.server.Connector getPort()");
            final JavaTemplate template = JavaTemplate.builder("((NetworkConnector) #{any()}).getPort()")
                    .imports("org.eclipse.jetty.server.NetworkConnector")
                    .javaParser(JavaParser.fromJavaVersion().dependsOn(
                            "package org.eclipse.jetty.server; public interface NetworkConnector { int getPort(); }"))
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (m.getSelect() != null && getPort.matches(m)) {
                    maybeAddImport("org.eclipse.jetty.server.NetworkConnector");
                    return template.apply(getCursor(), m.getCoordinates().replace(), m.getSelect());
                }
                return m;
            }
        };
    }
}
