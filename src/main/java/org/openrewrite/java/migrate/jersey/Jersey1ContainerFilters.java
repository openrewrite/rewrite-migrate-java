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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * What {@link Jersey1ContainerFilterToJaxRsFilter} and {@link MigrateJersey1ContainerFiltersInWebXml} share: which
 * Jersey 1 container filters can become JAX-RS 2 filters, which list each one is registered in, and how the calls on
 * a Jersey 1 {@code ContainerRequest} and {@code ContainerResponse} map to the JAX-RS 2 contexts.
 * <p>
 * Both recipes scan the Java sources and the {@code web.xml} files alike, so that they agree on which filters convert.
 */
final class Jersey1ContainerFilters {

    static final String REQUEST_FILTER = "com.sun.jersey.spi.container.ContainerRequestFilter";
    static final String RESPONSE_FILTER = "com.sun.jersey.spi.container.ContainerResponseFilter";
    static final String CONTAINER_REQUEST = "com.sun.jersey.spi.container.ContainerRequest";
    static final String CONTAINER_RESPONSE = "com.sun.jersey.spi.container.ContainerResponse";
    static final String RESOURCE_CONFIG = "com.sun.jersey.api.core.ResourceConfig";
    static final String JERSEY2_RESOURCE_CONFIG = "org.glassfish.jersey.server.ResourceConfig";

    static final String JAXRS_REQUEST_FILTER = "javax.ws.rs.container.ContainerRequestFilter";
    static final String JAXRS_RESPONSE_FILTER = "javax.ws.rs.container.ContainerResponseFilter";
    static final String REQUEST_CONTEXT = "javax.ws.rs.container.ContainerRequestContext";
    static final String RESPONSE_CONTEXT = "javax.ws.rs.container.ContainerResponseContext";
    static final String PRE_MATCHING = "javax.ws.rs.container.PreMatching";

    private static final String JERSEY1_PACKAGE = "com.sun.jersey.";

    private static final MethodMatcher GET_PROPERTIES = new MethodMatcher(RESOURCE_CONFIG + " getProperties()", true);
    private static final MethodMatcher JERSEY2_PROPERTY = new MethodMatcher(
            JERSEY2_RESOURCE_CONFIG + " property(java.lang.String, java.lang.Object)", true);
    private static final MethodMatcher CLASS_GET_NAME = new MethodMatcher("java.lang.Class getName()");

    /**
     * The two kinds of Jersey 1 container filter, each with the list of {@code ResourceConfig} and the
     * initialization parameter that registers it.
     */
    enum Phase {
        REQUEST(REQUEST_FILTER, JAXRS_REQUEST_FILTER, "getContainerRequestFilters",
                "PROPERTY_CONTAINER_REQUEST_FILTERS", "com.sun.jersey.spi.container.ContainerRequestFilters"),
        RESPONSE(RESPONSE_FILTER, JAXRS_RESPONSE_FILTER, "getContainerResponseFilters",
                "PROPERTY_CONTAINER_RESPONSE_FILTERS", "com.sun.jersey.spi.container.ContainerResponseFilters");

        final String jersey1Interface;
        final String jaxRsInterface;
        final MethodMatcher listGetter;
        final String constant;
        final String property;

        Phase(String jersey1Interface, String jaxRsInterface, String listGetter, String constant, String property) {
            this.jersey1Interface = jersey1Interface;
            this.jaxRsInterface = jaxRsInterface;
            this.listGetter = new MethodMatcher(RESOURCE_CONFIG + " " + listGetter + "()", true);
            this.constant = constant;
            this.property = property;
        }

        static @Nullable Phase ofProperty(@Nullable String name) {
            for (Phase phase : values()) {
                if (phase.property.equals(name)) {
                    return phase;
                }
            }
            return null;
        }
    }

    public static class Accumulator {
        /**
         * The classes declared in the sources that can become JAX-RS filters, with the Jersey 1 interfaces each
         * implements.
         */
        final Map<String, Set<Phase>> candidates = new HashMap<>();

        /**
         * Classes used in a way a JAX-RS filter would not compile or behave the same in.
         */
        final Set<String> blocked = new HashSet<>();

        /**
         * The filter lists each class is added to, in Java or in {@code web.xml}.
         */
        final Map<String, Set<Phase>> registrations = new HashMap<>();

        /**
         * Classes that a {@code web.xml} lists as only a request or only a response filter of a servlet.
         */
        final Set<String> listedOnceInWebXml = new HashSet<>();

        /**
         * @return the Jersey 1 interfaces the class implements when it becomes a JAX-RS filter, otherwise {@code null}.
         * A class implementing both is left alone when a {@code web.xml} registers it as only one of them, as
         * Jersey 2 reads class names without contracts and would use it as both.
         */
        @Nullable
        Set<Phase> converting(@Nullable String className) {
            Set<Phase> phases = className == null ? null : candidates.get(className);
            if (phases == null || blocked.contains(className) ||
                    (phases.size() == 2 && listedOnceInWebXml.contains(className))) {
                return null;
            }
            return phases;
        }

        boolean registeredAsBoth(String className) {
            Set<Phase> phases = registrations.get(className);
            return phases != null && phases.size() == 2;
        }

