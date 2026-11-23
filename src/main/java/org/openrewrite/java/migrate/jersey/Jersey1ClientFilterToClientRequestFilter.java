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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.migrate.jersey.Jersey2Templates.registrationTarget;

/**
 * Jersey 1 chains client filters: each one calls the next with {@code getNext().handle(request)} and gets the response
 * back. Jersey 2 splits that into request and response filters, so a Jersey 1 filter that only works on the request
 * and then hands it on is a {@code ClientRequestFilter}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ClientFilterToClientRequestFilter extends ScanningRecipe<Jersey1ClientFilterToClientRequestFilter.Accumulator> {

    private static final String CLIENT_FILTER = "com.sun.jersey.api.client.filter.ClientFilter";
    private static final String CLIENT_REQUEST = "com.sun.jersey.api.client.ClientRequest";
    private static final String CLIENT_RESPONSE = "com.sun.jersey.api.client.ClientResponse";
    private static final String CLIENT_HANDLER_EXCEPTION = "com.sun.jersey.api.client.ClientHandlerException";
    private static final String REQUEST_FILTER = "javax.ws.rs.client.ClientRequestFilter";
    private static final String REQUEST_CONTEXT = "javax.ws.rs.client.ClientRequestContext";
    private static final String IO_EXCEPTION = "java.io.IOException";

    private static final MethodMatcher GET_NEXT = new MethodMatcher(CLIENT_FILTER + " getNext()");

    /**
     * The calls on a Jersey 1 {@code ClientRequest} that a {@code ClientRequestContext} answers the same way, by the
     * name they have there.
     */
    private static final Map<String, String> REQUEST_METHODS = new HashMap<>();

    static {
        REQUEST_METHODS.put("getHeaders", "getHeaders");
        REQUEST_METHODS.put("getMetadata", "getHeaders");
        REQUEST_METHODS.put("getURI", "getUri");
        REQUEST_METHODS.put("setURI", "setUri");
        REQUEST_METHODS.put("getMethod", "getMethod");
        REQUEST_METHODS.put("setMethod", "setMethod");
        REQUEST_METHODS.put("getEntity", "getEntity");
        REQUEST_METHODS.put("setEntity", "setEntity");
    }

    String displayName = "Turn Jersey 1 client filters that only change the request into `ClientRequestFilter`s";

    String description = "Turns a subclass of the Jersey 1 `ClientFilter`, anonymous, inner or top-level, whose " +
            "`handle(ClientRequest)` only reads or changes the request and then returns `getNext().handle(request)`, " +
            "into a JAX-RS `ClientRequestFilter` with `filter(ClientRequestContext)`, and replaces `addFilter(..)` of it " +
            "on a `Client` or `WebResource` with `register(..)`. The calls on the request become their " +
            "`ClientRequestContext` counterparts: `getHeaders()` and `getMetadata()` become `getHeaders()`, " +
            "`getURI()` and `setURI(..)` become `getUri()` and `setUri(..)`, and `getMethod()`, `setMethod(..)`, " +
            "`getEntity()` and `setEntity(..)` stay. Filters that read the response, call `getNext()` anywhere else, use " +
            "the request in any other way, such as its properties, its adapter or passing it on to a method, are extended " +
            "by another class, or are used as anything but what `addFilter(..)` is given, are left alone. Jersey 1 ran the " +
            "filter added last first, while Jersey 2 runs request filters by priority and then in the order they were " +
            "registered.";

    public static class Accumulator {
        final Set<String> convertible = new HashSet<>();
        final Set<String> blocked = new HashSet<>();

        boolean converts(@Nullable JavaType type) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            return fq != null && convertible.contains(fq.getFullyQualifiedName()) &&
                   !blocked.contains(fq.getFullyQualifiedName());
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (classDecl.getType() != null && classDecl.getExtends() != null) {
                    JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(classDecl.getExtends().getType());
                    if (supertype != null) {
                        if (CLIENT_FILTER.equals(supertype.getFullyQualifiedName())) {
                            if (requestOnly(classDecl.getBody())) {
                                acc.convertible.add(classDecl.getType().getFullyQualifiedName());
                            }
                        } else {
                            // a subclass of a filter may rely on what it inherits from ClientFilter
                            acc.blocked.add(supertype.getFullyQualifiedName());
                        }
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (newClass.getBody() != null) {
                    JavaType.FullyQualified created = newClass.getClazz() == null ? null :
                            TypeUtils.asFullyQualified(newClass.getClazz().getType());
                    if (created != null) {
                        acc.blocked.add(created.getFullyQualifiedName());
                    }
                } else {
                    checkUsage(newClass, newClass.getType());
                }
                return n;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                if (identifier.getFieldType() != null || "this".equals(identifier.getSimpleName())) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    boolean declaration = parent instanceof J.VariableDeclarations.NamedVariable &&
                                          ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier;
                    boolean fieldName = parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier;
                    if (!declaration && !fieldName) {
                        checkUsage(identifier, identifier.getType());
                    }
                }
                return super.visitIdentifier(identifier, ctx);
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (fieldAccess.getName().getFieldType() != null) {
                    checkUsage(fieldAccess, fieldAccess.getType());
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                checkUsage(method, method.getType());
                return super.visitMethodInvocation(method, ctx);
            }

            /**
             * Blocks a filter class from being converted when an expression of its type is used where a
             * {@code ClientRequestFilter} would not compile, such as {@code removeFilter(..)} or a variable of type
             * {@code ClientFilter}.
             */
            private void checkUsage(Expression expression, @Nullable JavaType type) {
                if (!(type instanceof JavaType.Class)) {
                    return;
                }
                JavaType.Class filterType = (JavaType.Class) type;
                String name = filterType.getFullyQualifiedName();
                if (CLIENT_FILTER.equals(name) || !TypeUtils.isAssignableTo(CLIENT_FILTER, filterType)) {
                    return;
                }
                Object parent = getCursor().getParentTreeCursor().getValue();
                boolean allowed = false;
                if (parent instanceof J.MethodInvocation) {
                    J.MethodInvocation call = (J.MethodInvocation) parent;
                    if (call.getSelect() == expression) {
                        // what a filter calls on itself has been checked with the filter
                        allowed = isThis(expression) ||
                                  !"handle".equals(call.getSimpleName()) && !"getNext".equals(call.getSimpleName());
                    } else {
                        allowed = registrationTarget(call) != null ||
                                  takesItsOwnType(call.getMethodType(), call.getArguments(), expression, name);
                    }
                } else if (parent instanceof J.NewClass) {
                    J.NewClass call = (J.NewClass) parent;
                    allowed = takesItsOwnType(call.getConstructorType(), call.getArguments(), expression, name);
                } else if (parent instanceof J.VariableDeclarations.NamedVariable) {
                    allowed = TypeUtils.isOfClassType(((J.VariableDeclarations.NamedVariable) parent).getType(), name);
                } else if (parent instanceof J.Assignment) {
                    allowed = TypeUtils.isOfClassType(((J.Assignment) parent).getVariable().getType(), name);
                } else if (parent instanceof J.FieldAccess) {
                    allowed = ((J.FieldAccess) parent).getTarget() == expression;
                }
                if (!allowed) {
                    acc.blocked.add(name);
                }
            }
        };
    }

    private static boolean isThis(Expression expression) {
        return expression instanceof J.Identifier && "this".equals(((J.Identifier) expression).getSimpleName());
    }

    private static boolean takesItsOwnType(JavaType.@Nullable Method method, List<Expression> arguments,
                                           Expression argument, String type) {
        int index = arguments.indexOf(argument);
        return method != null && index >= 0 && index < method.getParameterTypes().size() &&
               TypeUtils.isOfClassType(method.getParameterTypes().get(index), type);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> check = Preconditions.or(
                new UsesType<>(CLIENT_FILTER, true),
                new UsesMethod<>(Jersey2Templates.ADD_FILTER));
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                if (c.getType() == null || !acc.converts(c.getType()) || c.getExtends() == null ||
                    !TypeUtils.isOfClassType(c.getExtends().getType(), CLIENT_FILTER)) {
                    return c;
                }
                Jersey2Types types = Jersey2Types.get(ctx);
                JavaType.Class classType = (JavaType.Class) c.getType();
                JavaType.Class newType = classType
                        .withSupertype(types.object)
                        .withInterfaces(ListUtils.concat(classType.getInterfaces(), types.requestFilter));

                J.Identifier requestFilter = identifier("ClientRequestFilter", types.requestFilter, Space.SINGLE_SPACE);
                JContainer<TypeTree> implementing = c.getPadding().getImplements();
                if (implementing == null) {
                    implementing = JContainer.build(Space.SINGLE_SPACE,
                            singletonList(JRightPadded.<TypeTree>build(requestFilter)), Markers.EMPTY);
                } else {
                    implementing = implementing.getPadding().withElements(ListUtils.concat(
                            implementing.getPadding().getElements(), JRightPadded.<TypeTree>build(requestFilter)));
                }
                c = c.getPadding().withExtends(null)
                        .getPadding().withImplements(implementing)
                        .withType(newType)
                        .withBody(convertBody(c.getBody(), newType, types));
                updateImports();
                return c;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                String target = registrationTarget(m);
                if (target == null) {
                    return m;
                }
                Expression filter = m.getArguments().get(0);
                if (filter instanceof J.NewClass && ((J.NewClass) filter).getBody() != null &&
                    TypeUtils.isOfClassType(((J.NewClass) filter).getClazz() == null ? null :
                            ((J.NewClass) filter).getClazz().getType(), CLIENT_FILTER) &&
                    requestOnly(((J.NewClass) filter).getBody())) {
                    filter = convertAnonymous((J.NewClass) filter, Jersey2Types.get(ctx));
                    updateImports();
                } else if (!acc.converts(filter.getType())) {
                    return m;
                }
                Jersey2Types types = Jersey2Types.get(ctx);
                JavaType.Method register = "javax.ws.rs.client.Client".equals(target) ? types.clientRegister : types.targetRegister;
                return m.withName(m.getName().withSimpleName("register").withType(register))
                        .withMethodType(register)
                        .withArguments(singletonList(filter));
            }

            private void updateImports() {
                maybeAddImport(REQUEST_FILTER);
                maybeAddImport(REQUEST_CONTEXT);
                maybeAddImport(IO_EXCEPTION);
                maybeRemoveImport(CLIENT_FILTER);
                maybeRemoveImport(CLIENT_REQUEST);
                maybeRemoveImport(CLIENT_RESPONSE);
                maybeRemoveImport(CLIENT_HANDLER_EXCEPTION);
            }
        });
    }

    private static J.NewClass convertAnonymous(J.NewClass newClass, Jersey2Types types) {
        JavaType.FullyQualified anonymousType = null;
        if (newClass.getType() instanceof JavaType.Class) {
            JavaType.Class classType = (JavaType.Class) newClass.getType();
            anonymousType = classType.withSupertype(types.object).withInterfaces(singletonList(types.requestFilter));
        }
        TypeTree clazz = Objects.requireNonNull(newClass.getClazz());
        return newClass
                .withClazz(identifier("ClientRequestFilter", types.requestFilter, clazz.getPrefix()))
                .withBody(convertBody(Objects.requireNonNull(newClass.getBody()), anonymousType, types));
    }

    private static J.Block convertBody(J.Block body, JavaType.@Nullable FullyQualified declaringType, Jersey2Types types) {
        return body.withStatements(ListUtils.map(body.getStatements(), statement -> {
            J.MethodDeclaration handle = handleMethod(statement);
            return handle == null ? statement : convertHandle(handle, declaringType, types);
        }));
    }

    private static J.MethodDeclaration convertHandle(J.MethodDeclaration handle, JavaType.@Nullable FullyQualified declaringType,
                                                     Jersey2Types types) {
        J.VariableDeclarations parameter = (J.VariableDeclarations) handle.getParameters().get(0);
        String parameterName = parameter.getVariables().get(0).getSimpleName();

        JavaType.Method filterType = types.filter.withParameterNames(singletonList(parameterName));
        if (declaringType != null) {
            filterType = filterType.withDeclaringType(declaringType);
        }
        JavaType.Variable oldVariable = parameter.getVariables().get(0).getVariableType();
        JavaType.Variable newVariable = oldVariable == null ? null :
                oldVariable.withType(types.requestContext).withOwner(filterType);

        J.VariableDeclarations newParameter = parameter
                .withTypeExpression(identifier("ClientRequestContext", types.requestContext,
                        Objects.requireNonNull(parameter.getTypeExpression()).getPrefix()))
                .withVariables(ListUtils.map(parameter.getVariables(), v -> v
                        .withName(v.getName().withType(types.requestContext).withFieldType(newVariable))
                        .withVariableType(newVariable)));

        J.Identifier ioException = identifier("IOException", types.ioException, Space.SINGLE_SPACE);
        JContainer<NameTree> oldThrows = handle.getPadding().getThrows();
        JContainer<NameTree> newThrows = oldThrows == null ?
                JContainer.build(Space.SINGLE_SPACE, singletonList(JRightPadded.<NameTree>build(ioException)), Markers.EMPTY) :
                oldThrows.getPadding().withElements(singletonList(JRightPadded.<NameTree>build(
                        ioException.withPrefix(oldThrows.getElements().get(0).getPrefix()))));

        J.Block body = (J.Block) new RequestToContext(parameterName, newVariable, types)
                .visitNonNull(Objects.requireNonNull(handle.getBody()), 0);
        List<Statement> statements = body.getStatements();
        if (!statements.isEmpty() && statements.get(statements.size() - 1) instanceof J.Return) {
            body = body.withStatements(statements.subList(0, statements.size() - 1));
        }

        TypeTree returnType = Objects.requireNonNull(handle.getReturnTypeExpression());
        return handle
                .withReturnTypeExpression(new J.Primitive(Tree.randomId(), returnType.getPrefix(), Markers.EMPTY, JavaType.Primitive.Void))
                .withName(handle.getName().withSimpleName("filter").withType(filterType))
                .withParameters(singletonList(newParameter))
                .getPadding().withThrows(newThrows)
                .withBody(body)
                .withMethodType(filterType);
    }

    /**
     * Retypes the request parameter, renames the calls on it and turns {@code return getNext().handle(request)} into
     * {@code return}.
     */
    private static class RequestToContext extends JavaVisitor<Integer> {
        private final String parameterName;
        private final JavaType.@Nullable Variable variable;
        private final Jersey2Types types;

        RequestToContext(String parameterName, JavaType.@Nullable Variable variable, Jersey2Types types) {
            this.parameterName = parameterName;
            this.variable = variable;
            this.types = types;
        }

        @Override
        public J visitReturn(J.Return aReturn, Integer p) {
            if (isHandOn(aReturn, parameterName)) {
                return aReturn.withExpression(null);
            }
            return super.visitReturn(aReturn, p);
        }

        @Override
        public J visitIdentifier(J.Identifier identifier, Integer p) {
            if (isParameter(identifier, parameterName)) {
                return identifier.withType(types.requestContext).withFieldType(variable);
            }
            return super.visitIdentifier(identifier, p);
        }

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, Integer p) {
            J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, p);
            if (!isParameter(method.getSelect(), parameterName)) {
                return m;
            }
            String name = REQUEST_METHODS.get(m.getSimpleName());
            JavaType.Method methodType = name == null ? null : types.requestContextMethod(name, m.getArguments());
            return m.withName(m.getName().withSimpleName(name == null ? m.getSimpleName() : name).withType(methodType))
                    .withMethodType(methodType);
        }
    }

    /**
     * @return whether the class body has a {@code handle(ClientRequest)} that does nothing with the response but
     * return it, hands on the request it was given, and only uses that request in ways a {@code ClientRequestContext}
     * offers too.
     */
    private static boolean requestOnly(J.Block classBody) {
        J.MethodDeclaration handle = null;
        for (Statement statement : classBody.getStatements()) {
            J.MethodDeclaration candidate = handleMethod(statement);
            if (candidate != null) {
                handle = candidate;
            }
        }
        if (handle == null || handle.getBody() == null) {
            return false;
        }
        String parameterName = ((J.VariableDeclarations) handle.getParameters().get(0)).getVariables().get(0).getSimpleName();

        // every call of getNext() or handle(..) in the class is in a `return getNext().handle(request)` of handle(..)
        AtomicInteger chainCalls = new AtomicInteger();
        new JavaIsoVisitor<AtomicInteger>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicInteger count) {
                if (GET_NEXT.matches(method) || "handle".equals(method.getSimpleName())) {
                    count.incrementAndGet();
                }
                return super.visitMethodInvocation(method, count);
            }
        }.visit(classBody, chainCalls);

        AtomicInteger handOns = new AtomicInteger();
        AtomicBoolean otherUse = new AtomicBoolean();
        new JavaIsoVisitor<AtomicInteger>() {
            @Override
            public J.Return visitReturn(J.Return aReturn, AtomicInteger count) {
                if (getCursor().firstEnclosing(J.Lambda.class) == null && getCursor().firstEnclosing(J.NewClass.class) == null &&
                    getCursor().firstEnclosing(J.ClassDeclaration.class) == null) {
                    if (isHandOn(aReturn, parameterName)) {
                        count.incrementAndGet();
                        return aReturn;
                    }
                    otherUse.set(true);
                }
                return super.visitReturn(aReturn, count);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicInteger count) {
                if (TypeUtils.isOfClassType(identifier.getType(), CLIENT_RESPONSE) && identifier.getFieldType() != null) {
                    otherUse.set(true);
                }
                if (isParameter(identifier, parameterName)) {
                    Object parent = getCursor().getParentTreeCursor().getValue();
                    if (!(parent instanceof J.MethodInvocation) ||
                        ((J.MethodInvocation) parent).getSelect() != identifier ||
                        !REQUEST_METHODS.containsKey(((J.MethodInvocation) parent).getSimpleName())) {
                        otherUse.set(true);
                    }
                }
                return super.visitIdentifier(identifier, count);
            }
        }.visit(handle.getBody(), handOns, new Cursor(null, Cursor.ROOT_VALUE));

        // each hand-on is one getNext() and one handle(..)
        return !otherUse.get() && handOns.get() > 0 && chainCalls.get() == 2 * handOns.get();
    }

    private static J.@Nullable MethodDeclaration handleMethod(Statement statement) {
        if (!(statement instanceof J.MethodDeclaration)) {
            return null;
        }
        J.MethodDeclaration method = (J.MethodDeclaration) statement;
        if (!"handle".equals(method.getSimpleName()) || method.getParameters().size() != 1 ||
            !(method.getParameters().get(0) instanceof J.VariableDeclarations) ||
            !TypeUtils.isOfClassType(((J.VariableDeclarations) method.getParameters().get(0)).getType(), CLIENT_REQUEST) ||
            method.getReturnTypeExpression() == null ||
            !TypeUtils.isOfClassType(method.getReturnTypeExpression().getType(), CLIENT_RESPONSE)) {
            return null;
        }
        return method;
    }

    /**
     * @return whether the statement is {@code return getNext().handle(request)} for the request parameter.
     */
    private static boolean isHandOn(J.Return aReturn, String parameterName) {
        if (!(aReturn.getExpression() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation handle = (J.MethodInvocation) aReturn.getExpression();
        if (!"handle".equals(handle.getSimpleName()) || handle.getArguments().size() != 1 ||
            !isParameter(handle.getArguments().get(0), parameterName) ||
            !(handle.getSelect() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation getNext = (J.MethodInvocation) handle.getSelect();
        return GET_NEXT.matches(getNext) && (getNext.getSelect() == null ||
               isThis(getNext.getSelect()));
    }

    private static boolean isParameter(@Nullable Expression expression, String parameterName) {
        if (!(expression instanceof J.Identifier)) {
            return false;
        }
        J.Identifier identifier = (J.Identifier) expression;
        return parameterName.equals(identifier.getSimpleName()) && identifier.getFieldType() != null &&
               TypeUtils.isOfClassType(identifier.getType(), CLIENT_REQUEST) &&
               identifier.getFieldType().getOwner() instanceof JavaType.Method &&
               "handle".equals(((JavaType.Method) identifier.getFieldType().getOwner()).getName());
    }

    private static J.Identifier identifier(String name, JavaType type, Space prefix) {
        return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, emptyList(), name, type, null);
    }

    /**
     * The JAX-RS 2 types the converted filters refer to, as a parser attributes them.
     */
    private static class Jersey2Types {
        final JavaType.FullyQualified object;
        final JavaType.FullyQualified requestFilter;
        final JavaType.FullyQualified requestContext;
        final JavaType.FullyQualified ioException;
        final JavaType.Method filter;
        final JavaType.Method clientRegister;
        final JavaType.Method targetRegister;

        private Jersey2Types(J.ClassDeclaration stub) {
            JavaType.Class stubType = (JavaType.Class) Objects.requireNonNull(stub.getType());
            object = Objects.requireNonNull(stubType.getSupertype());
            requestFilter = stubType.getInterfaces().get(0);
            J.MethodDeclaration filterMethod = (J.MethodDeclaration) stub.getBody().getStatements().get(0);
            filter = Objects.requireNonNull(filterMethod.getMethodType());
            requestContext = Objects.requireNonNull(TypeUtils.asFullyQualified(filter.getParameterTypes().get(0)));
            ioException = Objects.requireNonNull(TypeUtils.asFullyQualified(filter.getThrownExceptions().get(0)));
            J.MethodDeclaration registerMethod = (J.MethodDeclaration) stub.getBody().getStatements().get(1);
            List<Statement> registrations = Objects.requireNonNull(registerMethod.getBody()).getStatements();
            clientRegister = Objects.requireNonNull(((J.MethodInvocation) registrations.get(0)).getMethodType());
            targetRegister = Objects.requireNonNull(((J.MethodInvocation) registrations.get(1)).getMethodType());
        }

        static Jersey2Types get(ExecutionContext ctx) {
            return ctx.computeMessageIfAbsent(Jersey2Types.class.getName(), k -> {
                SourceFile stub = Jersey2Templates.parser(ctx).build().parse(ctx,
                        "import java.io.IOException;\n" +
                        "import javax.ws.rs.client.*;\n" +
                        "class Stub implements ClientRequestFilter {\n" +
                        "    public void filter(ClientRequestContext requestContext) throws IOException {\n" +
                        "    }\n" +
                        "    void register(Client client, WebTarget target) {\n" +
                        "        client.register(this);\n" +
                        "        target.register(this);\n" +
                        "    }\n" +
                        "}").findFirst().orElseThrow(() -> new IllegalStateException("Could not parse JAX-RS 2 types"));
                return new Jersey2Types(((J.CompilationUnit) stub).getClasses().get(0));
            });
        }

        JavaType.@Nullable Method requestContextMethod(String name, List<Expression> arguments) {
            int count = arguments.size() == 1 && arguments.get(0) instanceof J.Empty ? 0 : arguments.size();
            if (requestContext instanceof JavaType.Class) {
                for (JavaType.Method method : ((JavaType.Class) requestContext).getMethods()) {
                    if (name.equals(method.getName()) && method.getParameterTypes().size() == count) {
                        return method;
                    }
                }
            }
            return null;
        }
    }
}
