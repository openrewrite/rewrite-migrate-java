/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.SearchResult;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Collections.emptySet;
import static org.openrewrite.internal.StringUtils.uncapitalize;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDtoOverfetching extends Recipe {
    @Option(displayName = "DTO type",
            description = "The fully qualified name of the DTO.")
    String dtoType;

    @Override
    public String getDisplayName() {
        return "Find methods that only use one DTO data element";
    }

    @Override
    public String getDescription() {
        return "Find methods that have 'opportunities' for improvement.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dtoFields = new MethodMatcher(dtoType + " get*()");
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
                Set<String> allUses = getCursor().getMessage("dtoDataUses", emptySet());
                if (allUses.size() == 1) {
                    return SearchResult.found(m, String.join(", ", allUses));
                }
                return m;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (method.getSelect() instanceof J.Identifier && dtoFields.matches(method)) {
                    Iterator<Cursor> methodDeclarations = getCursor()
                            .getPathAsCursors(c -> c.getValue() instanceof J.MethodDeclaration);
                    if (methodDeclarations.hasNext()) {
                        Cursor methodCursor = methodDeclarations.next();
                        J.MethodDeclaration methodDeclaration = methodCursor.getValue();

                        outer:
                        for (Statement parameter : methodDeclaration.getParameters()) {
                            if (parameter instanceof J.VariableDeclarations) {
                                J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) parameter;
                                for (J.VariableDeclarations.NamedVariable variable : variableDeclarations.getVariables()) {
                                    if (variable.getName().getSimpleName().equals(((J.Identifier) method.getSelect()).getSimpleName())) {
                                        methodCursor.computeMessageIfAbsent("dtoDataUses", k -> new TreeSet<>())
                                                .add(uncapitalize(method.getSimpleName().replaceAll("^get", "")));
                                        break outer;
                                    }
                                }
                            }
                        }
                    }
                }
                return m;
            }
        };
    }
}
