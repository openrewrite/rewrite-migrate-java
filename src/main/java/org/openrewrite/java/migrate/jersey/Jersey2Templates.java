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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

/**
 * What the recipes that move Jersey 1 client code to Jersey 2 share: templates compiled against Jersey 2 and
 * JAX-RS 2.1, and a check for whether a type can be imported by its simple name.
 */
final class Jersey2Templates {

    static final MethodMatcher ADD_FILTER = new MethodMatcher(
            "com.sun.jersey.api.client.filter.Filterable addFilter(com.sun.jersey.api.client.filter.ClientFilter)", true);

    private Jersey2Templates() {
    }

    /**
     * @return the JAX-RS 2 type that a Jersey 1 {@code Client} or {@code WebResource} becomes, for the receiver of
     * an {@code addFilter(..)} call, or {@code null} for any other {@code Filterable}, such as the Apache HTTP client,
     * an {@code AsyncWebResource} or a subclass of {@code Client}, which a {@code register(..)} would not compile on.
     */
    static @Nullable String registrationTarget(J.MethodInvocation addFilter) {
        if (!ADD_FILTER.matches(addFilter) || addFilter.getSelect() == null) {
            return null;
        }
        JavaType type = addFilter.getSelect().getType();
        if (TypeUtils.isOfClassType(type, "com.sun.jersey.api.client.Client")) {
            return "javax.ws.rs.client.Client";
        }
        if (TypeUtils.isOfClassType(type, "com.sun.jersey.api.client.WebResource")) {
            return "javax.ws.rs.client.WebTarget";
        }
        return null;
    }

    static JavaParser.Builder<?, ?> parser(ExecutionContext ctx) {
        return JavaParser.fromJavaVersion()
                .classpathFromResources(ctx, "jersey-client-2", "jersey-common-2", "javax.ws.rs-api-2");
    }

    static JavaTemplate template(String code, ExecutionContext ctx, String... imports) {
        return JavaTemplate.builder(code)
                .imports(imports)
                .javaParser(parser(ctx))
                .build();
    }

    /**
     * @return {@code true} when no other type of the same simple name is imported, declared or used in the file, so
     * that the type can be imported and referred to by its simple name.
     */
    static boolean canImport(JavaSourceFile sourceFile, String fullyQualifiedName, String... replacedTypes) {
        String simpleName = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf('.') + 1);
        for (J.Import anImport : sourceFile.getImports()) {
            if (!anImport.isStatic() && simpleName.equals(anImport.getClassName()) &&
                !fullyQualifiedName.equals(anImport.getTypeName()) && !isReplaced(anImport.getTypeName(), replacedTypes)) {
                return false;
            }
        }
        for (J.ClassDeclaration classDeclaration : sourceFile.getClasses()) {
            if (simpleName.equals(classDeclaration.getSimpleName())) {
                return false;
            }
        }
        for (JavaType type : sourceFile.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if (fq != null && simpleName.equals(fq.getClassName()) &&
                !fullyQualifiedName.equals(fq.getFullyQualifiedName()) &&
                !isReplaced(fq.getFullyQualifiedName(), replacedTypes)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isReplaced(String fullyQualifiedName, String[] replacedTypes) {
        for (String replaced : replacedTypes) {
            if (replaced.equals(fullyQualifiedName)) {
                return true;
            }
        }
        return false;
    }
}
