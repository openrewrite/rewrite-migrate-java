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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaVarKeyword;

public class ExplicitRecordImport extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add explicit import for `Record` classes";
    }

    @Override
    public String getDescription() {
        return "Add explicit import for `Record` classes when upgrading past Java 14+, to avoid conflicts with `java.lang.Record`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("*..Record", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);
                        if (vd.getType() instanceof JavaType.FullyQualified) {
                            JavaType.FullyQualified type = (JavaType.FullyQualified) vd.getType();
                            if ("Record".equals(type.getClassName()) &&
                                    !type.getPackageName().startsWith("java.lang") &&
                                    (vd.getTypeExpression() == null || !vd.getTypeExpression().getMarkers().findFirst(JavaVarKeyword.class).isPresent())) {
                                maybeAddImport(type.getFullyQualifiedName());
                            }
                        }
                        return vd;
                    }
                }
        );
    }
}
