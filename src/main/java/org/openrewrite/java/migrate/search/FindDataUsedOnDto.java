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
import org.openrewrite.java.migrate.table.DtoDataUses;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import static org.openrewrite.internal.StringUtils.uncapitalize;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindDataUsedOnDto extends Recipe {
    transient DtoDataUses dtoDataUses = new DtoDataUses(this);

    @Option(displayName = "DTO type",
            description = "The fully qualified name of the DTO.",
            example = "com.example.dto.*")
    String dtoType;

    @Override
    public String getDisplayName() {
        return "Find data used on DTOs";
    }

    @Override
    public String getDescription() {
        return "Find data elements used on DTOs. This is useful to provide " +
               "information where data over-fetching may be a problem.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dtoFields = new MethodMatcher(dtoType + " get*()");
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodDeclaration methodDeclaration = getCursor().firstEnclosing(J.MethodDeclaration.class);
                if (methodDeclaration != null && dtoFields.matches(method)) {
                    dtoDataUses.insertRow(ctx, new DtoDataUses.Row(
                            getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                            methodDeclaration.getSimpleName(),
                            uncapitalize(method.getSimpleName().replaceAll("^get", ""))
                    ));
                    return SearchResult.found(method);
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
