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
package org.openrewrite.java.migrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class ReplaceLocalizedStreamMethods extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedInputStream(java.io.InputStream)",
            required = false)
    String localizedInputStreamMethodMatcher;

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedOutputStream(java.io.OutputStream)",
            required = false)
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

    String displayName = "Replace `getLocalizedInputStream` and `getLocalizedOutputStream` with direct assignment";

    String description = "Replaces `Runtime.getLocalizedInputStream(InputStream)` and `Runtime.getLocalizedOutputStream(OutputStream)` with their direct arguments. " +
               "This modification is made because the previous implementation of `getLocalizedInputStream` and `getLocalizedOutputStream` merely returned the arguments provided.";

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
