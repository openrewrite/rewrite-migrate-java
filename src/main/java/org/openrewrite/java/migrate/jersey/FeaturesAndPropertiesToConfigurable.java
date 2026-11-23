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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class FeaturesAndPropertiesToConfigurable extends Recipe {

    private static final String JERSEY1_RESOURCE_CONFIG = "com.sun.jersey.api.core.ResourceConfig";
    private static final String JERSEY1_CLIENT_CONFIG = "com.sun.jersey.api.client.config.ClientConfig";
    private static final String JERSEY1_CLIENT = "com.sun.jersey.api.client.Client";
    private static final String JSON_CONFIGURATION = "com.sun.jersey.api.json.JSONConfiguration";
    private static final String POJO_MAPPING_FEATURE = "com.sun.jersey.api.json.POJOMappingFeature";
    private static final String JACKSON_PROVIDER = "org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider";

    String displayName = "Register Jersey 1 classes, singletons, properties and features through `Configurable`";

    String description = "Jersey 2 returns read-only views from `getClasses()`, `getInstances()` and `getProperties()` " +
            "of `ResourceConfig` and `ClientConfig`, so a Jersey 1 statement that adds to the collections of a " +
            "`ResourceConfig`, a `ClientConfig` or the properties of a `Client` now calls `register(..)`, " +
            "`property(..)`, `registerClasses(..)`, `registerInstances(..)` or `addProperties(..)` instead, on an " +
            "explicit receiver or inside a subclass. Jersey 2 has no separate feature map, so a feature becomes a " +
            "property, except that switching `JSONConfiguration.FEATURE_POJO_MAPPING` on registers Jackson 1's " +
            "`JacksonJaxbJsonProvider`, which is what Jersey 1 used for POJO mapping, and switching it off is " +
            "dropped, as it is off by default. Calls whose result is used, removals, and POJO mapping set from a " +
            "variable are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.sun.jersey..*", true), new JavaVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (!(m.getSelect() instanceof J.MethodInvocation) || !isStatement(getCursor())) {
                    return m;
                }
                J.MethodInvocation getter = (J.MethodInvocation) m.getSelect();
                if (!hasNoArguments(getter) || m.getMethodType() == null) {
                    return m;
                }

                Expression receiver = getter.getSelect();
                JavaType receiverType = receiver == null ? enclosingClassType(getCursor()) : receiver.getType();
                Kind kind = Kind.of(receiverType);
                if (kind == null) {
                    return m;
                }

                String getterName = getter.getSimpleName();
                String operation = m.getSimpleName();
                List<Expression> args = m.getArguments();
                boolean collection = TypeUtils.isAssignableTo("java.util.Collection", m.getMethodType().getDeclaringType());
                boolean map = TypeUtils.isAssignableTo("java.util.Map", m.getMethodType().getDeclaringType());

                if (collection && args.size() == 1 && kind != Kind.CLIENT) {
                    if ("getClasses".equals(getterName) && "add".equals(operation)) {
                        return replace(m, receiver, receiverType, kind, "register(#{any(java.lang.Class)})", ctx, args.get(0));
                    }
                    if ("getSingletons".equals(getterName) && "add".equals(operation)) {
                        return replace(m, receiver, receiverType, kind, "register(#{any(java.lang.Object)})", ctx, args.get(0));
                    }
                    if (kind == Kind.SERVER && "getClasses".equals(getterName) && "addAll".equals(operation) &&
                            isParameterizedWith(args.get(0).getType(), "java.util.Set", true, "java.lang.Class")) {
                        return replace(m, receiver, receiverType, kind,
                                "registerClasses(#{any(java.util.Set<java.lang.Class<?>>)})", ctx, args.get(0));
                    }
                    if (kind == Kind.SERVER && "getSingletons".equals(getterName) && "addAll".equals(operation) &&
                            isParameterizedWith(args.get(0).getType(), "java.util.Set", false, "java.lang.Object")) {
                        return replace(m, receiver, receiverType, kind,
                                "registerInstances(#{any(java.util.Set<java.lang.Object>)})", ctx, args.get(0));
                    }
                }

                if (map && "put".equals(operation) && args.size() == 2 &&
                        ("getProperties".equals(getterName) || ("getFeatures".equals(getterName) && kind != Kind.CLIENT))) {
                    Expression key = args.get(0);
                    Expression value = args.get(1);
                    if (isPojoMappingFeature(key)) {
                        Boolean enabled = booleanValue(value);
                        if (enabled == null) {
                            return m;
                        }
                        if (!enabled) {
                            if (!(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                                return m;
                            }
                            maybeRemovePojoMappingImports();
                            return null;
                        }
                        maybeRemovePojoMappingImports();
                        maybeAddImport(JACKSON_PROVIDER);
                        return replace(m, receiver, receiverType, kind, "register(JacksonJaxbJsonProvider.class)", ctx);
                    }
                    return replace(m, receiver, receiverType, kind,
                            "property(#{any(java.lang.String)}, #{any(java.lang.Object)})", ctx, key, value);
                }

                if (map && kind == Kind.SERVER && "getProperties".equals(getterName) && "putAll".equals(operation) &&
                        args.size() == 1 &&
                        isParameterizedWith(args.get(0).getType(), "java.util.Map", false, "java.lang.String", "java.lang.Object")) {
                    return replace(m, receiver, receiverType, kind,
                            "addProperties(#{any(java.util.Map<java.lang.String, java.lang.Object>)})", ctx, args.get(0));
                }
                return m;
            }

            private void maybeRemovePojoMappingImports() {
                maybeRemoveImport(JSON_CONFIGURATION);
                maybeRemoveImport(JSON_CONFIGURATION + ".FEATURE_POJO_MAPPING");
            }

            private J.MethodInvocation replace(J.MethodInvocation m, @Nullable Expression receiver,
                                               @Nullable JavaType receiverType, Kind kind, String call,
                                               ExecutionContext ctx, Expression... args) {
                Expression target = receiver;
                if (target == null) {
                    target = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "this",
                            receiverType, null);
                }
                Object[] parameters = new Object[args.length + 1];
                parameters[0] = target;
                System.arraycopy(args, 0, parameters, 1, args.length);
                J.MethodInvocation replaced = JavaTemplate.builder("#{any(" + kind.jersey2Type + ")}." + call)
                        .imports(JACKSON_PROVIDER)
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx,
                                "javax.ws.rs-api-2", "jersey-common-2", "jersey-client-2", "jersey-server-2",
                                "jackson-jaxrs-1"))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters);
                return receiver == null ? replaced.withSelect(null) : replaced;
            }
        });
    }

    private enum Kind {
        SERVER("org.glassfish.jersey.server.ResourceConfig"),
        CLIENT_CONFIG("org.glassfish.jersey.client.ClientConfig"),
        CLIENT("javax.ws.rs.client.Client");

        final String jersey2Type;

        Kind(String jersey2Type) {
            this.jersey2Type = jersey2Type;
        }

        /**
         * Also takes the Jersey 2 types, as another recipe may already have moved a subclass to them.
         */
        static @Nullable Kind of(@Nullable JavaType receiverType) {
            if (TypeUtils.isAssignableTo(JERSEY1_RESOURCE_CONFIG, receiverType) ||
                    TypeUtils.isAssignableTo(SERVER.jersey2Type, receiverType)) {
                return SERVER;
            }
            if (TypeUtils.isAssignableTo(JERSEY1_CLIENT_CONFIG, receiverType) ||
                    TypeUtils.isAssignableTo(CLIENT_CONFIG.jersey2Type, receiverType)) {
                return CLIENT_CONFIG;
            }
            if (TypeUtils.isAssignableTo(JERSEY1_CLIENT, receiverType)) {
                return CLIENT;
            }
            return null;
        }
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

    private static boolean hasNoArguments(J.MethodInvocation method) {
        return method.getArguments().isEmpty() ||
                (method.getArguments().size() == 1 && method.getArguments().get(0) instanceof J.Empty);
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

    /**
     * Whether {@code type} is a {@code container} with exactly the given type arguments, where
     * {@code wildcardArgument} asks for the first one to be {@code Class<?>}.
     */
    private static boolean isParameterizedWith(@Nullable JavaType type, String container, boolean wildcardArgument,
                                               String... typeArguments) {
        if (!TypeUtils.isAssignableTo(container, type) || !(type instanceof JavaType.Parameterized)) {
            return false;
        }
        List<JavaType> parameters = ((JavaType.Parameterized) type).getTypeParameters();
        if (parameters.size() != typeArguments.length) {
            return false;
        }
        for (int i = 0; i < typeArguments.length; i++) {
            JavaType parameter = parameters.get(i);
            if (!TypeUtils.isOfClassType(parameter, typeArguments[i])) {
                return false;
            }
            if (wildcardArgument && i == 0 && !isUnboundedWildcardParameterized(parameter)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUnboundedWildcardParameterized(JavaType type) {
        if (!(type instanceof JavaType.Parameterized)) {
            return false;
        }
        List<JavaType> parameters = ((JavaType.Parameterized) type).getTypeParameters();
        if (parameters.size() != 1 || !(parameters.get(0) instanceof JavaType.GenericTypeVariable)) {
            return false;
        }
        JavaType.GenericTypeVariable wildcard = (JavaType.GenericTypeVariable) parameters.get(0);
        return "?".equals(wildcard.getName()) && wildcard.getBounds().isEmpty();
    }

    private static boolean isPojoMappingFeature(Expression key) {
        if (key instanceof J.Literal) {
            return POJO_MAPPING_FEATURE.equals(((J.Literal) key).getValue());
        }
        JavaType.Variable field = null;
        if (key instanceof J.FieldAccess) {
            field = ((J.FieldAccess) key).getName().getFieldType();
        } else if (key instanceof J.Identifier) {
            field = ((J.Identifier) key).getFieldType();
        }
        return field != null && "FEATURE_POJO_MAPPING".equals(field.getName()) &&
                TypeUtils.isOfClassType(field.getOwner(), JSON_CONFIGURATION);
    }

    /**
     * The value of a {@code true} or {@code false} literal, {@code Boolean.TRUE} or {@code Boolean.FALSE}, or a
     * {@code "true"} or {@code "false"} string, which Jersey 1 also read as a feature; {@code null} for anything else.
     */
    private static @Nullable Boolean booleanValue(Expression value) {
        if (value instanceof J.Literal) {
            Object literal = ((J.Literal) value).getValue();
            if (literal instanceof Boolean) {
                return (Boolean) literal;
            }
            if (literal instanceof String) {
                String s = ((String) literal).trim();
                if ("true".equalsIgnoreCase(s)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(s)) {
                    return false;
                }
            }
            return null;
        }
        JavaType.Variable field = null;
        if (value instanceof J.FieldAccess) {
            field = ((J.FieldAccess) value).getName().getFieldType();
        } else if (value instanceof J.Identifier) {
            field = ((J.Identifier) value).getFieldType();
        }
        if (field != null && TypeUtils.isOfClassType(field.getOwner(), "java.lang.Boolean")) {
            if ("TRUE".equals(field.getName())) {
                return true;
            }
            if ("FALSE".equals(field.getName())) {
                return false;
            }
        }
        return null;
    }
}
