/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang.var;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

public class UseVarForTypeCast extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `var` for variables initialized with type casts";
    }

    @Override
    public String getDescription() {
        return "Apply local variable type inference `var` to variables that are initialized by a cast expression " +
                "where the cast type matches the declared variable type. This removes the redundant type duplication. " +
                "For example, `String s = (String) obj;` becomes `var s = (String) obj;`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(10), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations, ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(variableDeclarations, ctx);
                if (usesVar(vd)) {
                    return vd;
                }

                J.TypeCast typeCast = getSingleTypeCastInitializer(vd);
                if (typeCast != null && typeCast.getType() != null &&
                        TypeUtils.isOfType(typeCast.getType(), vd.getType()) &&
                        isInsideMethod(getCursor())) {
                    return transformToVar(vd, typeCast);
                }

                return vd;
            }

            private boolean usesVar(J.VariableDeclarations vd) {
                TypeTree typeExpression = vd.getTypeExpression();
                return typeExpression instanceof J.Identifier &&
                        "var".equals(((J.Identifier) typeExpression).getSimpleName());
            }

            private J.@Nullable TypeCast getSingleTypeCastInitializer(J.VariableDeclarations vd) {
                if (vd.getVariables().size() != 1) {
                    return null;
                }
                Expression initializer = vd.getVariables().get(0).getInitializer();
                if (initializer != null) {
                    initializer = initializer.unwrap();
                    if (initializer instanceof J.TypeCast) {
                        return (J.TypeCast) initializer;
                    }
                }
                return null;
            }

            private boolean isInsideMethod(Cursor cursor) {
                return cursor.dropParentUntil(p -> p instanceof J.MethodDeclaration ||
                                p instanceof J.ClassDeclaration ||
                                Cursor.ROOT_VALUE.equals(p))
                        .getValue() instanceof J.MethodDeclaration;
            }

            private J.VariableDeclarations transformToVar(J.VariableDeclarations vd, J.TypeCast typeCast) {
                List<J.VariableDeclarations.NamedVariable> variables = ListUtils.mapFirst(vd.getVariables(), it -> {
                    JavaType.Variable variableType = it.getVariableType() == null ?
                            null : it.getVariableType().withOwner(null);
                    return it
                            .withName(it.getName().withType(typeCast.getType()).withFieldType(variableType))
                            .withVariableType(variableType);
                });
                J.Identifier typeExpression = new J.Identifier(
                        randomId(),
                        vd.getTypeExpression() == null ? EMPTY : vd.getTypeExpression().getPrefix(),
                        Markers.build(singleton(JavaVarKeyword.build())),
                        emptyList(),
                        "var",
                        typeCast.getType(),
                        null);
                return vd.withVariables(variables).withTypeExpression(typeExpression);
            }
        });
    }
}
