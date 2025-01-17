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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @deprecated in favor of {@link org.openrewrite.java.ChangeMethodInvocationReturnType}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@Deprecated
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The fully qualified new return type of method invocation.",
            example = "long")
    String newReturnType;

    @Override
    public String getDisplayName() {
        return "Change method invocation return type";
    }

    @Override
    public String getDescription() {
        return "Changes the return type of a method invocation.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new org.openrewrite.java.ChangeMethodInvocationReturnType(methodPattern, newReturnType));
    }
}
