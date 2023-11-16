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
package org.openrewrite.java.migrate.jakarta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class UpdateAddAnnotatedType extends Recipe {

    @Option(displayName = "Method Pattern", description = "A method pattern for matching required method definition.", example = "jakarta.enterprise.inject.spi.BeforeBeanDiscovery addAnnotatedType(jakarta.enterprise.inject.spi.AnnotatedType)")
    @NonNull String methodPattern;
    String templateStringJakarta = "jakarta.enterprise.inject.spi.AnnotatedType";
    String templateStringJavax = "javax.enterprise.inject.spi.AnnotatedType";
    MethodMatcher methodInputPattern = null;

    @JsonCreator
    public UpdateAddAnnotatedType(@NonNull @JsonProperty("methodPattern") String methodPattern) {
        this.methodPattern = methodPattern;
        methodInputPattern = new MethodMatcher(methodPattern, false);
    }

    @Override
    public String getDisplayName() {
        return "Replace `addAnnotatedType(AnnotatedType)` with `addAnnotatedType(AnnotatedType,String)`";
    }

    @Override
    public String getDescription() {
        return "`BeforeBeanDiscovery.addAnnotatedType(AnnotatedType)` is Deprecated in CDI 1.1. It is Replaced by `BeforeBeanDiscovery.addAnnotatedType(AnnotatedType, String)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String templateBuilderString = null;
        if (methodPattern.contains("jakarta.enterprise.inject.spi.")) {
            templateBuilderString = templateStringJakarta;
        } else if (methodPattern.contains("javax.enterprise.inject.spi.")) {
            templateBuilderString = templateStringJavax;
        }
        String finalTemplateBuilderString = templateBuilderString;
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (methodInputPattern.matches(method)) {
                    String tempString = "#{any(" + finalTemplateBuilderString + ")},null\"";
                    return JavaTemplate.builder(tempString).
                            build().apply(updateCursor(method),
                                    method.getCoordinates().
                                            replaceArguments(),
                                    method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
