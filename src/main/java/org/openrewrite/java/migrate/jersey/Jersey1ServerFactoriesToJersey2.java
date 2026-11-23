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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ServerFactoriesToJersey2 extends Recipe {

    private static final String JERSEY1_RESOURCE_CONFIG = "com.sun.jersey.api.core.ResourceConfig";
    private static final String HTTP_SERVER_FACTORY = "com.sun.jersey.api.container.httpserver.HttpServerFactory";
    private static final String GRIZZLY_SERVER_FACTORY = "com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory";
    private static final String JDK_HTTP_SERVER_FACTORY = "org.glassfish.jersey.jdkhttp.JdkHttpServerFactory";
    private static final String GRIZZLY_HTTP_SERVER_FACTORY = "org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory";

    private static final String IO_EXCEPTION = "java.io.IOException";
    private static final String PROCESSING_EXCEPTION = "javax.ws.rs.ProcessingException";

    private static final MethodMatcher HTTP_SERVER_CREATE = new MethodMatcher(
            HTTP_SERVER_FACTORY + " create(*, " + JERSEY1_RESOURCE_CONFIG + ")");
    private static final MethodMatcher GRIZZLY_SERVER_CREATE = new MethodMatcher(
            GRIZZLY_SERVER_FACTORY + " createHttpServer(*, " + JERSEY1_RESOURCE_CONFIG + ")");


    String displayName = "Replace Jersey 1 server factories with their Jersey 2 counterparts";

    String description = "Replaces `HttpServerFactory.create(uri, resourceConfig)` with " +
            "`JdkHttpServerFactory.createHttpServer(uri, resourceConfig, false)`, and " +
            "`GrizzlyServerFactory.createHttpServer(uri, resourceConfig)` with " +
            "`GrizzlyHttpServerFactory.createHttpServer(uri, resourceConfig)`, wrapping a `String` URI in " +
            "`URI.create(..)`. The JDK HTTP server stays unstarted and the Grizzly server is started, as in Jersey 1. " +
            "Jersey 2 throws an unchecked `ProcessingException` where Jersey 1 threw an `IOException`, so a " +
            "`catch (IOException e)` around the call catches `ProcessingException` instead when nothing else in its " +
            "`try` block throws an `IOException`, and the same goes for `IOException` among the types of a multi-catch, " +
            "which is dropped there when another of them already catches `ProcessingException`. Overloads without a `ResourceConfig`, for which Jersey 1 scanned the " +
            "class path, and those that take an `IoCComponentProviderFactory` or a handler are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(new UsesMethod<>(HTTP_SERVER_CREATE), new UsesMethod<>(GRIZZLY_SERVER_CREATE)),
                new JavaIsoVisitor<ExecutionContext>() {
                    private final Set<UUID> created = new HashSet<>();

                    @Override
                    public J.Try visitTry(J.Try tryable, ExecutionContext ctx) {
                        J.Try t = super.visitTry(tryable, ctx);
                        if (t.getResources() != null || !createsServer(t.getBody())) {
                            return t;
                        }
                        return t.withCatches(ListUtils.map(t.getCatches(), c -> {
                            J.VariableDeclarations parameter = c.getParameter().getTree();
                            if (parameter.getTypeExpression() instanceof J.MultiCatch) {
                                return multiCatch(c, (J.MultiCatch) parameter.getTypeExpression(), t.getBody());
                            }
                            if (!(parameter.getTypeExpression() instanceof J.Identifier || parameter.getTypeExpression() instanceof J.FieldAccess) ||
                                    !TypeUtils.isOfClassType(parameter.getType(), IO_EXCEPTION) ||
                                    mayThrowIOException(t.getBody())) {
                                return c;
                            }
                            // Jersey 2 wraps the IOException of creating or starting the server in a ProcessingException
                            JavaType.FullyQualified processingException = JavaType.ShallowClass.build(PROCESSING_EXCEPTION);
                            maybeAddImport(PROCESSING_EXCEPTION);
                            maybeRemoveImport(IO_EXCEPTION);
                            J.VariableDeclarations migrated = parameter
                                    .withTypeExpression(new J.Identifier(Tree.randomId(), parameter.getTypeExpression().getPrefix(),
                                            Markers.EMPTY, emptyList(), "ProcessingException", processingException, null))
                                    .withVariables(ListUtils.map(parameter.getVariables(), v -> v
                                            .withName(v.getName().withType(processingException).withFieldType(
                                                    v.getName().getFieldType() == null ? null : v.getName().getFieldType().withType(processingException)))));
                            return c.withParameter(c.getParameter().withTree(migrated));
                        }));
                    }

                    /**
                     * {@code IOException} as one alternative of a multi-catch becomes {@code ProcessingException}, or
                     * is dropped when another alternative already catches it, as those of a multi-catch may not be
                     * subtypes of each other.
                     */
                    private J.Try.Catch multiCatch(J.Try.Catch c, J.MultiCatch multiCatch, J body) {
                        List<NameTree> alternatives = multiCatch.getAlternatives();
                        int index = -1;
                        boolean caughtByAnother = false;
                        for (int i = 0; i < alternatives.size(); i++) {
                            NameTree alternative = alternatives.get(i);
                            if (!(alternative instanceof J.Identifier || alternative instanceof J.FieldAccess)) {
                                return c;
                            }
                            if (TypeUtils.isOfClassType(alternative.getType(), IO_EXCEPTION)) {
                                index = i;
                            } else if (TypeUtils.isAssignableTo(alternative.getType(), JavaType.ShallowClass.build(PROCESSING_EXCEPTION)) ||
                                    TypeUtils.isOfClassType(alternative.getType(), PROCESSING_EXCEPTION) ||
                                    TypeUtils.isOfClassType(alternative.getType(), "java.lang.RuntimeException") ||
                                    TypeUtils.isOfClassType(alternative.getType(), "java.lang.Exception") ||
                                    TypeUtils.isOfClassType(alternative.getType(), "java.lang.Throwable")) {
                                caughtByAnother = true;
                            }
                        }
                        if (index < 0 || mayThrowIOException(body)) {
                            return c;
                        }
                        maybeRemoveImport(IO_EXCEPTION);
                        J.MultiCatch.Padding padding = multiCatch.getPadding();
                        List<JRightPadded<NameTree>> padded = padding.getAlternatives();
                        int ioException = index;
                        if (caughtByAnother) {
                            List<JRightPadded<NameTree>> remaining = ListUtils.map(padded, (i, a) -> i == ioException ? null : a);
                            if (ioException == 0) {
                                remaining = ListUtils.mapFirst(remaining, a -> a.withElement(a.getElement().withPrefix(padded.get(0).getElement().getPrefix())));
                            }
                            if (ioException == padded.size() - 1) {
                                remaining = ListUtils.mapLast(remaining, a -> a.withAfter(padded.get(ioException).getAfter()));
                            }
                            J.VariableDeclarations parameter = c.getParameter().getTree();
                            if (remaining.size() == 1) {
                                // one type left is a catch of that type, not a multi-catch
                                // the alternatives are all identifiers or field accesses, as checked above
                                TypeTree single = (TypeTree) remaining.get(0).getElement().withPrefix(multiCatch.getPrefix());
                                JavaType type = single.getType();
                                return c.withParameter(c.getParameter().withTree(parameter
                                        .withTypeExpression(single)
                                        .withVariables(ListUtils.map(parameter.getVariables(), v -> v
                                                .withName(v.getName().withType(type).withFieldType(
                                                        v.getName().getFieldType() == null ? null : v.getName().getFieldType().withType(type)))))));
                            }
                            return c.withParameter(c.getParameter().withTree(parameter
                                    .withTypeExpression(padding.withAlternatives(remaining))));
                        }
                        JavaType.FullyQualified processingException = JavaType.ShallowClass.build(PROCESSING_EXCEPTION);
                        maybeAddImport(PROCESSING_EXCEPTION);
                        List<JRightPadded<NameTree>> replaced = ListUtils.map(padded, (i, a) -> i != ioException ? a :
                                a.withElement(new J.Identifier(Tree.randomId(), a.getElement().getPrefix(), Markers.EMPTY,
                                        emptyList(), "ProcessingException", processingException, null)));
                        return c.withParameter(c.getParameter().withTree(c.getParameter().getTree()
                                .withTypeExpression(padding.withAlternatives(replaced))));
                    }

                    private boolean createsServer(J body) {
                        return !new JavaIsoVisitor<Set<Boolean>>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<Boolean> found) {
                                if (created.contains(method.getId())) {
                                    found.add(true);
                                }
                                return super.visitMethodInvocation(method, found);
                            }
                        }.reduce(body, new HashSet<>()).isEmpty();
                    }

                    /**
                     * Whether anything in the block but the created servers may throw an {@code IOException}, its
                     * subclasses or superclasses, so that the catch clause is still needed.
                     */
                    private boolean mayThrowIOException(J body) {
                        return !new JavaIsoVisitor<Set<Boolean>>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<Boolean> found) {
                                if (!created.contains(method.getId()) && (method.getMethodType() == null ||
                                        throwsIOException(method.getMethodType().getThrownExceptions()))) {
                                    found.add(true);
                                }
                                return super.visitMethodInvocation(method, found);
                            }

                            @Override
                            public J.NewClass visitNewClass(J.NewClass newClass, Set<Boolean> found) {
                                if (newClass.getConstructorType() == null ||
                                        throwsIOException(newClass.getConstructorType().getThrownExceptions())) {
                                    found.add(true);
                                }
                                // the body of an anonymous class runs elsewhere
                                visit(newClass.getEnclosing(), found);
                                for (Expression argument : newClass.getArguments()) {
                                    visit(argument, found);
                                }
                                return newClass;
                            }

                            @Override
                            public J.Throw visitThrow(J.Throw thrown, Set<Boolean> found) {
                                if (thrown.getException().getType() == null ||
                                        throwsIOException(singletonList(thrown.getException().getType()))) {
                                    found.add(true);
                                }
                                return super.visitThrow(thrown, found);
                            }

                            @Override
                            public J.Lambda visitLambda(J.Lambda lambda, Set<Boolean> found) {
                                return lambda;
                            }

                            @Override
                            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<Boolean> found) {
                                return classDecl;
                            }
                        }.reduce(body, new HashSet<>()).isEmpty();
                    }

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
                                "jersey-container-jdk-http-2", "jersey-container-grizzly2-http-2",
                                "grizzly-framework-2.4", "grizzly-http-server-2.4");
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
                                    .javaParser(parser)
                                    .build();
                            maybeAddImport(GRIZZLY_HTTP_SERVER_FACTORY);
                            maybeRemoveImport(GRIZZLY_SERVER_FACTORY);
                        }
                        if (string) {
                            maybeAddImport("java.net.URI");
                        }
                        J.MethodInvocation server = template.apply(getCursor(), m.getCoordinates().replace(), uri, m.getArguments().get(1));
                        created.add(server.getId());
                        return server;
                    }
                });
    }

    /**
     * Whether a catch clause for {@code IOException} is needed for these thrown types: they hold the exception, one of
     * its subclasses, or {@code Exception} or {@code Throwable}.
     */
    private static boolean throwsIOException(List<JavaType> thrown) {
        for (JavaType type : thrown) {
            if (TypeUtils.isAssignableTo(IO_EXCEPTION, type) ||
                    TypeUtils.isOfClassType(type, "java.lang.Exception") ||
                    TypeUtils.isOfClassType(type, "java.lang.Throwable")) {
                return true;
            }
        }
        return false;
    }
}
