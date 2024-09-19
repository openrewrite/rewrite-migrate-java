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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.trait.MethodAccess;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FindInternalJavaxApis extends Recipe {

    private final transient MethodCalls methodCalls = new MethodCalls(this);

    @Option(
            displayName = "Method pattern",
            description = "Optionally limit the search to declarations that match the provided method pattern.",
            example = "java.util.List add(..)",
            required = false
    )
    @Nullable
    private final String methodPattern;

    @Override
    public String getDisplayName() {
        return "Find uses of internal javax APIs";
    }

    @Override
    public String getDescription() {
        return "The libraries that define these APIs will have to be migrated before any of the repositories that use them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern javaxType = Pattern.compile(StringUtils.aspectjNameToPattern("javax..*"));
        return Preconditions.check(new UsesType<>("javax..*", null),
                (methodPattern == null ? new MethodAccess.Matcher() : new MethodAccess.Matcher(methodPattern))
                        .asVisitor((ma, ctx) -> {
                            MethodCall call = ma.getTree();
                            JavaType.Method methodType = call.getMethodType();
                            //noinspection ConstantValue
                            if (methodType == null || methodType.getReturnType() == null || methodType.getReturnType() instanceof JavaType.Unknown) {
                                return call;
                            }
                            if (methodType.getReturnType().isAssignableFrom(javaxType)) {
                                insertRow(ma, ctx, methodType);
                                return SearchResult.found(call);
                            }
                            for (JavaType parameterType : methodType.getParameterTypes()) {
                                if (parameterType.isAssignableFrom(javaxType)) {
                                    insertRow(ma, ctx, methodType);
                                    return SearchResult.found(call);
                                }
                            }
                            return call;
                        })
        );
    }

    private void insertRow(MethodAccess ma, ExecutionContext ctx, JavaType.Method methodType) {
        methodCalls.insertRow(ctx, new MethodCalls.Row(
                ma.getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                ma.getTree().printTrimmed(ma.getCursor()),
                methodType.getDeclaringType().toString(),
                methodType.getName(),
                methodType.getParameterTypes().stream().map(String::valueOf)
                        .collect(Collectors.joining(", "))
        ));
    }
}
