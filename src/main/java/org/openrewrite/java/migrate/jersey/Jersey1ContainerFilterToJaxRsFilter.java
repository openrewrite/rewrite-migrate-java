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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.migrate.jersey.Jersey1ContainerFilters.*;

/**
 * Jersey 1 container filters return the request or response they were given, and are registered in lists of the
 * {@code ResourceConfig}. JAX-RS 2 filters return nothing and are registered as providers.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1ContainerFilterToJaxRsFilter extends ScanningRecipe<Accumulator> {

    private static final String CLASS_TYPE = "jersey1ContainerFilter.classType";
    private static final String FILTER_METHOD = "jersey1ContainerFilter.method";
    private static final String KEPT_INTERFACES = "jersey1ContainerFilter.keptInterfaces";

    String displayName = "Turn Jersey 1 container filters into JAX-RS 2 container filters";

    String description = "Turns a class that implements the Jersey 1 `ContainerRequestFilter` or `ContainerResponseFilter`, " +
            "or both, into a JAX-RS 2 `ContainerRequestFilter` or `ContainerResponseFilter` with " +
            "`void filter(ContainerRequestContext)` or `void filter(ContainerRequestContext, ContainerResponseContext)`, " +
            "and registers it with `register(..)` where it was added to `getContainerRequestFilters()` or " +
            "`getContainerResponseFilters()` of a Jersey 1 `ResourceConfig`, or put in its " +
            "`PROPERTY_CONTAINER_REQUEST_FILTERS` or `PROPERTY_CONTAINER_RESPONSE_FILTERS` property as an instance, a " +
            "class literal or the name of one. `return request;` and `return response;` become `return;`. The calls on " +
            "the request and the response become their counterparts on the contexts: `getRequestHeaders()` and " +
            "`getHttpHeaders()` become `getHeaders()`, `getRequestHeader(name)` becomes `getHeaders().get(name)`, " +
            "`getHeaderValue(name)` becomes `getHeaderString(name)`, `getStatusType()` and `setStatusType(..)` become " +
            "`getStatusInfo()` and `setStatusInfo(..)`, `getAnnotations()` becomes `getEntityAnnotations()`, " +
            "`getEntityInputStream()` and `setEntityInputStream(..)` become `getEntityStream()` and `setEntityStream(..)`, " +
            "`setUris(..)` becomes `setRequestUri(..)`, the URI and path methods move behind `getUriInfo()`, and " +
            "`getUserPrincipal()`, `isUserInRole(..)`, `isSecure()` and `getAuthenticationScheme()` move behind " +
            "`getSecurityContext()`. A request filter gets `@PreMatching`, as Jersey 1 ran request filters before it " +
            "matched the request to a resource. A class that is both filters but was registered as only one of them is " +
            "registered for that contract only. Filters are left alone when they use the request or response in any " +
            "other way, such as passing it to a method that does not take any `Object`, changing the query parameters " +
            "or path segments that Jersey 2 hands out unmodifiable, changing the request in a response filter, or " +
            "returning a different request or response; when they refer to Jersey 1 elsewhere, implement another " +
            "Jersey 1 interface such as `ResourceFilter` or extend a Jersey 1 class; when they are extended, " +
            "anonymous, or used as anything but a filter, such as by calling `filter(..)` directly; and when a " +
            "`web.xml` lists a filter that is both as only one of them. Jersey 2 runs filters by priority, so the " +
            "order of the Jersey 1 lists is not kept.";

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return Jersey1ContainerFilters.scanner(acc);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new UsesType<>("com.sun.jersey..*", true), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                getCursor().putMessage(KEPT_INTERFACES, keptJersey1Interfaces(cu, acc));
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                JavaType.FullyQualified type = classDecl.getType();
                Set<Phase> phases = type == null ? null : acc.converting(type.getFullyQualifiedName());
                if (phases == null || !(type instanceof JavaType.Class) || !phases.equals(implementedPhases(classDecl))) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }
                JaxRsTypes types = JaxRsTypes.get(ctx);
                JavaType.Class classType = (JavaType.Class) type;
                JavaType.Class newType = classType.withInterfaces(ListUtils.map(classType.getInterfaces(), implemented -> {
                    Phase phase = phase(implemented);
                    return phase == null ? implemented : types.filterInterface(phase);
                }));
                getCursor().putMessage(CLASS_TYPE, newType);
                // before the JAX-RS imports, which would otherwise not be added next to the Jersey 1 ones of the same name
                maybeRemoveImport(REQUEST_FILTER);
                maybeRemoveImport(RESPONSE_FILTER);
                maybeRemoveImport(CONTAINER_REQUEST);
                maybeRemoveImport(CONTAINER_RESPONSE);

                J.ClassDeclaration c = (J.ClassDeclaration) super.visitClassDeclaration(classDecl, ctx);
                JContainer<TypeTree> implementing = Objects.requireNonNull(c.getPadding().getImplements());
                c = c.getPadding().withImplements(implementing.getPadding().withElements(ListUtils.map(
                        implementing.getPadding().getElements(), element -> element.map(implemented -> {
                            Phase phase = phase(implemented.getType());
                            return phase == null ? implemented :
                                    typeTree(phase.jaxRsInterface, types.filterInterface(phase), implemented.getPrefix());
                        })))).withType(newType);
                if (phases.contains(Phase.REQUEST)) {
                    c = preMatching(c, ctx);
                }
                return c;
            }

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                Cursor classCursor = getCursor().dropParentUntil(v -> v instanceof J.ClassDeclaration ||
                        v instanceof J.NewClass || Cursor.ROOT_VALUE.equals(v));
                JavaType.Class classType = classCursor.getMessage(CLASS_TYPE);
                Phase phase = filterPhase(method);
                if (classType == null || phase == null) {
                    return super.visitMethodDeclaration(method, ctx);
                }
                ConvertedMethod converted = new ConvertedMethod(method, phase, classType, JaxRsTypes.get(ctx));
                getCursor().putMessage(FILTER_METHOD, converted);

                J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
                TypeTree returnType = Objects.requireNonNull(m.getReturnTypeExpression());
                m = m.withReturnTypeExpression(new J.Primitive(Tree.randomId(), returnType.getPrefix(), Markers.EMPTY, JavaType.Primitive.Void))
                        .withName(m.getName().withType(converted.methodType))
                        .withMethodType(converted.methodType)
                        .withParameters(ListUtils.map(m.getParameters(), parameter -> convertParameter(parameter, converted)));
                return m.withBody(withoutTrailingReturn(Objects.requireNonNull(m.getBody())));
            }

            private Statement convertParameter(Statement parameter, ConvertedMethod converted) {
                if (!(parameter instanceof J.VariableDeclarations)) {
                    return parameter;
                }
                J.VariableDeclarations declarations = (J.VariableDeclarations) parameter;
                Phase kind = converted.parameters.parameters.get(declarations.getVariables().get(0).getSimpleName());
                JavaType.FullyQualified context = converted.types.context(kind);
                TypeTree typeExpression = Objects.requireNonNull(declarations.getTypeExpression());
                return declarations
                        .withTypeExpression(typeTree(kind == Phase.REQUEST ? REQUEST_CONTEXT : RESPONSE_CONTEXT, context,
                                typeExpression.getPrefix()))
                        .withVariables(ListUtils.map(declarations.getVariables(), v -> {
                            JavaType.Variable variable = converted.variables.get(v.getSimpleName());
                            return v.withName(v.getName().withType(context).withFieldType(variable)).withVariableType(variable);
                        }));
            }

            @Override
            public J visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier i = (J.Identifier) super.visitIdentifier(identifier, ctx);
                ConvertedMethod converted = getCursor().getNearestMessage(FILTER_METHOD);
                Phase parameter = converted == null ? null : converted.parameters.of(i);
                if (parameter == null) {
                    return i;
                }
                return i.withType(converted.types.context(parameter)).withFieldType(converted.variables.get(i.getSimpleName()));
            }

            @Override
            public J visitReturn(J.Return aReturn, ExecutionContext ctx) {
                J.Return r = (J.Return) super.visitReturn(aReturn, ctx);
                ConvertedMethod converted = getCursor().getNearestMessage(FILTER_METHOD);
                if (converted != null && converted.parameters.of(r.getExpression()) != null &&
                        getCursor().dropParentUntil(v -> v instanceof J.MethodDeclaration || v instanceof J.Lambda ||
                                v instanceof J.ClassDeclaration || v instanceof J.NewClass).getValue() instanceof J.MethodDeclaration) {
                    return r.withExpression(null);
                }
                return r;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                ConvertedMethod converted = getCursor().getNearestMessage(FILTER_METHOD);
                Phase parameter = converted == null ? null : converted.parameters.of(m.getSelect());
                if (parameter != null) {
                    Mapping mapping = Mapping.of(parameter, m);
                    return mapping == null ? m : map(m, mapping, converted.types.context(parameter), converted.types);
                }
                Registration registration = Registration.of(m, getCursor());
                if (registration != null && isStatement(getCursor())) {
                    return register(m, registration, ctx);
                }
                return m;
            }

            private J.MethodInvocation register(J.MethodInvocation m, Registration registration, ExecutionContext ctx) {
                Set<Phase> phases = acc.converting(registration.className);
                if (phases == null || !phases.contains(registration.phase)) {
                    return m;
                }
                String contract = "";
                List<String> imports = new ArrayList<>();
                if (phases.size() == 2 && !acc.registeredAsBoth(Objects.requireNonNull(registration.className))) {
                    // Jersey 1 only used it as the filter of the list it was added to
                    String jaxRsInterface = registration.phase.jaxRsInterface;
                    if (importable(jaxRsInterface)) {
                        maybeAddImport(jaxRsInterface);
                        imports.add(jaxRsInterface);
                        contract = ", " + simpleName(jaxRsInterface) + ".class";
                    } else {
                        contract = ", " + jaxRsInterface + ".class";
                    }
                }
                // the key of a property may have been its only use
                maybeRemoveImport(RESOURCE_CONFIG);
                Expression receiver = registration.receiver;
                if (receiver == null) {
                    receiver = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "this",
                            registration.receiverType, null);
                }
                Expression filter = registration.classLiteral != null ? registration.classLiteral : registration.value;
                J.MethodInvocation registered = JavaTemplate.builder("#{any(" + JERSEY2_RESOURCE_CONFIG + ")}.register(" +
                                (registration.classLiteral != null ? "#{any(java.lang.Class)}" : "#{any(java.lang.Object)}") +
                                contract + ")")
                        .imports(imports.toArray(new String[0]))
                        .javaParser(JaxRsTypes.parser(ctx))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), receiver, filter);
                return registration.receiver == null ? registered.withSelect(null) : registered;
            }

            private J.ClassDeclaration preMatching(J.ClassDeclaration c, ExecutionContext ctx) {
                JavaTemplate.Builder template;
                if (importable(PRE_MATCHING)) {
                    maybeAddImport(PRE_MATCHING);
                    template = JavaTemplate.builder("@PreMatching").imports(PRE_MATCHING);
                } else {
                    template = JavaTemplate.builder("@" + PRE_MATCHING);
                }
                return template
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.ws.rs-api-2"))
                        .build()
                        .apply(updateCursor(c), c.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            }

            /**
             * @return the type as an identifier, which is imported, or fully qualified when its simple name is taken.
             */
            private TypeTree typeTree(String fullyQualifiedName, JavaType type, Space prefix) {
                if (importable(fullyQualifiedName)) {
                    maybeAddImport(fullyQualifiedName);
                    return new J.Identifier(Tree.randomId(), prefix, Markers.EMPTY, emptyList(), simpleName(fullyQualifiedName), type, null);
                }
                J.FieldAccess qualified = TypeTree.build(fullyQualifiedName);
                return qualified.withType(type).withPrefix(prefix);
            }

            /**
             * @return whether the type can be referred to by its simple name, once the Jersey 1 filter interfaces that
             * only converted filters refer to are gone.
             */
            private boolean importable(String fullyQualifiedName) {
                J.CompilationUnit cu = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                Set<String> kept = getCursor().getNearestMessage(KEPT_INTERFACES, Collections.<String>emptySet());
                List<String> replaced = new ArrayList<>(Arrays.asList(CONTAINER_REQUEST, CONTAINER_RESPONSE));
                for (Phase phase : Phase.values()) {
                    if (!kept.contains(phase.jersey1Interface)) {
                        replaced.add(phase.jersey1Interface);
                    }
                }
                return Jersey2Templates.canImport(cu, fullyQualifiedName, replaced.toArray(new String[0]));
            }
        });
    }

    private static @Nullable Phase phase(@Nullable JavaType type) {
        for (Phase phase : Phase.values()) {
            if (TypeUtils.isOfClassType(type, phase.jersey1Interface)) {
                return phase;
            }
        }
        return null;
    }

    private static String simpleName(String fullyQualifiedName) {
        return fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
    }

    /**
     * Calls the method of the JAX-RS 2 context that the mapping names, on what {@code getUriInfo()},
     * {@code getSecurityContext()} or {@code getHeaders()} return where the mapping says so.
     */
    private static J.MethodInvocation map(J.MethodInvocation m, Mapping mapping, JavaType.FullyQualified context,
                                          JaxRsTypes types) {
        if (mapping.via == null) {
            JavaType.Method methodType = JaxRsTypes.method(context, mapping.name, mapping.arity);
            return m.withName(m.getName().withSimpleName(mapping.name).withType(methodType)).withMethodType(methodType);
        }
        JavaType.Method viaType = JaxRsTypes.method(context, mapping.via, 0);
        J.MethodInvocation via = new J.MethodInvocation(Tree.randomId(), Space.EMPTY, Markers.EMPTY,
                JRightPadded.build(Objects.requireNonNull(m.getSelect())), null,
                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), mapping.via, viaType, null),
                JContainer.build(Space.EMPTY, singletonList(JRightPadded.<Expression>build(
                        new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                viaType);
        JavaType.Method methodType = "getHeaders".equals(mapping.via) ? types.headersGet :
                JaxRsTypes.method(viaType == null ? null : viaType.getReturnType(), mapping.name, mapping.arity);
        JRightPadded<Expression> select = Objects.requireNonNull(m.getPadding().getSelect());
        return m.getPadding().withSelect(select.withElement(via))
                .withName(m.getName().withSimpleName(mapping.name).withType(methodType))
                .withMethodType(methodType);
    }

    /**
     * Drops the {@code return;} that a trailing {@code return request;} or {@code return response;} became, keeping
     * the comments before it.
     */
    private static J.Block withoutTrailingReturn(J.Block body) {
        List<Statement> statements = body.getStatements();
        if (statements.isEmpty() || !(statements.get(statements.size() - 1) instanceof J.Return) ||
                ((J.Return) statements.get(statements.size() - 1)).getExpression() != null) {
            return body;
        }
        Statement last = statements.get(statements.size() - 1);
        J.Block b = body.withStatements(new ArrayList<>(statements.subList(0, statements.size() - 1)));
        Space prefix = last.getPrefix();
        if (!prefix.getComments().isEmpty() && body.getEnd().getComments().isEmpty()) {
            b = b.withEnd(prefix.withComments(ListUtils.mapLast(prefix.getComments(),
                    comment -> comment.withSuffix(body.getEnd().getWhitespace()))));
        }
        return b;
    }

    /**
     * Whether the method invocation the cursor points at is a statement of its own, so its result is unused.
     */
    private static boolean isStatement(Cursor cursor) {
        Object parent = cursor.getParentTreeCursor().getValue();
        return parent instanceof J.Block ||
                parent instanceof J.If ||
                parent instanceof J.If.Else ||
                parent instanceof J.ForLoop ||
                parent instanceof J.ForEachLoop ||
                parent instanceof J.WhileLoop ||
                parent instanceof J.DoWhileLoop ||
                parent instanceof J.Label ||
                parent instanceof J.Case;
    }

    /**
     * @return the Jersey 1 filter interfaces the compilation unit refers to other than in the {@code implements}
     * clauses of filters that convert, which keep their imports.
     */
    private static Set<String> keptJersey1Interfaces(J.CompilationUnit cu, Accumulator acc) {
        Set<String> kept = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Import visitImport(J.Import anImport, Set<String> k) {
                return anImport;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> k) {
                Set<Phase> phases = classDecl.getType() == null ? null : acc.converting(classDecl.getType().getFullyQualifiedName());
                if (phases != null && phases.equals(implementedPhases(classDecl))) {
                    for (J.Annotation annotation : classDecl.getLeadingAnnotations()) {
                        visit(annotation, k, getCursor());
                    }
                    if (classDecl.getExtends() != null) {
                        visit(classDecl.getExtends(), k, getCursor());
                    }
                    visit(classDecl.getBody(), k, getCursor());
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, k);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> k) {
                Phase phase = identifier.getFieldType() == null ? phase(identifier.getType()) : null;
                if (phase != null) {
                    k.add(phase.jersey1Interface);
                }
                return super.visitIdentifier(identifier, k);
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<String> k) {
                Phase phase = fieldAccess.getName().getFieldType() == null ? phase(fieldAccess.getType()) : null;
                if (phase != null) {
                    k.add(phase.jersey1Interface);
                }
                return super.visitFieldAccess(fieldAccess, k);
            }
        }.visit(cu, kept);
        return kept;
    }

    /**
     * A filter method being converted: what its parameters are, and the types they and the method get.
     */
    private static class ConvertedMethod {
        final FilterParameters parameters;
        final JavaType.Method methodType;
        final Map<String, JavaType.@Nullable Variable> variables = new HashMap<>();
        final JaxRsTypes types;

        ConvertedMethod(J.MethodDeclaration method, Phase phase, JavaType.Class classType, JaxRsTypes types) {
            this.parameters = new FilterParameters(method, phase, classType.getFullyQualifiedName());
            this.types = types;
            List<String> names = new ArrayList<>();
            for (Statement parameter : method.getParameters()) {
                names.add(((J.VariableDeclarations) parameter).getVariables().get(0).getSimpleName());
            }
            this.methodType = types.filterMethod(phase).withDeclaringType(classType).withParameterNames(names);
            for (Statement parameter : method.getParameters()) {
                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) parameter).getVariables()) {
                    JavaType.Variable old = variable.getVariableType();
                    variables.put(variable.getSimpleName(), old == null ? null :
                            old.withType(types.context(Objects.requireNonNull(parameters.parameters.get(variable.getSimpleName()))))
                                    .withOwner(methodType));
                }
            }
        }
    }
}