        void register(String className, Phase phase) {
            registrations.computeIfAbsent(className, k -> EnumSet.noneOf(Phase.class)).add(phase);
        }
    }

    private Jersey1ContainerFilters() {
    }

    static boolean isWebXml(Path sourcePath) {
        String fileName = sourcePath.getFileName().toString();
        return "web.xml".equals(fileName) || "web-fragment.xml".equals(fileName);
    }

    /**
     * @return the scanner both recipes use, over Java sources and {@code web.xml} files.
     */
    static TreeVisitor<?, ExecutionContext> scanner(Accumulator acc) {
        JavaIsoVisitor<ExecutionContext> java = new JavaScanner(acc);
        XmlIsoVisitor<ExecutionContext> xml = new WebXmlScanner(acc);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    return java.visit(tree, ctx);
                }
                if (tree instanceof Xml.Document && isWebXml(((Xml.Document) tree).getSourcePath())) {
                    return xml.visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    /**
     * The class names listed by a filter initialization parameter, which Jersey 1 splits at spaces, commas,
     * semicolons and line breaks.
     */
    static List<String> classNames(String value) {
        List<String> names = new ArrayList<>();
        for (String name : value.trim().split("[\\s,;]+")) {
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }

    static @Nullable String paramName(Xml.Tag param) {
        return param.getChildValue("param-name").map(String::trim).orElse(null);
    }

    static String paramValue(Xml.Tag param) {
        return param.getChildValue("param-value").map(String::trim).orElse("");
    }

    private static class WebXmlScanner extends XmlIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        WebXmlScanner(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if ("servlet".equals(tag.getName()) || "filter".equals(tag.getName())) {
                Map<String, Set<Phase>> listed = new HashMap<>();
                for (Xml.Tag param : tag.getChildren("init-param")) {
                    Phase phase = Phase.ofProperty(paramName(param));
                    if (phase != null) {
                        for (String name : classNames(paramValue(param))) {
                            listed.computeIfAbsent(name, k -> EnumSet.noneOf(Phase.class)).add(phase);
                        }
                    }
                }
                for (Map.Entry<String, Set<Phase>> entry : listed.entrySet()) {
                    for (Phase phase : entry.getValue()) {
                        acc.register(entry.getKey(), phase);
                    }
                    if (entry.getValue().size() == 1) {
                        acc.listedOnceInWebXml.add(entry.getKey());
                    }
                }
            }
            return super.visitTag(tag, ctx);
        }
    }

    private static class JavaScanner extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        JavaScanner(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            JavaType.FullyQualified type = classDecl.getType();
            if (type != null) {
                if (classDecl.getExtends() != null) {
                    JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(classDecl.getExtends().getType());
                    if (supertype != null) {
                        // a subclass may rely on the Jersey 1 signatures it inherits
                        acc.blocked.add(supertype.getFullyQualifiedName());
                    }
                }
                Set<Phase> phases = implementedPhases(classDecl);
                if (!phases.isEmpty()) {
                    if (convertible(classDecl, phases)) {
                        acc.candidates.put(type.getFullyQualifiedName(), phases);
                    } else {
                        acc.blocked.add(type.getFullyQualifiedName());
                    }
                }
            }
            return super.visitClassDeclaration(classDecl, ctx);
        }

        @Override
        public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
            if (newClass.getBody() != null) {
                JavaType.FullyQualified created = newClass.getClazz() == null ? null :
                        TypeUtils.asFullyQualified(newClass.getClazz().getType());
                if (created != null) {
                    acc.blocked.add(created.getFullyQualifiedName());
                }
            } else {
                checkUsage(newClass, newClass.getType());
            }
            return super.visitNewClass(newClass, ctx);
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
            blockDirectFilterCall(method.getMethodType());
            Registration registration = Registration.of(method, getCursor());
            if (registration != null && registration.className != null) {
                acc.register(registration.className, registration.phase);
            }
            return super.visitMethodInvocation(method, ctx);
        }

