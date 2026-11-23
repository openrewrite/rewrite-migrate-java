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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jersey 1 describes the application under test with an {@code AppDescriptor} handed to the {@code JerseyTest}
 * constructor. Jersey 2 asks the test for a {@code DeploymentContext} through {@code configureDeployment()}, or takes
 * the JAX-RS {@code Application} in its constructor.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class JerseyTestToJersey2 extends Recipe {

    private static final String JERSEY_TEST = "com.sun.jersey.test.framework.JerseyTest";
    private static final String APP_DESCRIPTOR = "com.sun.jersey.test.framework.AppDescriptor";
    private static final String WEB_APP_DESCRIPTOR = "com.sun.jersey.test.framework.WebAppDescriptor";
    private static final String WEB_APP_BUILDER = "com.sun.jersey.test.framework.WebAppDescriptor$Builder";
    private static final String LOW_LEVEL_BUILDER = "com.sun.jersey.test.framework.LowLevelAppDescriptor$Builder";
    private static final String SERVLET_DEPLOYMENT_CONTEXT = "org.glassfish.jersey.test.ServletDeploymentContext";

    private static final MethodMatcher SUPER_PACKAGES = new MethodMatcher(JERSEY_TEST + " <constructor>(String[])");
    private static final MethodMatcher SUPER_DESCRIPTOR = new MethodMatcher(JERSEY_TEST + " <constructor>(" + APP_DESCRIPTOR + ")");
    private static final MethodMatcher CONFIGURE = new MethodMatcher(JERSEY_TEST + " configure()", true);
    private static final MethodMatcher GET_TEST_CONTAINER_FACTORY = new MethodMatcher(JERSEY_TEST + " getTestContainerFactory()", true);

    private static final MethodMatcher WEB_BUILDER = new MethodMatcher(WEB_APP_BUILDER + " <constructor>()");
    private static final MethodMatcher WEB_BUILDER_PACKAGES = new MethodMatcher(WEB_APP_BUILDER + " <constructor>(String[])");
    private static final MethodMatcher WEB_BUILDER_INIT_PARAM = new MethodMatcher(WEB_APP_BUILDER + " <constructor>(String, String)");
    private static final MethodMatcher WEB_BUILDER_INIT_PARAMS = new MethodMatcher(WEB_APP_BUILDER + " <constructor>(java.util.Map)");
    private static final MethodMatcher LOW_LEVEL_BUILDER_PACKAGES = new MethodMatcher(LOW_LEVEL_BUILDER + " <constructor>(String[])");
    private static final MethodMatcher LOW_LEVEL_BUILDER_CLASSES = new MethodMatcher(LOW_LEVEL_BUILDER + " <constructor>(Class[])");
    private static final MethodMatcher LOW_LEVEL_BUILDER_CONFIG = new MethodMatcher(LOW_LEVEL_BUILDER + " <constructor>(com.sun.jersey.api.core.ResourceConfig)");

    String displayName = "Migrate Jersey 1 `JerseyTest` to the Jersey 2 test framework";

    String description = "Describes the application under a Jersey 1 `JerseyTest` the way the Jersey 2 test framework " +
            "takes it. A `WebAppDescriptor.Builder` becomes a `ServletDeploymentContext` builder and a " +
            "`LowLevelAppDescriptor.Builder` a `DeploymentContext` builder. The packages handed to the " +
            "`JerseyTest` constructor become a `ResourceConfig` scanning them. A descriptor handed to the " +
            "constructor, or returned by an overridden `configure()`, is returned by `configureDeployment()` " +
            "instead, and a servlet deployment is run on the Grizzly servlet container, as Jersey 1 did, unless " +
            "the test chooses its own container. A descriptor built from the constructor's parameters is left for " +
            "the compiler to point out.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.sun.jersey.test.framework..*", false), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J visited = super.visitNewClass(newClass, ctx);
                if (!(visited instanceof J.NewClass)) {
                    return visited;
                }
                J.NewClass n = (J.NewClass) visited;
                List<Expression> args = arguments(n);
                String code;
                List<Expression> params = args;
                if (WEB_BUILDER.matches(n)) {
                    code = "ServletDeploymentContext.forServlet(ServletContainer.class)";
                } else if (WEB_BUILDER_PACKAGES.matches(n)) {
                    code = "ServletDeploymentContext.forPackages(" + packageList(args) + ")";
                    params = packageListParameters(args);
                } else if (WEB_BUILDER_INIT_PARAM.matches(n)) {
                    code = "new ServletDeploymentContext.Builder(#{any(java.lang.String)}, #{any(java.lang.String)})";
                } else if (WEB_BUILDER_INIT_PARAMS.matches(n)) {
                    code = "ServletDeploymentContext.builder(#{any(java.util.Map)})";
                } else if (LOW_LEVEL_BUILDER_PACKAGES.matches(n)) {
                    code = "DeploymentContext.builder(new ResourceConfig().packages(" + placeholders(args, "java.lang.String") + "))";
                } else if (LOW_LEVEL_BUILDER_CLASSES.matches(n)) {
                    code = "DeploymentContext.builder(new ResourceConfig(" + placeholders(args, "java.lang.Class") + "))";
                } else if (LOW_LEVEL_BUILDER_CONFIG.matches(n)) {
                    code = "DeploymentContext.builder(#{any(org.glassfish.jersey.server.ResourceConfig)})";
                } else {
                    return n;
                }
                maybeRemoveImport(WEB_APP_DESCRIPTOR);
                maybeRemoveImport("com.sun.jersey.test.framework.LowLevelAppDescriptor");
                addImportsFor(code);
                return template(code, ctx).apply(getCursor(), n.getCoordinates().replace(), params.toArray());
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J visited = super.visitMethodInvocation(method, ctx);
                if (!(visited instanceof J.MethodInvocation)) {
                    return visited;
                }
                J.MethodInvocation m = (J.MethodInvocation) visited;
                if (!"super".equals(m.getSimpleName()) || !SUPER_PACKAGES.matches(m)) {
                    return m;
                }
                List<Expression> args = arguments(m);
                if (args.isEmpty()) {
                    return m;
                }
                Expression first = args.get(0);
                Expression config = template("new ResourceConfig().packages(" + placeholders(args, "java.lang.String") + ")", ctx)
                        .apply(new Cursor(getCursor(), first), first.getCoordinates().replace(), args.toArray());
                maybeAddImport("org.glassfish.jersey.server.ResourceConfig");
                return m.withArguments(Collections.singletonList(config.withPrefix(first.getPrefix())));
            }

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                if (c.getType() == null || !TypeUtils.isAssignableTo(JERSEY_TEST, c.getType())) {
                    return c;
                }
                boolean servlet = false;
                boolean choosesContainer = false;
                Expression descriptor = null;
                J.MethodDeclaration constructor = null;
                int constructors = 0;
                for (Statement statement : c.getBody().getStatements()) {
                    if (!(statement instanceof J.MethodDeclaration)) {
                        continue;
                    }
                    J.MethodDeclaration md = (J.MethodDeclaration) statement;
                    if (md.isConstructor()) {
                        constructors++;
                        J.MethodInvocation superCall = superCall(md);
                        if (superCall != null && SUPER_DESCRIPTOR.matches(superCall)) {
                            constructor = md;
                            descriptor = superCall.getArguments().get(0);
                        }
                    } else if (md.getMethodType() != null && GET_TEST_CONTAINER_FACTORY.matches(md.getMethodType())) {
                        choosesContainer = true;
                    } else if (md.getMethodType() != null && CONFIGURE.matches(md.getMethodType()) &&
                            TypeUtils.isAssignableTo(APP_DESCRIPTOR, md.getMethodType().getReturnType())) {
                        servlet |= returnsServletDeployment(md);
                    }
                }

                // Jersey 1 asked an overridden configure() for the descriptor, which Jersey 2 asks configureDeployment() for.
                c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s -> {
                    if (s instanceof J.MethodDeclaration) {
                        J.MethodDeclaration md = (J.MethodDeclaration) s;
                        if (!md.isConstructor() && md.getMethodType() != null && CONFIGURE.matches(md.getMethodType()) &&
                                TypeUtils.isAssignableTo(APP_DESCRIPTOR, md.getMethodType().getReturnType())) {
                            return md.withName(md.getName().withSimpleName("configureDeployment"))
                                    .withMethodType(md.getMethodType().withName("configureDeployment"));
                        }
                    }
                    return s;
                })));

                if (constructor != null && descriptor != null) {
                    if (constructors != 1 || usesParameters(descriptor)) {
                        return c;
                    }
                    servlet |= isServletDeployment(descriptor);
                    c = moveDescriptorToConfigureDeployment(c, constructor, descriptor, ctx);
                }
                if (servlet && !choosesContainer) {
                    c = template("@Override\nprotected TestContainerFactory getTestContainerFactory() {\n" +
                            "    return new GrizzlyWebTestContainerFactory();\n}", ctx)
                            .apply(updateCursor(c), c.getBody().getCoordinates().lastStatement());
                    maybeAddImport("org.glassfish.jersey.test.spi.TestContainerFactory");
                    maybeAddImport("org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory");
                }
                return c;
            }

            private void addImportsFor(String code) {
                for (String type : TEMPLATE_IMPORTS) {
                    if (code.contains(type.substring(type.lastIndexOf('.') + 1) + ".") ||
                            code.contains(type.substring(type.lastIndexOf('.') + 1) + "(")) {
                        maybeAddImport(type);
                    }
                }
            }

            private J.ClassDeclaration moveDescriptorToConfigureDeployment(J.ClassDeclaration c, J.MethodDeclaration constructor,
                                                                           Expression descriptor, ExecutionContext ctx) {
                // The descriptor moves out of the constructor, which goes when nothing else is left in it.
                J.Block body = constructor.getBody();
                boolean empty = body != null && body.getStatements().size() == 1 && constructor.getParameters().stream()
                        .allMatch(p -> p instanceof J.Empty);
                int index = c.getBody().getStatements().indexOf(constructor);
                J.MethodDeclaration withoutSuper = constructor.withBody(body == null ? null :
                        body.withStatements(ListUtils.map(body.getStatements(), (i, s) -> i == 0 ? null : s)));
                c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(),
                        s -> s == constructor ? (empty ? null : withoutSuper) : s)));

                c = template("@Override\nprotected DeploymentContext configureDeployment() {\n" +
                        "    return #{any(org.glassfish.jersey.test.DeploymentContext)};\n}", ctx)
                        .apply(updateCursor(c), c.getBody().getCoordinates().lastStatement(), descriptor);
                maybeAddImport("org.glassfish.jersey.test.DeploymentContext");

                // Put configureDeployment() where the constructor was.
                List<Statement> statements = new ArrayList<>(c.getBody().getStatements());
                Statement configureDeployment = statements.remove(statements.size() - 1);
                statements.add(Math.min(empty ? index : index + 1, statements.size()), configureDeployment);
                return c.withBody(c.getBody().withStatements(statements));
            }
        });
    }

    private static final String[] TEMPLATE_IMPORTS = {
            "org.glassfish.jersey.server.ResourceConfig",
            "org.glassfish.jersey.servlet.ServletContainer",
            "org.glassfish.jersey.test.DeploymentContext",
            "org.glassfish.jersey.test.ServletDeploymentContext",
            "org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory",
            "org.glassfish.jersey.test.spi.TestContainerFactory"
    };

    private static JavaTemplate template(String code, ExecutionContext ctx) {
        return JavaTemplate.builder(code)
                .imports(TEMPLATE_IMPORTS)
                .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx,
                        "javax.ws.rs-api-2", "jersey-common-2", "jersey-server-2", "jersey-container-servlet-core-2",
                        "jersey-test-framework-core-2", "jersey-test-framework-provider-grizzly2-2", "javax.servlet-3"))
                .build();
    }

    private static List<Expression> arguments(MethodCall call) {
        List<Expression> args = new ArrayList<>();
        for (Expression arg : call.getArguments()) {
            if (!(arg instanceof J.Empty)) {
                args.add(arg);
            }
        }
        return args;
    }

    private static String placeholders(List<Expression> args, String type) {
        StringBuilder code = new StringBuilder();
        for (Expression arg : args) {
            if (code.length() > 0) {
                code.append(", ");
            }
            code.append(arg.getType() instanceof JavaType.Array ? "#{anyArray(" + type + ")}" : "#{any(" + type + ")}");
        }
        return code.toString();
    }

    /**
     * Jersey 2 takes the packages of a servlet deployment as one string, the way its init parameter holds them.
     */
    private static String packageList(List<Expression> args) {
        if (args.size() == 1 && !(args.get(0).getType() instanceof JavaType.Array)) {
            return "#{any(java.lang.String)}";
        }
        StringBuilder joined = new StringBuilder();
        for (Expression arg : args) {
            if (!(arg instanceof J.Literal) || !(((J.Literal) arg).getValue() instanceof String)) {
                return "String.join(\",\", " + placeholders(args, "java.lang.String") + ")";
            }
            joined.append(joined.length() == 0 ? "" : ",").append(((J.Literal) arg).getValue());
        }
        return "\"" + joined.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static List<Expression> packageListParameters(List<Expression> args) {
        return packageList(args).contains("#{") ? args : Collections.emptyList();
    }

    private static J.@Nullable MethodInvocation superCall(J.MethodDeclaration constructor) {
        if (constructor.getBody() == null || constructor.getBody().getStatements().isEmpty()) {
            return null;
        }
        Statement first = constructor.getBody().getStatements().get(0);
        return first instanceof J.MethodInvocation && "super".equals(((J.MethodInvocation) first).getSimpleName()) ?
                (J.MethodInvocation) first : null;
    }

    private static boolean isServletDeployment(Expression descriptor) {
        return isServletType(descriptor.getType());
    }

    private static boolean returnsServletDeployment(J.MethodDeclaration configure) {
        if (configure.getMethodType() != null && isServletType(configure.getMethodType().getReturnType())) {
            return true;
        }
        AtomicBoolean servlet = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Return visitReturn(J.Return aReturn, AtomicBoolean found) {
                if (aReturn.getExpression() != null && isServletDeployment(aReturn.getExpression())) {
                    found.set(true);
                }
                return aReturn;
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, AtomicBoolean found) {
                return lambda;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                return classDecl;
            }
        }.visit(configure.getBody(), servlet);
        return servlet.get();
    }

    private static boolean isServletType(@Nullable JavaType type) {
        return TypeUtils.isAssignableTo(WEB_APP_DESCRIPTOR, type) || TypeUtils.isAssignableTo(SERVLET_DEPLOYMENT_CONTEXT, type);
    }

    /**
     * A descriptor built from the constructor's parameters cannot move to a method that has none.
     */
    private static boolean usesParameters(Expression descriptor) {
        AtomicBoolean uses = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                JavaType.Variable variable = identifier.getFieldType();
                if (variable != null && variable.getOwner() instanceof JavaType.Method) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(descriptor, uses);
        return uses.get();
    }
}
