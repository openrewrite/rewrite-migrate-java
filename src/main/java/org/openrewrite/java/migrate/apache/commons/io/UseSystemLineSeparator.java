/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.apache.commons.io;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesField;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

public class UseSystemLineSeparator extends Recipe {

    @Override
    public String getDisplayName() {
        return "Use `System.lineSeparator()`";
    }

    @Override
    public String getDescription() {
        return "Migrate `IOUtils.LINE_SEPARATOR` to `System.lineSeparator()`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesField<>("org.apache.commons.io.IOUtils", "LINE_SEPARATOR");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (getCursor().firstEnclosing(J.Import.class) == null &&
                        TypeUtils.isOfClassType(fieldAccess.getTarget().getType(), "org.apache.commons.io.IOUtils") &&
                        fieldAccess.getSimpleName().equals("LINE_SEPARATOR")) {
                    return useSystemLineSeparator(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }

            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext executionContext) {
                JavaType.Variable varType = TypeUtils.asVariable(ident.getFieldType());
                if (varType != null &&
                        TypeUtils.isOfClassType(varType.getOwner(), "org.apache.commons.io.IOUtils") &&
                        varType.getName().equals("LINE_SEPARATOR")) {
                    return useSystemLineSeparator(ident);
                }
                return ident;
            }

            @NotNull
            private J useSystemLineSeparator(J j) {
                maybeRemoveImport("org.apache.commons.io.IOUtils");

                Cursor statementCursor = getCursor().dropParentUntil(Statement.class::isInstance);
                Statement statement = statementCursor.getValue();
                JavaTemplate template = JavaTemplate
                        .builder(() -> statementCursor, "System.lineSeparator()")
                        .build();

                return statement.withTemplate(template, statement.getCoordinates().replace())
                        .withPrefix(j.getPrefix());
            }
        };
    }
}
