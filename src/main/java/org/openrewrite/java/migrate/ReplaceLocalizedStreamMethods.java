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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceLocalizedStreamMethods extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedInputStream(java.io.InputStream)")
    String localizedInputStreamMethodMatcher;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedOutputStream(java.io.OutputStream)")
    String localizedOutputStreamMethodMatcher;

    @JsonCreator
    public ReplaceLocalizedStreamMethods(
            @Nullable String localizedInputStreamMethodMatcher,
            @Nullable String localizedOutputStreamMethodMatcher) {
        this.localizedInputStreamMethodMatcher = localizedInputStreamMethodMatcher == null ?
                "java.lang.Runtime getLocalizedInputStream(java.io.InputStream)" : localizedInputStreamMethodMatcher;
        this.localizedOutputStreamMethodMatcher = localizedOutputStreamMethodMatcher == null ?
                "java.lang.Runtime getLocalizedOutputStream(java.io.OutputStream)" : localizedOutputStreamMethodMatcher;
    }

    @Override
    public String getDisplayName() {
        return "Replace `getLocalizedInputStream` and `getLocalizedOutputStream` with direct assignment";
    }

    @Override
    public String getDescription() {
        return "Replaces `Runtime.getLocalizedInputStream(InputStream)` and `Runtime.getLocalizedOutputStream(OutputStream)` with their direct arguments. " +
               "This modification is made because the previous implementation of `getLocalizedInputStream` and `getLocalizedOutputStream` merely returned the arguments provided.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher LocalizedInputStreamMethod = new MethodMatcher(localizedInputStreamMethodMatcher, false);
            private final MethodMatcher localizedOutputStreamMethod = new MethodMatcher(localizedOutputStreamMethodMatcher, false);

            @Override
            public J visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                if (LocalizedInputStreamMethod.matches(mi) || localizedOutputStreamMethod.matches(mi)) {
                    return mi.getArguments().get(0).withPrefix(mi.getPrefix());
                }
                return super.visitMethodInvocation(mi, ctx);
            }
        };
    }
}
