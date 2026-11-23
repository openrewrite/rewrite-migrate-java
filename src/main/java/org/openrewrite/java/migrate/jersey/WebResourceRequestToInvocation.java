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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Jersey 1 {@code WebResource} is both the target of a request and, through {@code accept}, {@code type} and
 * {@code header}, the start of building one, and it sends the request itself. In JAX-RS 2 a {@code WebTarget} only
 * names the resource, {@code request(...)} starts an {@code Invocation.Builder}, and the request entity is an
 * {@code Entity} carrying its media type.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class WebResourceRequestToInvocation extends Recipe {

    private static final String WEB_RESOURCE = "com.sun.jersey.api.client.WebResource";
    private static final String WEB_RESOURCE_BUILDER = "com.sun.jersey.api.client.WebResource$Builder";
    private static final MethodMatcher[] REQUEST_BUILDER_METHODS = matchers("com.sun.jersey.api.client.RequestBuilder",
            "accept", "acceptLanguage", "cookie", "entity", "header", "type");
    private static final MethodMatcher[] UNIFORM_INTERFACE_METHODS = matchers("com.sun.jersey.api.client.UniformInterface",
            "delete", "get", "head", "method", "options", "post", "put");

    String displayName = "Send Jersey 1 `WebResource` requests through a JAX-RS 2 `Invocation.Builder`";

    String description = "Rewrites the requests built on a Jersey 1 `WebResource` or `WebResource.Builder` for the JAX-RS 2 " +
            "client API: `accept(..)` on a resource becomes `request(..)`, a request sent from the resource itself " +
            "gets a `request()` first, and the request entity of `post`, `put`, `delete` and `method` becomes an " +
            "`Entity` with the media type given by `type(..)` or `entity(.., type)`. Without one, the media type is " +
            "the one Jersey 1 would have picked for the entity's declared type: `text/plain` for a `String`, " +
            "`application/octet-stream` for bytes, streams and files, `application/x-www-form-urlencoded` for a " +
            "`MultivaluedMap`, `multipart/form-data` for a `FormDataMultiPart`, `application/xml` for a JAXB " +
            "element, and `application/json` for any other object. The Jersey 1 methods that returned nothing read " +
            "the response as a `String`, so that an error status still throws a `WebApplicationException`. " +
            "`delete` with an entity becomes `method(\"DELETE\", ..)`, which Jersey 2 only sends with a non-null " +
            "entity when `ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION` is set. A `type(..)` or " +
            "`entity(..)` that is not followed by the request in the same expression, and `put()` without an entity, " +
            "which Jersey 2 refuses, are left for the compiler to point out.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>("com.sun.jersey.api.client.UniformInterface *(..)", true),
                        new UsesMethod<>("com.sun.jersey.api.client.RequestBuilder *(..)", true)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        BuilderContentType known = BuilderContentType.of(multiVariable, getCursor());
                        if (known != null) {
                            // The request is sent elsewhere, so its media type goes to where it is sent.
                            getCursor().getParentTreeCursor().putMessage(known.key(), known);
                            getCursor().putMessage(DROP_TYPE, known.initializer);
                        }
                        return super.visitVariableDeclarations(multiVariable, ctx);
                    }

                    @Override
                    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        boolean terminal = matchesAny(UNIFORM_INTERFACE_METHODS, method);
                        if (!terminal && !matchesAny(REQUEST_BUILDER_METHODS, method)) {
                            return super.visitMethodInvocation(method, ctx);
                        }
                        // Visit the arguments and the expression the chain starts from first, as they can hold requests of their own.
                        Chain chain = Chain.of(method, terminal);
                        if (chain == null) {
                            return method;
                        }
                        if (!terminal && chain.isDiscarded(getCursor())) {
                            // A Jersey 1 resource makes a new builder for each step, so one nobody sends does nothing.
                            return null;
                        }
                        Chain visited = chain.map(e -> (Expression) visitNonNull(e, ctx, getCursor()));
                        boolean dropsType = method.getId().equals(getCursor().getNearestMessage(DROP_TYPE));
                        Expression knownContentType = null;
                        if (terminal && chain.base instanceof J.Identifier) {
                            BuilderContentType known = getCursor().getNearestMessage(
                                    BuilderContentType.key(((J.Identifier) chain.base).getSimpleName()));
                            if (known != null && known.variable.equals(((J.Identifier) chain.base).getFieldType())) {
                                knownContentType = known.copyOfContentType();
                            }
                        }
                        J rewritten = visited.rewrite(this, getCursor(), method, dropsType, knownContentType, ctx);
                        return rewritten == null ? visited.unchanged() : rewritten;
                    }
                });
    }

    private static final String DROP_TYPE = "jersey.dropContentType";

    /**
     * The media type that a {@code WebResource.Builder} variable was given with {@code type(..)} where it was declared,
     * when the variable keeps that builder, so that the requests sent with it can carry the media type on their
     * entity, which is where JAX-RS 2 takes it from.
     */
    private static class BuilderContentType {
        final JavaType.Variable variable;
        final Expression contentType;
        final UUID initializer;

        BuilderContentType(JavaType.Variable variable, Expression contentType, UUID initializer) {
            this.variable = variable;
            this.contentType = contentType;
            this.initializer = initializer;
        }

        static String key(String variableName) {
            return "jersey.builderContentType." + variableName;
        }

        String key() {
            return key(variable.getName());
        }

        Expression copyOfContentType() {
            return (Expression) new JavaIsoVisitor<Integer>() {
                @Override
                public @Nullable J preVisit(J tree, Integer p) {
                    return tree.withId(Tree.randomId());
                }
            }.visitNonNull(contentType, 0);
        }

        static @Nullable BuilderContentType of(J.VariableDeclarations declarations, Cursor cursor) {
            if (declarations.getVariables().size() != 1 || !(cursor.getParentTreeCursor().getValue() instanceof J.Block) ||
                    !TypeUtils.isOfClassType(declarations.getType(), WEB_RESOURCE_BUILDER)) {
                return null;
            }
            J.VariableDeclarations.NamedVariable variable = declarations.getVariables().get(0);
            if (!(variable.getInitializer() instanceof J.MethodInvocation) || variable.getVariableType() == null) {
                return null;
            }
            Chain chain = Chain.of((J.MethodInvocation) variable.getInitializer(), false);
            if (chain == null || !chain.fromResource) {
                return null;
            }
            Expression contentType = null;
            for (J.MethodInvocation step : chain.steps) {
                if ("entity".equals(step.getSimpleName())) {
                    return null;
                }
                if ("type".equals(step.getSimpleName())) {
                    contentType = step.getArguments().get(0);
                }
            }
            if (contentType == null || !isConstant(contentType) ||
                    changesContentType(cursor.getParentTreeCursor().getValue(), variable.getVariableType())) {
                return null;
            }
            return new BuilderContentType(variable.getVariableType(), contentType, variable.getInitializer().getId());
        }

        private static boolean isConstant(Expression e) {
            if (e instanceof J.Literal) {
                return true;
            }
            JavaType.Variable field = e instanceof J.Identifier ? ((J.Identifier) e).getFieldType() :
                    e instanceof J.FieldAccess ? ((J.FieldAccess) e).getName().getFieldType() : null;
            return field != null && field.hasFlags(Flag.Static, Flag.Final);
        }

        /**
         * Whether the variable is given another builder, other than the same one with more headers.
         */
        private static boolean changesContentType(J scope, JavaType.Variable variable) {
            AtomicBoolean changes = new AtomicBoolean();
            new JavaIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean found) {
                    if (assignment.getVariable() instanceof J.Identifier &&
                            variable.equals(((J.Identifier) assignment.getVariable()).getFieldType())) {
                        Chain chain = assignment.getAssignment() instanceof J.MethodInvocation ?
                                Chain.of((J.MethodInvocation) assignment.getAssignment(), false) : null;
                        if (chain == null || !(chain.base instanceof J.Identifier) ||
                                !variable.equals(((J.Identifier) chain.base).getFieldType()) ||
                                chain.steps.stream().anyMatch(step -> "type".equals(step.getSimpleName()) ||
                                        "entity".equals(step.getSimpleName()))) {
                            found.set(true);
                        }
                    }
                    return super.visitAssignment(assignment, found);
                }
            }.visit(scope, changes);
            return changes.get();
        }
    }

    private static MethodMatcher[] matchers(String type, String... names) {
        MethodMatcher[] matchers = new MethodMatcher[names.length];
        for (int i = 0; i < names.length; i++) {
            matchers[i] = new MethodMatcher(type + " " + names[i] + "(..)", true);
        }
        return matchers;
    }

    private static boolean matchesAny(MethodMatcher[] matchers, J.MethodInvocation method) {
        for (MethodMatcher matcher : matchers) {
            if (matcher.matches(method)) {
                return true;
            }
        }
        return false;
    }

    private interface ExpressionMapper {
        Expression map(Expression e);
    }

    private static class Chain {
        final Expression base;
        final boolean fromResource;
        final List<J.MethodInvocation> steps;
        final J.@Nullable MethodInvocation terminal;

        private Chain(Expression base, boolean fromResource, List<J.MethodInvocation> steps, J.@Nullable MethodInvocation terminal) {
            this.base = base;
            this.fromResource = fromResource;
            this.steps = steps;
            this.terminal = terminal;
        }

        static @Nullable Chain of(J.MethodInvocation method, boolean isTerminal) {
            List<J.MethodInvocation> steps = new ArrayList<>();
            Expression current = isTerminal ? method.getSelect() : method;
            while (current instanceof J.MethodInvocation && matchesAny(REQUEST_BUILDER_METHODS, (J.MethodInvocation) current)) {
                steps.add(0, (J.MethodInvocation) current);
                current = ((J.MethodInvocation) current).getSelect();
            }
            if (current == null) {
                return null;
            }
            boolean fromResource = TypeUtils.isAssignableTo(WEB_RESOURCE, current.getType());
            if (!fromResource && !TypeUtils.isAssignableTo(WEB_RESOURCE_BUILDER, current.getType())) {
                return null;
            }
            return new Chain(current, fromResource, steps, isTerminal ? method : null);
        }

        Chain map(ExpressionMapper mapper) {
            List<J.MethodInvocation> mappedSteps = ListUtils.map(steps, step -> step.withArguments(mapArguments(step.getArguments(), mapper)));
            J.MethodInvocation mappedTerminal = terminal == null ? null :
                    terminal.withArguments(mapArguments(terminal.getArguments(), mapper));
            return new Chain(mapper.map(base), fromResource, mappedSteps, mappedTerminal);
        }

        private static List<Expression> mapArguments(List<Expression> arguments, ExpressionMapper mapper) {
            return ListUtils.map(arguments, argument -> argument instanceof J.Empty ? argument : mapper.map(argument));
        }

        /**
         * The chain as it was, with its arguments and the expression it starts from visited.
         */
        /**
         * Whether the chain is a statement of its own, of a resource, with arguments that cannot do anything either.
         */
        boolean isDiscarded(Cursor cursor) {
            if (!fromResource || !(cursor.getParentTreeCursor().getValue() instanceof J.Block)) {
                return false;
            }
            for (J.MethodInvocation step : steps) {
                for (Expression arg : step.getArguments()) {
                    if (!(arg instanceof J.Literal || arg instanceof J.Identifier || arg instanceof J.FieldAccess ||
                            arg instanceof J.Empty)) {
                        return false;
                    }
                }
            }
            return base instanceof J.Identifier || base instanceof J.FieldAccess;
        }

        J unchanged() {
            Expression select = base;
            for (J.MethodInvocation step : steps) {
                select = step.withSelect(select);
            }
            return terminal == null ? select : terminal.withSelect(select);
        }

        @Nullable
        J rewrite(JavaVisitor<ExecutionContext> visitor, Cursor cursor, J.MethodInvocation original, boolean dropsType,
                  @Nullable Expression knownContentType, ExecutionContext ctx) {
            StringBuilder code = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
            // The whitespace before the '.' of each method in the new chain, from the outermost one in.
            List<Space> dotPrefixes = new ArrayList<>();

            code.append(fromResource ? "#{any(javax.ws.rs.client.WebTarget)}" : "#{any(javax.ws.rs.client.Invocation.Builder)}");
            parameters.add(base);

            Expression contentType = null;
            Expression stepEntity = null;
            boolean changed = false;
            int first = 0;
            if (fromResource) {
                changed = true;
                if (!steps.isEmpty() && "accept".equals(steps.get(0).getSimpleName())) {
                    J.MethodInvocation accept = steps.get(0);
                    if (!appendCall(code, parameters, "request", accept.getArguments())) {
                        return null;
                    }
                    dotPrefixes.add(0, accept.getPadding().getSelect().getAfter());
                    first = 1;
                } else {
                    code.append(".request()");
                    dotPrefixes.add(0, Space.EMPTY);
                }
            }
            for (int i = first; i < steps.size(); i++) {
                J.MethodInvocation step = steps.get(i);
                List<Expression> args = step.getArguments();
                switch (step.getSimpleName()) {
                    case "type":
                        if (terminal == null) {
                            if (!dropsType) {
                                return null;
                            }
                            changed = true;
                            continue;
                        }
                        contentType = args.get(0);
                        changed = true;
                        continue;
                    case "entity":
                        if (terminal == null) {
                            return null;
                        }
                        stepEntity = args.get(0);
                        if (args.size() > 1) {
                            contentType = args.get(1);
                        }
                        changed = true;
                        continue;
                    case "accept":
                    case "acceptLanguage":
                    case "cookie":
                    case "header":
                        if (!appendCall(code, parameters, step.getSimpleName(), args)) {
                            return null;
                        }
                        dotPrefixes.add(0, step.getPadding().getSelect().getAfter());
                        continue;
                    default:
                        return null;
                }
            }

            if (contentType == null) {
                contentType = knownContentType;
            }
            boolean usesEntity = false;
            if (terminal != null) {
                Request request = Request.of(terminal, stepEntity);
                if (request == null) {
                    return null;
                }
                String name = terminal.getSimpleName();
                List<String> args = new ArrayList<>();
                if ("method".equals(name)) {
                    args.add("#{any(java.lang.String)}");
                    parameters.add(request.methodName);
                }
                if (request.entity != null || request.sendsEntity) {
                    if ("delete".equals(name)) {
                        name = "method";
                        args.add("\"DELETE\"");
                    }
                    String entityCode = entity(request.entity, contentType, parameters);
                    if (entityCode == null) {
                        return null;
                    }
                    args.add(entityCode);
                    usesEntity = !"null".equals(entityCode);
                    changed = true;
                }
                if (request.responseType != null) {
                    String responsePlaceholder = placeholder(request.responseType, false);
                    if (responsePlaceholder == null) {
                        return null;
                    }
                    args.add(responsePlaceholder);
                    parameters.add(request.responseType);
                } else if (request.returnsNothing) {
                    args.add("String.class");
                    changed = true;
                }
                code.append('.').append(name).append('(').append(String.join(", ", args)).append(')');
                dotPrefixes.add(0, terminal.getPadding().getSelect().getAfter());
            }
            if (!changed) {
                return null;
            }

            JavaTemplate.Builder template = JavaTemplate.builder(code.toString())
                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.ws.rs-api-2"));
            if (usesEntity) {
                template.imports("javax.ws.rs.client.Entity", "javax.ws.rs.core.MediaType");
            }
            J result = template.build().apply(cursor, original.getCoordinates().replace(), parameters.toArray());
            if (usesEntity) {
                visitor.maybeAddImport("javax.ws.rs.client.Entity");
                if (code.indexOf("MediaType.") >= 0) {
                    visitor.maybeAddImport("javax.ws.rs.core.MediaType");
                }
            }
            return restoreLineBreaks(result, dotPrefixes);
        }

        private static J restoreLineBreaks(J result, List<Space> dotPrefixes) {
            if (!(result instanceof J.MethodInvocation)) {
                return result;
            }
            J.MethodInvocation invocation = (J.MethodInvocation) result;
            if (dotPrefixes.isEmpty() || invocation.getPadding().getSelect() == null) {
                return invocation;
            }
            Expression select = invocation.getSelect();
            if (dotPrefixes.size() > 1) {
                select = (Expression) restoreLineBreaks(select, dotPrefixes.subList(1, dotPrefixes.size()));
            }
            return invocation.getPadding().withSelect(invocation.getPadding().getSelect()
                    .withElement(select)
                    .withAfter(dotPrefixes.get(0)));
        }

        private static boolean appendCall(StringBuilder code, List<Object> parameters, String name, List<Expression> args) {
            code.append('.').append(name).append('(');
            boolean firstArg = true;
            for (Expression arg : args) {
                if (arg instanceof J.Empty) {
                    continue;
                }
                String placeholder = placeholder(arg, "header".equals(name) && !firstArg);
                if (placeholder == null) {
                    return false;
                }
                if (!firstArg) {
                    code.append(", ");
                }
                code.append(placeholder);
                parameters.add(arg);
                firstArg = false;
            }
            code.append(')');
            return true;
        }

        /**
         * The code for the request entity, adding its parameters, or null when it cannot be built.
         */
        private static @Nullable String entity(@Nullable Expression entity, @Nullable Expression contentType, List<Object> parameters) {
            if (entity == null || entity instanceof J.Literal && ((J.Literal) entity).getValue() == null) {
                return "null";
            }
            if (contentType != null) {
                String typePlaceholder = placeholder(contentType, false);
                if (typePlaceholder == null) {
                    return null;
                }
                parameters.add(entity);
                parameters.add(contentType);
                return "Entity.entity(#{any(java.lang.Object)}, " + typePlaceholder + ")";
            }
            JavaType type = entity.getType();
            if (type == null || type instanceof JavaType.Unknown) {
                return null;
            }
            parameters.add(entity);
            if (TypeUtils.isString(type) || TypeUtils.isAssignableTo("java.io.Reader", type)) {
                return "Entity.text(#{any(java.lang.Object)})";
            }
            if (type instanceof JavaType.Array ||
                    TypeUtils.isAssignableTo("java.io.InputStream", type) ||
                    TypeUtils.isAssignableTo("java.io.File", type) ||
                    TypeUtils.isAssignableTo("javax.activation.DataSource", type)) {
                return "Entity.entity(#{any(java.lang.Object)}, MediaType.APPLICATION_OCTET_STREAM_TYPE)";
            }
            if (TypeUtils.isAssignableTo("javax.ws.rs.core.MultivaluedMap", type)) {
                return "Entity.entity(#{any(java.lang.Object)}, MediaType.APPLICATION_FORM_URLENCODED_TYPE)";
            }
            if (TypeUtils.isAssignableTo("com.sun.jersey.multipart.MultiPart", type)) {
                if (!TypeUtils.isAssignableTo("com.sun.jersey.multipart.FormDataMultiPart", type)) {
                    parameters.remove(parameters.size() - 1);
                    return null;
                }
                return "Entity.entity(#{any(java.lang.Object)}, MediaType.MULTIPART_FORM_DATA_TYPE)";
            }
            if (TypeUtils.isAssignableTo("javax.xml.bind.JAXBElement", type) ||
                    type instanceof JavaType.FullyQualified && isXmlRootElement((JavaType.FullyQualified) type)) {
                return "Entity.xml(#{any(java.lang.Object)})";
            }
            return "Entity.json(#{any(java.lang.Object)})";
        }

        private static boolean isXmlRootElement(JavaType.FullyQualified type) {
            for (JavaType.FullyQualified annotation : type.getAnnotations()) {
                if ("javax.xml.bind.annotation.XmlRootElement".equals(annotation.getFullyQualifiedName()) ||
                        "jakarta.xml.bind.annotation.XmlRootElement".equals(annotation.getFullyQualifiedName())) {
                    return true;
                }
            }
            return false;
        }

        private static @Nullable String placeholder(Expression arg, boolean anyObject) {
            if (anyObject) {
                return "#{any(java.lang.Object)}";
            }
            JavaType type = arg.getType();
            if (type instanceof JavaType.Array) {
                JavaType element = ((JavaType.Array) type).getElemType();
                String elementName = simpleTypeName(element);
                return elementName == null ? null : "#{anyArray(" + elementName + ")}";
            }
            String name = simpleTypeName(type);
            if (name == null) {
                return null;
            }
            return "#{any(" + name + ")}";
        }

        private static @Nullable String simpleTypeName(@Nullable JavaType type) {
            if (TypeUtils.isString(type)) {
                return "java.lang.String";
            }
            if (TypeUtils.isOfClassType(type, "javax.ws.rs.core.MediaType")) {
                return "javax.ws.rs.core.MediaType";
            }
            if (TypeUtils.isOfClassType(type, "java.util.Locale")) {
                return "java.util.Locale";
            }
            if (TypeUtils.isAssignableTo("javax.ws.rs.core.Cookie", type)) {
                return "javax.ws.rs.core.Cookie";
            }
            if (TypeUtils.isOfClassType(type, "java.lang.Class")) {
                return "java.lang.Class";
            }
            if (TypeUtils.isAssignableTo("com.sun.jersey.api.client.GenericType", type)) {
                return "javax.ws.rs.core.GenericType";
            }
            return null;
        }
    }

    /**
     * What a call to one of the methods of Jersey 1's {@code UniformInterface} sends and expects back.
     */
    private static class Request {
        @Nullable
        Expression methodName;

        @Nullable
        Expression responseType;

        @Nullable
        Expression entity;

        boolean sendsEntity;
        boolean returnsNothing;

        static @Nullable Request of(J.MethodInvocation terminal, @Nullable Expression stepEntity) {
            JavaType.Method type = terminal.getMethodType();
            if (type == null) {
                return null;
            }
            Request request = new Request();
            List<Expression> args = terminal.getArguments();
            List<JavaType> parameterTypes = type.getParameterTypes();
            for (int i = 0; i < parameterTypes.size(); i++) {
                JavaType parameterType = parameterTypes.get(i);
                Expression arg = args.get(i);
                if (TypeUtils.isOfClassType(parameterType, "java.lang.Object")) {
                    request.entity = arg;
                } else if (TypeUtils.isOfClassType(parameterType, "java.lang.String")) {
                    request.methodName = arg;
                } else {
                    request.responseType = arg;
                }
            }
            if (request.entity == null) {
                request.entity = stepEntity;
            }
            String name = terminal.getSimpleName();
            request.returnsNothing = type.getReturnType() == JavaType.Primitive.Void;
            switch (name) {
                case "post":
                    request.sendsEntity = true;
                    break;
                case "put":
                    if (request.entity == null) {
                        // Jersey 2 refuses a PUT without an entity.
                        return null;
                    }
                    break;
                case "get":
                case "head":
                case "options":
                    if (request.entity != null) {
                        return null;
                    }
                    break;
                case "method":
                    if (request.methodName == null) {
                        return null;
                    }
                    break;
                default:
                    break;
            }
            return request;
        }
    }
}
