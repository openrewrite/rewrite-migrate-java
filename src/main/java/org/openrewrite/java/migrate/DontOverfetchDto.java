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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static org.openrewrite.internal.StringUtils.uncapitalize;

@EqualsAndHashCode(callSuper = false)
@Value
public class DontOverfetchDto extends Recipe {

    @Option(displayName = "DTO type",
            description = "The fully qualified name of the DTO.",
            example = "animals.Dog")
    String dtoType;

    @Option(displayName = "Data element",
            description = "Replace the DTO as a method parameter when only this data element is used.",
            example = "name")
    String dtoDataElement;

    String displayName = "Replace DTO method parameters with data elements";

    String description = "Replace method parameters that have DTOs with their " +
               "data elements when only the specified data element is used.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dtoFields = new MethodMatcher(dtoType + " *(..)");
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                for (Entry<String, Set<String>> usesForArgument : getCursor().getMessage("dtoDataUses",
                        Collections.<String, Set<String>>emptyMap()).entrySet()) {
                    String dtoVariableName = usesForArgument.getKey();

                    Set<String> allUses = usesForArgument.getValue();
                    if (allUses.size() == 1 && allUses.iterator().next().equals(dtoDataElement)) {
                        AtomicReference<JavaType.FullyQualified> memberTypeAtomic = new AtomicReference<>();

                        m = m.withParameters(ListUtils.map(m.getParameters(), p -> {
                            if (p instanceof J.VariableDeclarations) {
                                J.VariableDeclarations v = (J.VariableDeclarations) p;
                                if (v.getVariables().get(0).getSimpleName().equals(dtoVariableName)) {
                                    JavaType.FullyQualified dtoType = v.getTypeAsFullyQualified();
                                    if (dtoType != null) {
                                        for (JavaType.Variable member : dtoType.getMembers()) {
                                            if (member.getName().equals(dtoDataElement)) {
                                                JavaType.FullyQualified memberType = TypeUtils.asFullyQualified(member.getType());
                                                memberTypeAtomic.set(memberType);
                                                if (memberType != null) {
                                                    maybeRemoveImport(dtoType);
                                                    maybeAddImport(memberType);
                                                    return v
                                                            .withType(memberType)
                                                            .withTypeExpression(TypeTree.build(memberType.getFullyQualifiedName()))
                                                            .withVariables(ListUtils.map(v.getVariables(), nv -> {
                                                                JavaType.Variable fieldType = nv.getName().getFieldType();
                                                                return nv
                                                                        .withName(nv.getName().withSimpleName(dtoDataElement).withType(memberType))
                                                                        .withType(memberType)
                                                                        .withVariableType(fieldType
                                                                                .withName(dtoDataElement).withOwner(memberType));
                                                            }));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            return p;
                        }));

                        m = (J.MethodDeclaration) new ReplaceWithDtoElement(dtoVariableName, memberTypeAtomic.get()).visitNonNull(m, ctx,
                                getCursor().getParentOrThrow());
                    }
                }
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (dtoFields.matches(method)) {
                    Iterator<Cursor> methodDeclarations = getCursor()
                            .getPathAsCursors(c -> c.getValue() instanceof J.MethodDeclaration);
                    if (methodDeclarations.hasNext() && method.getSelect() instanceof J.Identifier) {
                        String argumentName = ((J.Identifier) method.getSelect()).getSimpleName();
                        methodDeclarations.next().computeMessageIfAbsent("dtoDataUses", k -> new HashMap<String, Set<String>>())
                                .computeIfAbsent(argumentName, n -> new HashSet<>())
                                .add(uncapitalize(method.getSimpleName().replaceAll("^get", "")));
                    }
                }
                return m;
            }
        };
    }

    @RequiredArgsConstructor
    private class ReplaceWithDtoElement extends JavaVisitor<ExecutionContext> {
        private final String dtoVariableName;
        private final JavaType.FullyQualified memberType;

        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            if (method.getSelect() instanceof J.Identifier && ((J.Identifier) method.getSelect()).getSimpleName()
                    .equals(dtoVariableName)) {
                return new J.Identifier(Tree.randomId(), method.getPrefix(),
                        Markers.EMPTY, emptyList(), dtoDataElement, memberType, null);
            }
            return super.visitMethodInvocation(method, ctx);
        }
    }
}
