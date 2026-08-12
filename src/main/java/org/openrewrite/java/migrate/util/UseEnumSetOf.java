/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.service.ImportService;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseEnumSetOf extends Recipe {
    private static final MethodMatcher SET_OF = new MethodMatcher("java.util.Set of(..)", true);
    private static final String METHOD_TYPE = "java.util.EnumSet";

    @Option(
            displayName = "Convert empty `Set.of()` to an unmodifiable `EnumSet.noneOf()`",
            description = "When true, converts `Set.of()` with no arguments to an unmodifiable `EnumSet.noneOf()`. Default true.",
            example = "true",
            required = false
    )
    @Nullable
    Boolean convertEmptySet;

    String displayName = "Prefer `EnumSet of(..)`";

    String description = "Prefer an unmodifiable `EnumSet` instead of using `Set.of(..)` when the arguments are enums in Java 9 or higher.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes( 2 );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesJavaVersion<>(9), new UsesMethod<>(SET_OF)), new JavaVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation methodInvocation, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, ctx);

                if (SET_OF.matches(mi) &&
                        mi.getType() instanceof JavaType.Parameterized &&
                        !TypeUtils.isOfClassType(mi.getType(), METHOD_TYPE) &&
                        convertEmptySet(mi)) {
                    Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.Assignment || is instanceof J.VariableDeclarations || is instanceof J.Block);
                    if (!(parent.getValue() instanceof J.Block)) {
                        JavaType type = parent.getValue() instanceof J.Assignment ?
                                ((J.Assignment) parent.getValue()).getType() : ((J.VariableDeclarations) parent.getValue()).getVariables().get(0).getType();
                        if (isAssignmentSetOfEnum(type)) {
                            boolean collectionsUnavailable = isNameUnavailable("Collections", "java.util.Collections");
                            boolean javaUnavailable = isNameUnavailable("java", null);
                            if (collectionsUnavailable && javaUnavailable) {
                                return mi;
                            }
                            String collections = javaUnavailable ? "Collections" : "java.util.Collections";
                            if (javaUnavailable) {
                                maybeAddImport("java.util.Collections");
                            }
                            maybeAddImport(METHOD_TYPE);

                            List<Expression> args = mi.getArguments();
                            if (isArrayParameter(args)) {
                                return mi;
                            }

                            if (args.get(0) instanceof J.Empty) {
                                if (isStaticField(parent)) {
                                    return mi;
                                }
                                JavaType firstTypeParameter = ((JavaType.Parameterized) type).getTypeParameters().get(0);
                                JavaType.ShallowClass shallowClass = JavaType.ShallowClass.build(firstTypeParameter.toString());
                                J.MethodInvocation replacement = JavaTemplate.builder(collections + ".unmodifiableSet(EnumSet.noneOf(" +
                                                shallowClass.getClassName() + ".class))")
                                        .contextSensitive()
                                        .imports("java.util.Collections", METHOD_TYPE)
                                        .build()
                                        .apply(updateCursor(mi), mi.getCoordinates().replace());
                                if (!collectionsUnavailable) {
                                    doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(
                                            requireNonNull(replacement.getSelect())));
                                }
                                return replacement;
                            }

                            StringJoiner setOf = new StringJoiner(", ", collections + ".unmodifiableSet(EnumSet.of(", "))");
                            args.forEach(o -> setOf.add("#{any()}"));
                            J.MethodInvocation replacement = JavaTemplate.builder(setOf.toString())
                                    .contextSensitive()
                                    .imports("java.util.Collections", METHOD_TYPE)
                                    .build()
                                    .apply(updateCursor(mi), mi.getCoordinates().replace(), args.toArray());
                            if (!collectionsUnavailable) {
                                doAfterVisit(service(ImportService.class).shortenFullyQualifiedTypeReferencesIn(
                                        requireNonNull(replacement.getSelect())));
                            }
                            return replacement;
                        }
                    }
                }
                return mi;
            }

            private boolean convertEmptySet(J.MethodInvocation mi) {
                if (convertEmptySet == null || convertEmptySet) {
                    return true;
                }
                return !mi.getArguments().isEmpty() && !(mi.getArguments().get(0) instanceof J.Empty);
            }

            private boolean isAssignmentSetOfEnum(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    if (TypeUtils.isOfClassType(parameterized.getType(), "java.util.Set")) {
                        return ((JavaType.Parameterized) type).getTypeParameters().stream()
                                .filter(JavaType.Class.class::isInstance)
                                .map(JavaType.Class.class::cast)
                                .anyMatch(o -> o.getKind() == JavaType.FullyQualified.Kind.Enum);
                    }
                }
                return false;
            }

            private boolean isStaticField(Cursor parent) {
                return parent.getValue() instanceof J.VariableDeclarations &&
                        ((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Static);
            }

            private boolean isArrayParameter(final List<Expression> args) {
                if (args.size() != 1) {
                    return false;
                }
                JavaType type = args.get(0).getType();
                return TypeUtils.asArray(type) != null;
            }

            private boolean isNameUnavailable(String name, @Nullable String allowedImport) {
                J.CompilationUnit compilationUnit = getCursor().firstEnclosingOrThrow(J.CompilationUnit.class);
                boolean conflictingImport = compilationUnit.getImports().stream()
                        .filter(anImport -> !anImport.isStatic())
                        .filter(anImport -> name.equals(anImport.getQualid().getSimpleName()))
                        .anyMatch(anImport -> allowedImport == null || !allowedImport.equals(anImport.getTypeName()));
                if (conflictingImport) {
                    return true;
                }

                AtomicBoolean typeDeclared = new AtomicBoolean();
                new JavaVisitor<AtomicBoolean>() {
                    @Override
                    public J visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                        if (name.equals(classDecl.getSimpleName())) {
                            found.set(true);
                            return classDecl;
                        }
                        return super.visitClassDeclaration(classDecl, found);
                    }

                    @Override
                    public J visitTypeParameter(J.TypeParameter typeParameter, AtomicBoolean found) {
                        if (typeParameter.getName() instanceof J.Identifier &&
                                name.equals(((J.Identifier) typeParameter.getName()).getSimpleName())) {
                            found.set(true);
                            return typeParameter;
                        }
                        return super.visitTypeParameter(typeParameter, found);
                    }
                }.visit(compilationUnit, typeDeclared);

                boolean visibleMember = false;
                Iterator<J.ClassDeclaration> enclosingClasses = getCursor().getPathAsStream()
                        .filter(J.ClassDeclaration.class::isInstance)
                        .map(J.ClassDeclaration.class::cast)
                        .iterator();
                while (enclosingClasses.hasNext() && !visibleMember) {
                    JavaType.FullyQualified classType = TypeUtils.asFullyQualified(enclosingClasses.next().getType());
                    if (classType != null) {
                        Iterator<JavaType.Variable> members = classType.getVisibleMembers();
                        while (members.hasNext()) {
                            if (name.equals(members.next().getName())) {
                                visibleMember = true;
                                break;
                            }
                        }
                    }
                }

                return typeDeclared.get() || visibleMember ||
                       VariableNameUtils.findNamesInScope(getCursor()).contains(name) ||
                       getCursor().getPathAsStream()
                               .filter(J.ClassDeclaration.class::isInstance)
                               .map(J.ClassDeclaration.class::cast)
                               .anyMatch(classDecl -> name.equals(classDecl.getSimpleName()));
            }
        });
    }

}
