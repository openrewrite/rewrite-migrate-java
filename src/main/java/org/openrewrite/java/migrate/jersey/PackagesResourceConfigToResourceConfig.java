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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class PackagesResourceConfigToResourceConfig extends Recipe {

    private static final String PACKAGES_RESOURCE_CONFIG = "com.sun.jersey.api.core.PackagesResourceConfig";
    private static final String RESOURCE_CONFIG = "org.glassfish.jersey.server.ResourceConfig";

    String displayName = "Replace Jersey 1 `PackagesResourceConfig` with `ResourceConfig.packages(..)`";

    String description = "Replaces `new PackagesResourceConfig(\"a\", \"b\")` with " +
            "`new ResourceConfig().packages(\"a\", \"b\")` of Jersey 2, and a class that extends " +
            "`PackagesResourceConfig` and passes package names to `super(..)` in each of its constructors with one " +
            "that extends Jersey 2's `ResourceConfig` and calls `packages(..)` with the same arguments. Both versions " +
            "scan the packages and their subpackages for root resource and provider classes. Jersey 2 also splits each " +
            "argument at spaces, commas and semicolons, where Jersey 1 only did so for the packages property. The " +
            "constructor that takes a property map, anonymous subclasses, and subclasses that rely on the implicit " +
            "no-argument constructor are left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(PACKAGES_RESOURCE_CONFIG, false), new JavaVisitor<ExecutionContext>() {

            /**
             * Classes that extend {@code PackagesResourceConfig} and are migrated, by id.
             */
            private final Set<UUID> migratedClasses = new HashSet<>();

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                boolean migrate = isMigratableSubclass(classDecl);
                if (migrate) {
                    migratedClasses.add(classDecl.getId());
                }
                J j = super.visitClassDeclaration(classDecl, ctx);
                if (!migrate || !(j instanceof J.ClassDeclaration)) {
                    return j;
                }
                J.ClassDeclaration c = (J.ClassDeclaration) j;
                c = JavaTemplate.builder("ResourceConfig")
                        .imports(RESOURCE_CONFIG)
                        .javaParser(parser(ctx))
                        .build()
                        .apply(updateCursor(c), c.getCoordinates().replaceExtendsClause());
                if (c.getType() instanceof JavaType.Class && c.getExtends() != null &&
                        c.getExtends().getType() instanceof JavaType.FullyQualified) {
                    c = c.withType(((JavaType.Class) c.getType())
                            .withSupertype((JavaType.FullyQualified) c.getExtends().getType()));
                }
                maybeAddImport(RESOURCE_CONFIG);
                maybeRemoveImport(PACKAGES_RESOURCE_CONFIG);
                return c;
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J j = super.visitNewClass(newClass, ctx);
                if (!(j instanceof J.NewClass)) {
                    return j;
                }
                J.NewClass n = (J.NewClass) j;
                if (n.getBody() != null || !isPackagesConstructor(n.getConstructorType())) {
                    return n;
                }
                J.MethodInvocation packages = JavaTemplate.builder("new ResourceConfig().packages()")
                        .imports(RESOURCE_CONFIG)
                        .javaParser(parser(ctx))
                        .build()
                        .apply(getCursor(), n.getCoordinates().replace());
                maybeAddImport(RESOURCE_CONFIG);
                maybeRemoveImport(PACKAGES_RESOURCE_CONFIG);
                return packages.withArguments(n.getArguments());
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (!"super".equals(m.getSimpleName()) || !isPackagesConstructor(m.getMethodType())) {
                    return m;
                }
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null || !migratedClasses.contains(enclosing.getId())) {
                    return m;
                }
                J.Identifier self = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "this",
                        enclosing.getType(), null);
                J.MethodInvocation packages = JavaTemplate.builder("#{any(" + RESOURCE_CONFIG + ")}.packages()")
                        .javaParser(parser(ctx))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), self);
                return packages.withSelect(null).withArguments(m.getArguments());
            }
        });
    }

    private static JavaParser.Builder<?, ?> parser(ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().classpathFromResources(ctx,
                "javax.ws.rs-api-2", "jersey-common-2", "jersey-server-2");
    }

    /**
     * Whether a method type is {@code PackagesResourceConfig(String...)}, not the constructor that takes a map.
     */
    private static boolean isPackagesConstructor(JavaType.@Nullable Method constructor) {
        if (constructor == null || !"<constructor>".equals(constructor.getName()) ||
                !TypeUtils.isOfClassType(constructor.getDeclaringType(), PACKAGES_RESOURCE_CONFIG)) {
            return false;
        }
        List<JavaType> parameters = constructor.getParameterTypes();
        return parameters.size() == 1 && parameters.get(0) instanceof JavaType.Array &&
                TypeUtils.isString(((JavaType.Array) parameters.get(0)).getElemType());
    }

    /**
     * A class that extends {@code PackagesResourceConfig} directly can extend Jersey 2's {@code ResourceConfig} when
     * it declares constructors and each one starts by passing package names to {@code super(..)} or by delegating to
     * another constructor with {@code this(..)}.
     */
    private static boolean isMigratableSubclass(J.ClassDeclaration classDecl) {
        if (classDecl.getExtends() == null ||
                !TypeUtils.isOfClassType(classDecl.getExtends().getType(), PACKAGES_RESOURCE_CONFIG)) {
            return false;
        }
        boolean callsSuper = false;
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (!(statement instanceof J.MethodDeclaration) || !((J.MethodDeclaration) statement).isConstructor()) {
                continue;
            }
            J.MethodDeclaration constructor = (J.MethodDeclaration) statement;
            if (constructor.getBody() == null || constructor.getBody().getStatements().isEmpty() ||
                    !(constructor.getBody().getStatements().get(0) instanceof J.MethodInvocation)) {
                return false;
            }
            J.MethodInvocation first = (J.MethodInvocation) constructor.getBody().getStatements().get(0);
            if ("super".equals(first.getSimpleName()) && isPackagesConstructor(first.getMethodType())) {
                callsSuper = true;
            } else if (!"this".equals(first.getSimpleName())) {
                return false;
            }
        }
        if (!callsSuper) {
            return false;
        }
        // super.method() calls would now reach Jersey 2's ResourceConfig, which has none of Jersey 1's methods
        return new JavaIsoVisitor<Set<Boolean>>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<Boolean> found) {
                if (fieldAccess.getTarget() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName())) {
                    found.add(true);
                }
                return super.visitFieldAccess(fieldAccess, found);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<Boolean> found) {
                if (method.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) method.getSelect()).getSimpleName())) {
                    found.add(true);
                }
                return super.visitMethodInvocation(method, found);
            }
        }.reduce(classDecl.getBody(), new HashSet<>()).isEmpty();
    }
}