        @Override
        public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
            blockDirectFilterCall(memberRef.getMethodType());
            return super.visitMemberReference(memberRef, ctx);
        }

        /**
         * A call of a Jersey 1 {@code filter(..)} method, such as a unit test of the filter, passes and expects
         * Jersey 1 types, so the filter stays as it is.
         */
        private void blockDirectFilterCall(JavaType.@Nullable Method method) {
            if (method != null && filterPhase(method) != null) {
                acc.blocked.add(method.getDeclaringType().getFullyQualifiedName());
            }
        }

        /**
         * Blocks a filter class when an expression of its type is used where a JAX-RS filter would not compile or
         * would not mean the same, such as a variable of a Jersey 1 type or a collection that Jersey 1 did not read
         * filters from.
         */
        private void checkUsage(Expression expression, @Nullable JavaType type) {
            if (!(type instanceof JavaType.Class)) {
                return;
            }
            JavaType.Class filterType = (JavaType.Class) type;
            String name = filterType.getFullyQualifiedName();
            if (REQUEST_FILTER.equals(name) || RESPONSE_FILTER.equals(name) ||
                    (!TypeUtils.isAssignableTo(REQUEST_FILTER, filterType) && !TypeUtils.isAssignableTo(RESPONSE_FILTER, filterType))) {
                return;
            }
            Cursor parentCursor = getCursor().getParentTreeCursor();
            Object parent = parentCursor.getValue();
            boolean allowed = false;
            if (parent instanceof J.MethodInvocation) {
                J.MethodInvocation call = (J.MethodInvocation) parent;
                if (call.getSelect() == expression) {
                    // what a filter calls on itself has been checked with the filter, and filter(..) is blocked
                    allowed = true;
                } else {
                    Registration registration = Registration.of(call, parentCursor);
                    allowed = (registration != null && registration.value == expression) ||
                            takesItsOwnTypeOrObject(call.getMethodType(), call.getArguments(), expression, name);
                }
            } else if (parent instanceof J.NewClass) {
                J.NewClass call = (J.NewClass) parent;
                allowed = takesItsOwnTypeOrObject(call.getConstructorType(), call.getArguments(), expression, name);
            } else if (parent instanceof J.VariableDeclarations.NamedVariable) {
                allowed = TypeUtils.isOfClassType(((J.VariableDeclarations.NamedVariable) parent).getType(), name);
            } else if (parent instanceof J.Assignment) {
                allowed = TypeUtils.isOfClassType(((J.Assignment) parent).getVariable().getType(), name);
            } else if (parent instanceof J.FieldAccess) {
                allowed = ((J.FieldAccess) parent).getTarget() == expression;
            } else if (parent instanceof J.Return) {
                J.MethodDeclaration enclosing = parentCursor.firstEnclosing(J.MethodDeclaration.class);
                J.Lambda lambda = parentCursor.firstEnclosing(J.Lambda.class);
                allowed = lambda == null && enclosing != null && enclosing.getReturnTypeExpression() != null &&
                        TypeUtils.isOfClassType(enclosing.getReturnTypeExpression().getType(), name);
            } else if (parent instanceof J.Binary) {
                J.Binary.Type operator = ((J.Binary) parent).getOperator();
                allowed = operator == J.Binary.Type.Equal || operator == J.Binary.Type.NotEqual;
            }
            if (!allowed) {
                acc.blocked.add(name);
            }
        }
    }

    private static boolean takesItsOwnTypeOrObject(JavaType.@Nullable Method method, List<Expression> arguments,
                                                   Expression argument, String type) {
        JavaType parameter = parameterType(method, arguments, argument);
        return parameter != null && (TypeUtils.isOfClassType(parameter, type) || isObject(parameter));
    }

    /**
     * @return the type of the parameter the argument is passed to, the component type for a variable arity
     * parameter, or {@code null} if it is not an argument of the method.
     */
    private static @Nullable JavaType parameterType(JavaType.@Nullable Method method, List<Expression> arguments,
                                                    Expression argument) {
        if (method == null) {
            return null;
        }
        int index = -1;
        for (int i = 0; i < arguments.size(); i++) {
            if (arguments.get(i) == argument) {
                index = i;
            }
        }
        List<JavaType> parameters = method.getParameterTypes();
        if (index < 0 || parameters.isEmpty()) {
            return null;
        }
        if (method.hasFlags(Flag.Varargs) && index >= parameters.size() - 1) {
            JavaType last = parameters.get(parameters.size() - 1);
            if (arguments.size() == parameters.size() && argument.getType() instanceof JavaType.Array) {
                // an array passed as the variable arity parameter itself
                return last;
            }
            return last instanceof JavaType.Array ? ((JavaType.Array) last).getElemType() : last;
        }
        return index < parameters.size() ? parameters.get(index) : null;
    }

    private static boolean isObject(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, "java.lang.Object");
    }

    /**
     * @return the Jersey 1 filter interfaces that the class itself names in its {@code implements} clause.
     */
    static Set<Phase> implementedPhases(J.ClassDeclaration classDecl) {
        Set<Phase> phases = EnumSet.noneOf(Phase.class);
        if (classDecl.getImplements() != null) {
            for (TypeTree implemented : classDecl.getImplements()) {
                for (Phase phase : Phase.values()) {
                    if (TypeUtils.isOfClassType(implemented.getType(), phase.jersey1Interface)) {
                        phases.add(phase);
                    }
                }
            }
        }
        return phases;
    }

    /**
     * @return which filter method the method type is: {@code ContainerRequest filter(ContainerRequest)} or
     * {@code ContainerResponse filter(ContainerRequest, ContainerResponse)}.
     */
    static @Nullable Phase filterPhase(JavaType.@Nullable Method method) {
        if (method == null || !"filter".equals(method.getName())) {
            return null;
        }
        List<JavaType> parameters = method.getParameterTypes();
        if (parameters.size() == 1 && TypeUtils.isOfClassType(parameters.get(0), CONTAINER_REQUEST) &&
                TypeUtils.isOfClassType(method.getReturnType(), CONTAINER_REQUEST)) {
            return Phase.REQUEST;
        }
        if (parameters.size() == 2 && TypeUtils.isOfClassType(parameters.get(0), CONTAINER_REQUEST) &&
                TypeUtils.isOfClassType(parameters.get(1), CONTAINER_RESPONSE) &&
                TypeUtils.isOfClassType(method.getReturnType(), CONTAINER_RESPONSE)) {
            return Phase.RESPONSE;
        }
        return null;
    }

    static @Nullable Phase filterPhase(Statement statement) {
        if (!(statement instanceof J.MethodDeclaration) || ((J.MethodDeclaration) statement).getReturnTypeExpression() == null) {
            return null;
        }
        return filterPhase(((J.MethodDeclaration) statement).getMethodType());
    }

    /**
     * @return whether a {@code return} at the cursor returns from the method being looked at, rather than from a
     * lambda or a class declared in it.
     */
    private static boolean returnsFromMethod(Cursor cursor) {
        return cursor.firstEnclosing(J.Lambda.class) == null && cursor.firstEnclosing(J.NewClass.class) == null &&
                cursor.firstEnclosing(J.ClassDeclaration.class) == null && cursor.firstEnclosing(J.MethodDeclaration.class) == null;
    }

    /**
     * @return whether the class only relies on Jersey 1 through the filter methods it declares, and uses their
     * parameters only in ways the JAX-RS 2 contexts offer as well.
     */
    private static boolean convertible(J.ClassDeclaration classDecl, Set<Phase> phases) {
        if (classDecl.getKind() != J.ClassDeclaration.Kind.Type.Class || classDecl.getType() == null) {
            return false;
        }
        if (classDecl.getImplements() != null) {
            for (TypeTree implemented : classDecl.getImplements()) {
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(implemented.getType());
                if (type == null) {
                    return false;
                }
                if (!REQUEST_FILTER.equals(type.getFullyQualifiedName()) &&
                        !RESPONSE_FILTER.equals(type.getFullyQualifiedName()) && inJersey1Hierarchy(type, new HashSet<>())) {
                    return false;
                }
            }
        }
        if (classDecl.getExtends() != null) {
            JavaType.FullyQualified supertype = TypeUtils.asFullyQualified(classDecl.getExtends().getType());
            if (supertype == null || inJersey1Hierarchy(supertype, new HashSet<>())) {
                return false;
            }
        }

        Map<Phase, J.MethodDeclaration> filterMethods = new EnumMap<>(Phase.class);
        for (Statement statement : classDecl.getBody().getStatements()) {
            Phase phase = filterPhase(statement);
            if (phase != null && filterMethods.put(phase, (J.MethodDeclaration) statement) != null) {
                return false;
            }
        }
        if (!filterMethods.keySet().equals(phases)) {
            return false;
        }
        for (Map.Entry<Phase, J.MethodDeclaration> entry : filterMethods.entrySet()) {
            if (entry.getValue().getBody() == null || !convertibleBody(entry.getValue(), entry.getKey())) {
                return false;
            }
        }
        return !usesJersey1Elsewhere(classDecl, filterMethods.values());
    }

    private static boolean inJersey1Hierarchy(JavaType.@Nullable FullyQualified type, Set<String> seen) {
        if (type == null || !seen.add(type.getFullyQualifiedName())) {
            return false;
        }
        if (type.getFullyQualifiedName().startsWith(JERSEY1_PACKAGE)) {
            return true;
        }
        if (inJersey1Hierarchy(type.getSupertype(), seen)) {
            return true;
        }
        for (JavaType.FullyQualified implemented : type.getInterfaces()) {
            if (inJersey1Hierarchy(implemented, seen)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJersey1(@Nullable JavaType type) {
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        return fq != null && fq.getFullyQualifiedName().startsWith(JERSEY1_PACKAGE);
    }

    /**
     * The parameters of a Jersey 1 filter method, by their names, and what they are.
     */
    static final class FilterParameters {
        final Phase phase;
        final String className;
        final Map<String, Phase> parameters = new HashMap<>();

        FilterParameters(J.MethodDeclaration method, Phase phase, String className) {
            this.phase = phase;
            this.className = className;
            List<Statement> declared = method.getParameters();
            for (int i = 0; i < declared.size(); i++) {
                if (declared.get(i) instanceof J.VariableDeclarations) {
                    for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) declared.get(i)).getVariables()) {
                        // a response filter is given the request first, then the response
                        parameters.put(variable.getSimpleName(), i == 0 ? Phase.REQUEST : Phase.RESPONSE);
                    }
                }
            }
        }

        /**
         * @return whether the expression is the request ({@link Phase#REQUEST}) or the response
         * ({@link Phase#RESPONSE}) parameter of the filter method, or {@code null} if it is neither. Recognizes the
         * parameters before and after their type changes.
         */
        @Nullable
        Phase of(@Nullable Expression expression) {
            if (!(expression instanceof J.Identifier)) {
                return null;
            }
            J.Identifier identifier = (J.Identifier) expression;
            JavaType.Variable variable = identifier.getFieldType();
            if (variable == null || !(variable.getOwner() instanceof JavaType.Method)) {
                return null;
            }
            JavaType.Method owner = (JavaType.Method) variable.getOwner();
            if (!"filter".equals(owner.getName()) || owner.getParameterTypes().size() != (phase == Phase.REQUEST ? 1 : 2) ||
                    !className.equals(owner.getDeclaringType().getFullyQualifiedName())) {
                return null;
            }
            return parameters.get(identifier.getSimpleName());
        }
    }

    /**
     * @return whether the filter method only returns the parameter it is meant to hand on, and uses its parameters
     * only to call what their JAX-RS 2 counterparts offer with the same meaning, to pass them where any
     * {@code Object} is taken, such as {@code System.out.println(request)}, to concatenate them to a string or to
     * compare them with {@code null}. Those only use what every object has, so they work the same on the JAX-RS 2
     * contexts, and a method that took the object back to a Jersey 1 type would no longer compile.
     */
    private static boolean convertibleBody(J.MethodDeclaration method, Phase phase) {
        JavaType.FullyQualified declaringType = Objects.requireNonNull(method.getMethodType()).getDeclaringType();
        FilterParameters parameters = new FilterParameters(method, phase, declaringType.getFullyQualifiedName());
        AtomicBoolean convertible = new AtomicBoolean(true);
        new JavaIsoVisitor<AtomicBoolean>() {
            private final Set<String> readOnlyVariables = new HashSet<>();

            @Override
            public J.Return visitReturn(J.Return aReturn, AtomicBoolean ok) {
                if (returnsFromMethod(getCursor()) && parameters.of(aReturn.getExpression()) != phase) {
                    // a request filter hands on the request, a response filter the response
                    ok.set(false);
                }
                return super.visitReturn(aReturn, ok);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean ok) {
                Cursor parentCursor = getCursor().getParentTreeCursor();
                Object parent = parentCursor.getValue();
                Phase parameter = parameters.of(identifier);
                if (parameter != null) {
                    if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getSelect() == identifier) {
                        J.MethodInvocation call = (J.MethodInvocation) parent;
                        Mapping mapping = Mapping.of(parameter, call);
                        if (mapping == null || (mapping.setter && phase == Phase.RESPONSE) ||
                                (mapping.readOnlyResult && !readOnly(parentCursor.getParentTreeCursor(), call))) {
                            // Jersey 2 refuses to change the request while the response is filtered
                            ok.set(false);
                        }
                    } else if (!(parent instanceof J.Return && returnsFromMethod(parentCursor)) &&
                            !takenAsObject(parentCursor, identifier)) {
                        ok.set(false);
                    }
                } else if (identifier.getFieldType() != null && readOnlyVariables.contains(variableKey(identifier.getFieldType())) &&
                        !(parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier) &&
                        !readOnly(parentCursor, identifier)) {
                    ok.set(false);
                }
                return super.visitIdentifier(identifier, ok);
            }

            /**
             * Jersey 2 hands out unmodifiable query parameters, path segments and cookies, and new lists of the
             * acceptable media types and languages, where Jersey 1 handed out the ones it kept.
             */
            private boolean readOnly(Cursor parentCursor, Expression expression) {
                Object parent = parentCursor.getValue();
                if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getSelect() == expression) {
                    J.MethodInvocation call = (J.MethodInvocation) parent;
                    if (!READ_ONLY_METHODS.contains(call.getSimpleName())) {
                        return false;
                    }
                    Object grandParent = parentCursor.getParentTreeCursor().getValue();
                    return !(grandParent instanceof J.MethodInvocation &&
                            ((J.MethodInvocation) grandParent).getSelect() == call &&
                            MUTATING_METHODS.contains(((J.MethodInvocation) grandParent).getSimpleName()));
                }
                if (parent instanceof J.ForEachLoop.Control) {
                    return ((J.ForEachLoop.Control) parent).getIterable() == expression;
                }
                if (parent instanceof J.VariableDeclarations.NamedVariable &&
                        ((J.VariableDeclarations.NamedVariable) parent).getInitializer() == expression) {
                    J.VariableDeclarations.NamedVariable variable = (J.VariableDeclarations.NamedVariable) parent;
                    if (variable.getVariableType() == null || !(variable.getVariableType().getOwner() instanceof JavaType.Method)) {
                        return false;
                    }
                    readOnlyVariables.add(variableKey(variable.getVariableType()));
                    return true;
                }
                return takenAsObject(parentCursor, expression);
            }
        }.visit(method.getBody(), convertible, new Cursor(null, Cursor.ROOT_VALUE));
        return convertible.get();
    }

    private static String variableKey(JavaType.Variable variable) {
        JavaType owner = variable.getOwner();
        return variable.getName() + "@" + (owner instanceof JavaType.Method ?
                ((JavaType.Method) owner).getDeclaringType().getFullyQualifiedName() + "#" + ((JavaType.Method) owner).getName() : "");
    }

    /**
     * @return whether the expression is passed where any {@code Object} is taken, is concatenated to a string or is
     * compared with {@code null}.
     */
    private static boolean takenAsObject(Cursor parentCursor, Expression expression) {
        Object parent = parentCursor.getValue();
        if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getSelect() != expression) {
            J.MethodInvocation call = (J.MethodInvocation) parent;
            return isObject(parameterType(call.getMethodType(), call.getArguments(), expression));
        }
        if (parent instanceof J.NewClass) {
            J.NewClass call = (J.NewClass) parent;
            return isObject(parameterType(call.getConstructorType(), call.getArguments(), expression));
        }
        if (parent instanceof J.Binary) {
            J.Binary binary = (J.Binary) parent;
            if (binary.getOperator() == J.Binary.Type.Addition) {
                return TypeUtils.isString(binary.getType());
            }
            if (binary.getOperator() == J.Binary.Type.Equal || binary.getOperator() == J.Binary.Type.NotEqual) {
                Expression other = binary.getLeft() == expression ? binary.getRight() : binary.getLeft();
                return other instanceof J.Literal && ((J.Literal) other).getValue() == null;
            }
        }
        return false;
    }

    /**
     * @return whether the class body refers to Jersey 1 anywhere but in the signatures of its filter methods and the
     * calls on their parameters.
     */
    private static boolean usesJersey1Elsewhere(J.ClassDeclaration classDecl, Collection<J.MethodDeclaration> filterMethods) {
        Map<J.MethodDeclaration, FilterParameters> parameters = new IdentityHashMap<>();
        for (J.MethodDeclaration method : filterMethods) {
            JavaType.Method type = Objects.requireNonNull(method.getMethodType());
            parameters.put(method, new FilterParameters(method, Objects.requireNonNull(filterPhase(type)),
                    type.getDeclaringType().getFullyQualifiedName()));
        }
        AtomicBoolean uses = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, AtomicBoolean found) {
                if (parameters.containsKey(method)) {
                    // the return type and the parameters change with the method
                    for (J.Annotation annotation : method.getLeadingAnnotations()) {
                        visit(annotation, found, getCursor());
                    }
                    if (method.getThrows() != null) {
                        for (NameTree thrown : method.getThrows()) {
                            visit(thrown, found, getCursor());
                        }
                    }
                    visit(method.getBody(), found, getCursor());
                    return method;
                }
                return super.visitMethodDeclaration(method, found);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean found) {
                if (isJersey1(identifier.getType()) && !isFilterParameter(identifier)) {
                    found.set(true);
                }
                return super.visitIdentifier(identifier, found);
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean found) {
                if (isJersey1(fieldAccess.getType())) {
                    found.set(true);
                }
                return super.visitFieldAccess(fieldAccess, found);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if (method.getMethodType() != null && isJersey1(method.getMethodType().getDeclaringType()) &&
                        !isFilterParameter(method.getSelect())) {
                    found.set(true);
                }
                return super.visitMethodInvocation(method, found);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                if (isJersey1(newClass.getType())) {
                    found.set(true);
                }
                return super.visitNewClass(newClass, found);
            }

            private boolean isFilterParameter(@Nullable Expression expression) {
                for (FilterParameters filterParameters : parameters.values()) {
                    if (filterParameters.of(expression) != null) {
                        return true;
                    }
                }
                return false;
            }
        }.visit(classDecl.getBody(), uses, new Cursor(null, Cursor.ROOT_VALUE));
        return uses.get();
    }

    /**
     * Methods that read a map or a list without changing it.
     */
    private static final Set<String> READ_ONLY_METHODS = new HashSet<>(Arrays.asList(
            "get", "getFirst", "containsKey", "containsValue", "contains", "containsAll", "isEmpty", "size", "keySet",
            "entrySet", "values", "forEach", "indexOf", "lastIndexOf", "iterator", "stream", "toString", "equals",
            "hashCode", "equalsIgnoreValueOrder"));

    private static final Set<String> MUTATING_METHODS = new HashSet<>(Arrays.asList(
            "add", "addAll", "remove", "removeAll", "removeIf", "retainAll", "set", "clear", "replaceAll", "sort"));

    /**
     * How a call on a Jersey 1 {@code ContainerRequest} or {@code ContainerResponse} is made on the JAX-RS 2
     * {@code ContainerRequestContext} or {@code ContainerResponseContext}: by the method of the name it has there,
     * possibly on what {@code getUriInfo()}, {@code getSecurityContext()} or {@code getHeaders()} return.
     */
    static final class Mapping {
        private static final Map<String, Mapping> REQUEST = new HashMap<>();
        private static final Map<String, Mapping> RESPONSE = new HashMap<>();

        static {
            request("getRequestHeaders", 0, "getHeaders", null);
            request("getRequestHeader", 1, "get", "getHeaders");
            request("getHeaderValue", 1, "getHeaderString", null);
            request("getMediaType", 0, "getMediaType", null);
            request("getLanguage", 0, "getLanguage", null);
            request("getMethod", 0, "getMethod", null);
            request("getEntityInputStream", 0, "getEntityStream", null);
            request("getSecurityContext", 0, "getSecurityContext", null);
            readOnly(request("getAcceptableMediaTypes", 0, "getAcceptableMediaTypes", null));
            readOnly(request("getAcceptableLanguages", 0, "getAcceptableLanguages", null));
            readOnly(request("getCookies", 0, "getCookies", null));
            // only allowed while requests are filtered, and before they are matched for the URI and the method
            setter(request("setMethod", 1, "setMethod", null));
            setter(request("setUris", 2, "setRequestUri", null));
            setter(request("setEntityInputStream", 1, "setEntityStream", null));
            setter(request("setSecurityContext", 1, "setSecurityContext", null));
            for (String name : Arrays.asList("getUserPrincipal", "isSecure", "getAuthenticationScheme")) {
                request(name, 0, name, "getSecurityContext");
            }
            request("isUserInRole", 1, "isUserInRole", "getSecurityContext");
            for (String name : Arrays.asList("getBaseUri", "getBaseUriBuilder", "getRequestUri", "getRequestUriBuilder",
                    "getAbsolutePath", "getAbsolutePathBuilder", "getPath")) {
                request(name, 0, name, "getUriInfo");
            }
            request("getPath", 1, "getPath", "getUriInfo");
            for (String name : Arrays.asList("getPathSegments", "getQueryParameters")) {
                readOnly(request(name, 0, name, "getUriInfo"));
                readOnly(request(name, 1, name, "getUriInfo"));
            }

            response("getHttpHeaders", 0, "getHeaders");
            response("getStatus", 0, "getStatus");
            response("setStatus", 1, "setStatus");
            response("getStatusType", 0, "getStatusInfo");
            response("setStatusType", 1, "setStatusInfo");
            response("getEntity", 0, "getEntity");
            response("setEntity", 1, "setEntity");
            response("getEntityType", 0, "getEntityType");
            response("getAnnotations", 0, "getEntityAnnotations");
            response("getMediaType", 0, "getMediaType");
        }

        final String name;
        final int arity;
        final @Nullable String via;
        boolean readOnlyResult;
        boolean setter;

        private Mapping(String name, int arity, @Nullable String via) {
            this.name = name;
            this.arity = arity;
            this.via = via;
        }

        private static Mapping request(String jersey1Name, int arity, String name, @Nullable String via) {
            Mapping mapping = new Mapping(name, arity, via);
            REQUEST.put(jersey1Name + "/" + arity, mapping);
            return mapping;
        }

        private static void response(String jersey1Name, int arity, String name) {
            RESPONSE.put(jersey1Name + "/" + arity, new Mapping(name, arity, null));
        }

        private static void readOnly(Mapping mapping) {
            mapping.readOnlyResult = true;
        }

        private static void setter(Mapping mapping) {
            mapping.setter = true;
        }

        static @Nullable Mapping of(Phase parameter, J.MethodInvocation call) {
            return (parameter == Phase.REQUEST ? REQUEST : RESPONSE).get(call.getSimpleName() + "/" + arity(call));
        }

        static int arity(J.MethodInvocation call) {
            List<Expression> arguments = call.getArguments();
            return arguments.size() == 1 && arguments.get(0) instanceof J.Empty ? 0 : arguments.size();
        }
    }

    /**
     * A statement that adds a filter to the request or response filters of a Jersey 1 {@code ResourceConfig}:
     * {@code getContainerRequestFilters().add(x)}, {@code getContainerResponseFilters().add(x)}, or a
     * {@code getProperties().put(..)} or {@code property(..)} of {@code PROPERTY_CONTAINER_REQUEST_FILTERS} or
     * {@code PROPERTY_CONTAINER_RESPONSE_FILTERS}. The filter is an instance, a class literal or the name of a
     * class literal.
     */
    static final class Registration {
        final Phase phase;
        final @Nullable Expression receiver;
        final JavaType.@Nullable FullyQualified receiverType;
        final Expression value;

        /**
         * The class literal to register, when the value is a class literal or its name.
         */
        final @Nullable Expression classLiteral;

        final @Nullable String className;

        private Registration(Phase phase, @Nullable Expression receiver, JavaType.@Nullable FullyQualified receiverType,
                             Expression value) {
            this.phase = phase;
            this.receiver = receiver;
            this.receiverType = receiverType;
            this.value = value;
            Expression literal = value;
            if (value instanceof J.MethodInvocation && CLASS_GET_NAME.matches((J.MethodInvocation) value)) {
                literal = ((J.MethodInvocation) value).getSelect();
            }
            if (literal instanceof J.FieldAccess && "class".equals(((J.FieldAccess) literal).getSimpleName())) {
                this.classLiteral = literal;
                JavaType.FullyQualified type = TypeUtils.asFullyQualified(((J.FieldAccess) literal).getTarget().getType());
                this.className = type == null ? null : type.getFullyQualifiedName();
            } else {
                this.classLiteral = null;
                JavaType.FullyQualified type = value.getType() instanceof JavaType.Class ? (JavaType.Class) value.getType() : null;
                this.className = type == null ? null : type.getFullyQualifiedName();
            }
        }

        static @Nullable Registration of(J.MethodInvocation call, Cursor cursor) {
            List<Expression> arguments = call.getArguments();
            if ("add".equals(call.getSimpleName()) && arguments.size() == 1 && call.getSelect() instanceof J.MethodInvocation) {
                J.MethodInvocation getter = (J.MethodInvocation) call.getSelect();
                for (Phase phase : Phase.values()) {
                    if (phase.listGetter.matches(getter)) {
                        return of(phase, getter.getSelect(), cursor, arguments.get(0));
                    }
                }
                return null;
            }
            if ("put".equals(call.getSimpleName()) && arguments.size() == 2 && call.getSelect() instanceof J.MethodInvocation &&
                    GET_PROPERTIES.matches((J.MethodInvocation) call.getSelect())) {
                Phase phase = propertyPhase(arguments.get(0));
                return phase == null ? null : of(phase, ((J.MethodInvocation) call.getSelect()).getSelect(), cursor, arguments.get(1));
            }
            if ("property".equals(call.getSimpleName()) && arguments.size() == 2 && JERSEY2_PROPERTY.matches(call)) {
                Phase phase = propertyPhase(arguments.get(0));
                return phase == null ? null : of(phase, call.getSelect(), cursor, arguments.get(1));
            }
            return null;
        }

        private static @Nullable Registration of(Phase phase, @Nullable Expression receiver, Cursor cursor, Expression value) {
            JavaType.FullyQualified receiverType = TypeUtils.asFullyQualified(receiver == null ? enclosingClassType(cursor) : receiver.getType());
            if (!TypeUtils.isAssignableTo(RESOURCE_CONFIG, receiverType) && !TypeUtils.isAssignableTo(JERSEY2_RESOURCE_CONFIG, receiverType)) {
                return null;
            }
            return new Registration(phase, receiver, receiverType, value);
        }

        private static @Nullable Phase propertyPhase(Expression key) {
            if (key instanceof J.Literal && ((J.Literal) key).getValue() instanceof String) {
                return Phase.ofProperty((String) ((J.Literal) key).getValue());
            }
            JavaType.Variable field = null;
            if (key instanceof J.FieldAccess) {
                field = ((J.FieldAccess) key).getName().getFieldType();
            } else if (key instanceof J.Identifier) {
                field = ((J.Identifier) key).getFieldType();
            }
            if (field != null && TypeUtils.isOfClassType(field.getOwner(), RESOURCE_CONFIG)) {
                for (Phase phase : Phase.values()) {
                    if (phase.constant.equals(field.getName())) {
                        return phase;
                    }
                }
            }
            return null;
        }

        /**
         * The type that {@code this} has where the cursor is, for a method invoked without a receiver.
         */
        private static @Nullable JavaType enclosingClassType(Cursor cursor) {
            for (Cursor c = cursor; c != null; c = c.getParent()) {
                Object value = c.getValue();
                if (value instanceof J.ClassDeclaration) {
                    return ((J.ClassDeclaration) value).getType();
                }
                if (value instanceof J.NewClass && ((J.NewClass) value).getBody() != null) {
                    return ((J.NewClass) value).getType();
                }
            }
            return null;
        }
    }

    /**
     * The JAX-RS 2 types the converted filters refer to, as a parser attributes them.
     */
    static final class JaxRsTypes {
        final JavaType.FullyQualified requestFilter;
        final JavaType.FullyQualified responseFilter;
        final JavaType.FullyQualified requestContext;
        final JavaType.FullyQualified responseContext;
        final JavaType.Method requestFilterMethod;
        final JavaType.Method responseFilterMethod;
        final JavaType.Method headersGet;

        private JaxRsTypes(J.ClassDeclaration stub) {
            JavaType.Class stubType = (JavaType.Class) Objects.requireNonNull(stub.getType());
            requestFilter = stubType.getInterfaces().get(0);
            responseFilter = stubType.getInterfaces().get(1);
            J.MethodDeclaration request = (J.MethodDeclaration) stub.getBody().getStatements().get(0);
            J.MethodDeclaration response = (J.MethodDeclaration) stub.getBody().getStatements().get(1);
            requestFilterMethod = Objects.requireNonNull(request.getMethodType());
            responseFilterMethod = Objects.requireNonNull(response.getMethodType());
            requestContext = Objects.requireNonNull(TypeUtils.asFullyQualified(requestFilterMethod.getParameterTypes().get(0)));
            responseContext = Objects.requireNonNull(TypeUtils.asFullyQualified(responseFilterMethod.getParameterTypes().get(1)));
            J.MethodInvocation get = (J.MethodInvocation) Objects.requireNonNull(request.getBody()).getStatements().get(0);
            headersGet = Objects.requireNonNull(get.getMethodType());
        }

        static JaxRsTypes get(ExecutionContext ctx) {
            return ctx.computeMessageIfAbsent(JaxRsTypes.class.getName(), k -> {
                SourceFile stub = parser(ctx).build().parse(ctx,
                        "import javax.ws.rs.container.*;\n" +
                        "class Stub implements ContainerRequestFilter, ContainerResponseFilter {\n" +
                        "    public void filter(ContainerRequestContext requestContext) {\n" +
                        "        requestContext.getHeaders().get(\"\");\n" +
                        "    }\n" +
                        "    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {\n" +
                        "    }\n" +
                        "}").findFirst().orElseThrow(() -> new IllegalStateException("Could not parse JAX-RS 2 types"));
                return new JaxRsTypes(((J.CompilationUnit) stub).getClasses().get(0));
            });
        }

        static JavaParser.Builder<?, ?> parser(ExecutionContext ctx) {
            return JavaParser.fromJavaVersion()
                    .classpathFromResources(ctx, "javax.ws.rs-api-2", "jersey-common-2", "jersey-server-2");
        }

        JavaType.FullyQualified context(Phase parameter) {
            return parameter == Phase.REQUEST ? requestContext : responseContext;
        }

        JavaType.FullyQualified filterInterface(Phase phase) {
            return phase == Phase.REQUEST ? requestFilter : responseFilter;
        }

        JavaType.Method filterMethod(Phase phase) {
            return phase == Phase.REQUEST ? requestFilterMethod : responseFilterMethod;
        }

        static JavaType.@Nullable Method method(@Nullable JavaType owner, String name, int arity) {
            JavaType.FullyQualified type = TypeUtils.asFullyQualified(owner);
            if (type != null) {
                for (JavaType.Method method : type.getMethods()) {
                    if (name.equals(method.getName()) && method.getParameterTypes().size() == arity) {
                        return method;
                    }
                }
            }
            return null;
        }
    }
}
