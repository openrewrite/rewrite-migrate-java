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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.table.MethodCalls;
import org.openrewrite.java.trait.MethodAccess;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

@EqualsAndHashCode(callSuper = false)
@RequiredArgsConstructor
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
        TypeMatcher javaxType = new TypeMatcher("javax..*");
        return Preconditions.check(new UsesType<>("javax..*", null),
                (StringUtils.isBlank(methodPattern) ? new MethodAccess.Matcher() : new MethodAccess.Matcher(methodPattern))
                        .asVisitor((ma, ctx) -> {
                            MethodCall call = ma.getTree();
                            JavaType.Method methodType = call.getMethodType();
                            //noinspection ConstantValue
                            if (methodType == null || methodType.getReturnType() == null || methodType.getReturnType() instanceof JavaType.Unknown) {
                                return call;
                            }
                            if (TypeUtils.isAssignableTo(javaxType::matches, methodType.getReturnType())) {
                                insertRow(ma, ctx, methodType);
                                return SearchResult.found(call);
                            }
                            for (JavaType parameterType : methodType.getParameterTypes()) {
                                if (TypeUtils.isAssignableTo(javaxType::matches, parameterType)) {
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
                ma.getTree().printTrimmed(ma.getCursor().getParentOrThrow()),
                methodType.getDeclaringType().toString(),
                methodType.getName(),
                methodType.getParameterTypes().stream().map(String::valueOf)
                        .collect(joining(", "))
        ));
    }
}
