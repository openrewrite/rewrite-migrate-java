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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.staticanalysis.RemoveMethodCallVisitor;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveMethodInvocation extends Recipe {
    @Option(displayName = "Method Pattern",
            description = "A method pattern for matching required method definition.",
            example = "*..* hello(..)")
    @NonNull
    String methodPattern;

    @Override
    public String getDisplayName() {
        return "Remove methods calls";
    }

    @Override
    public String getDescription() {
        return "Checks for a method patterns and removes the method call from the class.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveMethodCallVisitor<>(new MethodMatcher(methodPattern), (n, it) -> true);
    }
}
