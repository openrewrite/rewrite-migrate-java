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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.java.migrate.jersey.Jersey2Templates.canImport;
import static org.openrewrite.java.migrate.jersey.Jersey2Templates.registrationTarget;
import static org.openrewrite.java.migrate.jersey.Jersey2Templates.typeName;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ClientFiltersToFeatures extends Recipe {

    private static final String FILTER_PACKAGE = "com.sun.jersey.api.client.filter.";
    private static final String BASIC_AUTH = FILTER_PACKAGE + "HTTPBasicAuthFilter";
    private static final String DIGEST_AUTH = FILTER_PACKAGE + "HTTPDigestAuthFilter";
    private static final String LOGGING = FILTER_PACKAGE + "LoggingFilter";
    private static final String GZIP = FILTER_PACKAGE + "GZIPContentEncodingFilter";
    private static final String CSRF = FILTER_PACKAGE + "CsrfProtectionFilter";
    private static final String CSRF_2 = "org.glassfish.jersey.client.filter.CsrfProtectionFilter";

    private static final MethodMatcher BASIC_AUTH_NEW = new MethodMatcher(BASIC_AUTH + " <constructor>(java.lang.String, ..)");
    private static final MethodMatcher DIGEST_AUTH_NEW = new MethodMatcher(DIGEST_AUTH + " <constructor>(java.lang.String, ..)");
    private static final MethodMatcher LOGGING_NEW = new MethodMatcher(LOGGING + " <constructor>()");
    private static final MethodMatcher LOGGING_LOGGER_NEW = new MethodMatcher(LOGGING + " <constructor>(java.util.logging.Logger)");
    private static final MethodMatcher LOGGING_LOGGER_SIZE_NEW = new MethodMatcher(LOGGING + " <constructor>(java.util.logging.Logger, int)");
    private static final MethodMatcher GZIP_NEW = new MethodMatcher(GZIP + " <constructor>(..)");
    private static final MethodMatcher CSRF_NEW = new MethodMatcher(CSRF + " <constructor>(..)");

    String displayName = "Register Jersey 2 features in place of Jersey 1 client filters";

    String description = "Replaces `addFilter(new ..)` on a Jersey 1 `Client` or `WebResource` with `register(..)` of " +
            "what Jersey 2 offers instead. `HTTPBasicAuthFilter` becomes the preemptive `HttpAuthenticationFeature.basic(..)` " +
            "and `HTTPDigestAuthFilter` becomes `HttpAuthenticationFeature.digest(..)`, both with the same credentials. " +
            "`LoggingFilter` becomes a `LoggingFeature` that logs to the same `java.util.logging` logger at `INFO`, with " +
            "entities of any media type up to the same size (10 KiB unless given); Jersey 2 redacts the `Authorization` " +
            "header, which Jersey 1 logged. `GZIPContentEncodingFilter` becomes an `EncodingFeature` with `GZipEncoder` that " +
            "compresses request entities when the Jersey 1 filter did, and `CsrfProtectionFilter` becomes the Jersey 2 " +
            "filter of that name. `LoggingFilter` writing to a `PrintStream`, filters that are not created in the " +
            "`addFilter(..)` call itself, and filters added to any other `Filterable`, such as the Apache HTTP client, are " +
            "left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(Jersey2Templates.ADD_FILTER), new JavaVisitor<ExecutionContext>() {

            private boolean csrfOnlyRegisteredInline;

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                csrfOnlyRegisteredInline = !otherReferences(cu, CSRF);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                String target = registrationTarget(m);
                if (target == null || !(m.getArguments().get(0) instanceof J.NewClass)) {
                    return m;
                }
                J.NewClass filter = (J.NewClass) m.getArguments().get(0);
                if (filter.getBody() != null) {
                    return m;
                }
                JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                List<Object> parameters = new ArrayList<>();
                parameters.add(m.getSelect());
                List<Expression> args = filter.getArguments();
                String feature;
                String replaced;
                if (BASIC_AUTH_NEW.matches(filter) || DIGEST_AUTH_NEW.matches(filter)) {
                    replaced = BASIC_AUTH_NEW.matches(filter) ? BASIC_AUTH : DIGEST_AUTH;
                    String factory = BASIC_AUTH_NEW.matches(filter) ? "basic" : "digest";
                    boolean bytes = args.get(1).getType() instanceof JavaType.Array;
                    feature = "HttpAuthenticationFeature." + factory + "(#{any(java.lang.String)}, " +
                              (bytes ? "#{anyArray(byte)}" : "#{any(java.lang.String)}") + ")";
                    parameters.add(args.get(0));
                    parameters.add(args.get(1));
                    maybeAddImport("org.glassfish.jersey.client.authentication.HttpAuthenticationFeature");
                } else if (LOGGING_NEW.matches(filter) || LOGGING_LOGGER_NEW.matches(filter) ||
                           LOGGING_LOGGER_SIZE_NEW.matches(filter)) {
                    replaced = LOGGING;
                    String level = typeName(this, cu, "java.util.logging.Level");
                    String logger;
                    if (LOGGING_NEW.matches(filter)) {
                        // the logger that Jersey 1 logs to by default
                        logger = typeName(this, cu, "java.util.logging.Logger") + ".getLogger(\"" + LOGGING + "\")";
                    } else {
                        logger = "#{any(java.util.logging.Logger)}";
                        parameters.add(args.get(0));
                    }
                    String maxEntitySize;
                    if (LOGGING_LOGGER_SIZE_NEW.matches(filter)) {
                        maxEntitySize = "#{any(int)}";
                        parameters.add(args.get(1));
                    } else {
                        maxEntitySize = "10 * 1024";
                    }
                    feature = "new LoggingFeature(" + logger + ", " + level + ".INFO, " +
                              "LoggingFeature.Verbosity.PAYLOAD_ANY, " + maxEntitySize + ")";
                    maybeAddImport("org.glassfish.jersey.logging.LoggingFeature");
                } else if (GZIP_NEW.matches(filter)) {
                    replaced = GZIP;
                    if (args.isEmpty() || args.get(0) instanceof J.Empty || isLiteral(args.get(0), true)) {
                        feature = "new EncodingFeature(\"gzip\", GZipEncoder.class)";
                    } else if (isLiteral(args.get(0), false)) {
                        feature = "new EncodingFeature(GZipEncoder.class)";
                    } else {
                        feature = "new EncodingFeature(#{any(boolean)} ? \"gzip\" : null, GZipEncoder.class)";
                        parameters.add(args.get(0));
                    }
                    maybeAddImport("org.glassfish.jersey.client.filter.EncodingFeature");
                    maybeAddImport("org.glassfish.jersey.message.GZipEncoder");
                } else if (CSRF_NEW.matches(filter) && csrfOnlyRegisteredInline && canImport(cu, CSRF_2, CSRF)) {
                    replaced = CSRF;
                    if (args.isEmpty() || args.get(0) instanceof J.Empty) {
                        feature = "new CsrfProtectionFilter()";
                    } else {
                        feature = "new CsrfProtectionFilter(#{any(java.lang.String)})";
                        parameters.add(args.get(0));
                    }
                    maybeAddImport(CSRF_2);
                } else {
                    return m;
                }
                maybeRemoveImport(replaced);
                J.MethodInvocation registration = Jersey2Templates.template("#{any(" + target + ")}.register(" + feature + ")", ctx,
                                "org.glassfish.jersey.client.authentication.HttpAuthenticationFeature",
                                "org.glassfish.jersey.logging.LoggingFeature",
                                "org.glassfish.jersey.client.filter.EncodingFeature",
                                "org.glassfish.jersey.message.GZipEncoder",
                                CSRF_2,
                                "java.util.logging.Level",
                                "java.util.logging.Logger")
                        .apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
                if (CSRF.equals(replaced)) {
                    // the template qualifies the Jersey 2 filter while the Jersey 1 filter of that name is imported
                    doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(registration));
                }
                return registration;
            }
        });
    }

    private static boolean isLiteral(Expression e, boolean value) {
        return e instanceof J.Literal && Boolean.valueOf(value).equals(((J.Literal) e).getValue());
    }

    /**
     * @return whether the type is referred to anywhere but in imports and in the {@code addFilter(new ..)} calls that
     * this recipe replaces, in which case its import has to stay.
     */
    private static boolean otherReferences(J.CompilationUnit cu, String fullyQualifiedName) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Import visitImport(J.Import anImport, AtomicBoolean found) {
                return anImport;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if (registrationTarget(method) != null && method.getArguments().get(0) instanceof J.NewClass &&
                    TypeUtils.isOfClassType(method.getArguments().get(0).getType(), fullyQualifiedName)) {
                    J.NewClass filter = (J.NewClass) method.getArguments().get(0);
                    visit(method.getSelect(), found);
                    for (Expression argument : filter.getArguments()) {
                        visit(argument, found);
                    }
                    return method;
                }
                return super.visitMethodInvocation(method, found);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (TypeUtils.isOfClassType(identifier.getType(), fullyQualifiedName)) {
                    found.set(true);
                }
                return identifier;
            }
        }.visit(cu, found);
        return found.get();
    }
}
