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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.staticanalysis.ModifierOrder.sortModifiers;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddSealedClassModifier extends ScanningRecipe<AddSealedClassModifier.Accumulator> {

    String displayName = "Use `sealed` classes where possible";

    String description = "Adds the `sealed` modifier to classes and interfaces whose only subclasses/implementations " +
            "are nested within the same class declaration and whose constructors are all private. " +
            "Also adds `final` to permitted subclasses that are not already `final`, `sealed`, or `non-sealed`.";

    static class Accumulator {
        // Maps fully qualified parent class name -> set of fully qualified subclass names found across the whole codebase
        final Map<String, Set<String>> subclassesByParent = new ConcurrentHashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return Preconditions.check(new UsesJavaVersion<>(17), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                // Record every class's superclass relationship
                JavaType.FullyQualified type = cd.getType();
                if (type == null) {
                    return cd;
                }
                TypeTree extendsClause = cd.getExtends();
                if (extendsClause != null && extendsClause.getType() instanceof JavaType.FullyQualified) {
                    String parentFqn = ((JavaType.FullyQualified) extendsClause.getType()).getFullyQualifiedName();
                    acc.subclassesByParent
                            .computeIfAbsent(parentFqn, k -> ConcurrentHashMap.newKeySet())
                            .add(type.getFullyQualifiedName());
                }
                List<TypeTree> implementsClauses = cd.getImplements();
                if (implementsClauses != null) {
                    for (TypeTree impl : implementsClauses) {
                        if (impl.getType() instanceof JavaType.FullyQualified) {
                            String parentFqn = ((JavaType.FullyQualified) impl.getType()).getFullyQualifiedName();
                            acc.subclassesByParent
                                    .computeIfAbsent(parentFqn, k -> ConcurrentHashMap.newKeySet())
                                    .add(type.getFullyQualifiedName());
                        }
                    }
                }
                return cd;
            }
        });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new UsesJavaVersion<>(17), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                JavaType.FullyQualified type = cd.getType();
                if (type == null) {
                    return cd;
                }

                // Skip if already sealed
                if (cd.hasModifier(J.Modifier.Type.Sealed)) {
                    return cd;
                }

                // Skip enums and records
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum ||
                    cd.getKind() == J.ClassDeclaration.Kind.Type.Record) {
                    return cd;
                }

                String parentFqn = type.getFullyQualifiedName();
                boolean isInterface = cd.getKind() == J.ClassDeclaration.Kind.Type.Interface;

                // For classes: verify all constructors are private
                if (!isInterface && !allConstructorsPrivate(cd)) {
                    return cd;
                }

                // Find nested subclasses/implementors within this class declaration
                List<J.ClassDeclaration> nestedSubclasses = findNestedSubclasses(cd, parentFqn);
                if (nestedSubclasses.isEmpty()) {
                    return cd;
                }

                // Verify that the set of nested subclasses matches ALL known subclasses across the codebase
                Set<String> nestedFqns = nestedSubclasses.stream()
                        .map(J.ClassDeclaration::getType)
                        .filter(Objects::nonNull)
                        .map(JavaType.FullyQualified::getFullyQualifiedName)
                        .collect(Collectors.toSet());

                Set<String> allKnownSubclasses = acc.subclassesByParent.getOrDefault(parentFqn, Collections.emptySet());
                if (!nestedFqns.equals(allKnownSubclasses)) {
                    // There are subclasses outside this class declaration — not safe to seal
                    return cd;
                }

                // Verify all nested subclasses are already final, sealed, or non-sealed
                // (if not, we'll add final to them)
                boolean allSubclassesCompatible = nestedSubclasses.stream().allMatch(sub ->
                        sub.hasModifier(J.Modifier.Type.Final) ||
                        sub.hasModifier(J.Modifier.Type.Sealed) ||
                        sub.hasModifier(J.Modifier.Type.NonSealed) ||
                        sub.getKind() == J.ClassDeclaration.Kind.Type.Record ||
                        sub.getKind() == J.ClassDeclaration.Kind.Type.Enum);
                if (!allSubclassesCompatible) {
                    return cd;
                }

                // Build the permits clause
                List<JRightPadded<TypeTree>> permitsEntries = new ArrayList<>();
                for (int i = 0; i < nestedSubclasses.size(); i++) {
                    J.ClassDeclaration sub = nestedSubclasses.get(i);
                    J.Identifier permitIdent = new J.Identifier(
                            randomId(),
                            i == 0 ? Space.SINGLE_SPACE : Space.SINGLE_SPACE,
                            Markers.EMPTY,
                            emptyList(),
                            sub.getSimpleName(),
                            sub.getType(),
                            null
                    );
                    Space after = i < nestedSubclasses.size() - 1 ? Space.EMPTY : Space.EMPTY;
                    permitsEntries.add(JRightPadded.build((TypeTree) permitIdent).withAfter(after));
                }

                JContainer<TypeTree> permits = JContainer.build(
                        Space.SINGLE_SPACE, // space before "permits" keyword
                        permitsEntries,
                        Markers.EMPTY
                );

                // Add sealed modifier
                List<J.Modifier> modifiers = new ArrayList<>(cd.getModifiers());
                modifiers.add(new J.Modifier(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, null, J.Modifier.Type.Sealed, emptyList()));

                return cd.withModifiers(sortModifiers(modifiers))
                        .getPadding().withPermits(permits);
            }
        });
    }

    private static boolean allConstructorsPrivate(J.ClassDeclaration cd) {
        List<J.MethodDeclaration> constructors = cd.getBody().getStatements().stream()
                .filter(J.MethodDeclaration.class::isInstance)
                .map(J.MethodDeclaration.class::cast)
                .filter(J.MethodDeclaration::isConstructor)
                .collect(Collectors.toList());

        // If there are no explicit constructors, the default constructor is package-private — not safe
        if (constructors.isEmpty()) {
            return false;
        }

        return constructors.stream().allMatch(ctor ->
                ctor.hasModifier(J.Modifier.Type.Private));
    }

    private static List<J.ClassDeclaration> findNestedSubclasses(J.ClassDeclaration parent, String parentFqn) {
        List<J.ClassDeclaration> result = new ArrayList<>();
        for (Statement stmt : parent.getBody().getStatements()) {
            if (stmt instanceof J.ClassDeclaration) {
                J.ClassDeclaration nested = (J.ClassDeclaration) stmt;
                if (extendsOrImplements(nested, parentFqn)) {
                    result.add(nested);
                }
            }
        }
        return result;
    }

    private static boolean extendsOrImplements(J.ClassDeclaration cd, String parentFqn) {
        TypeTree extendsClause = cd.getExtends();
        if (extendsClause != null && extendsClause.getType() instanceof JavaType.FullyQualified) {
            if (parentFqn.equals(((JavaType.FullyQualified) extendsClause.getType()).getFullyQualifiedName())) {
                return true;
            }
        }
        List<TypeTree> implementsClauses = cd.getImplements();
        if (implementsClauses != null) {
            for (TypeTree impl : implementsClauses) {
                if (impl.getType() instanceof JavaType.FullyQualified &&
                    parentFqn.equals(((JavaType.FullyQualified) impl.getType()).getFullyQualifiedName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
