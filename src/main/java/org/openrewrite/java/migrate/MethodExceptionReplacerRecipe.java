/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Map;

public class MethodExceptionReplacerRecipe extends Recipe {
    private final Map<String, Map<String, String>> methodToExceptionMapping;

    public MethodExceptionReplacerRecipe(Map<String, Map<String, String>> methodToExceptionMapping) {
        this.methodToExceptionMapping = methodToExceptionMapping;
    }

    @Override
    public String getDisplayName() {
        return "Generic Recipe to Exception Replacement based on method signatures";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces specified exceptions with other exceptions based on method signatures.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Try visitTry(J.Try tryStatement, ExecutionContext ctx) {
                J.Try try_ = super.visitTry(tryStatement, ctx);

                for (Map.Entry<String, Map<String, String>> entry : methodToExceptionMapping.entrySet()) {
                    String methodPattern = entry.getKey();
                    Map<String, String> exceptionMapping = entry.getValue();

                    if (FindMethods.find(try_, methodPattern).isEmpty()) {
                        continue;
                    }

                    for (Map.Entry<String, String> exceptionEntry : exceptionMapping.entrySet()) {
                        String oldException = exceptionEntry.getKey();
                        String newException = exceptionEntry.getValue();
                        try_ = try_.withCatches(ListUtils.map(try_.getCatches(), catch_ -> {
                            if (TypeUtils.isOfClassType(catch_.getParameter().getType(), oldException)) {
                                return (J.Try.Catch) new ChangeType(oldException, newException, true)
                                        .getVisitor().visit(catch_, ctx);
                            }
                            return catch_;
                        }));
                    }
                }

                return try_;
            }
        };
    }
}
