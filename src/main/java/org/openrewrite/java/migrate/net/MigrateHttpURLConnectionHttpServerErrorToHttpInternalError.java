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
package org.openrewrite.java.migrate.net;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeFieldName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.Set;

public class MigrateHttpURLConnectionHttpServerErrorToHttpInternalError extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `java.net.HttpURLConnection.HTTP_INTERNAL_ERROR`";
    }

    @Override
    public String getDescription() {
        return "Use `java.net.HttpURLConnection.HTTP_INTERNAL_ERROR` instead of the deprecated `java.net.HttpURLConnection.HTTP_SERVER_ERROR`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.net.HttpURLConnection", false), new MigrateHttpURLConnectionHttpServerErrorToHttpInternalErrorVisitor());
    }

    private static class MigrateHttpURLConnectionHttpServerErrorToHttpInternalErrorVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            doAfterVisit(new ChangeFieldName<>("java.net.HttpURLConnection", "HTTP_SERVER_ERROR", "HTTP_INTERNAL_ERROR"));
            return super.visitCompilationUnit(cu, ctx);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            if ("HTTP_SERVER_ERROR".equals(identifier.getSimpleName())) {
                if (identifier.getFieldType() != null) {
                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(identifier.getFieldType().getOwner());
                    if (fq != null && "java.net.HttpURLConnection".equals(fq.getFullyQualifiedName())) {
                        identifier = identifier.withSimpleName("HTTP_INTERNAL_ERROR");
                    }
                }
            }

            return super.visitIdentifier(identifier, ctx);
        }

    }

}
