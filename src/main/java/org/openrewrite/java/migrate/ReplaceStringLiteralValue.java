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
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.NonNull;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @deprecated in favor of {@link org.openrewrite.java.ReplaceStringLiteralValue}.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@Deprecated
public class ReplaceStringLiteralValue extends Recipe {

    @Option(displayName = "Old literal `String` value",
            description = "The `String` value to replace.",
            example = "apple")
    @NonNull
    String oldLiteralValue;

    @Option(displayName = "New literal `String` value",
            description = "The `String` value to replace with.",
            example = "orange")
    @NonNull
    String newLiteralValue;

    @JsonCreator
    public ReplaceStringLiteralValue(@NonNull @JsonProperty("oldStringValue") String oldStringValue, @NonNull @JsonProperty("newStringValue") String newStringValue) {
        this.oldLiteralValue = oldStringValue;
        this.newLiteralValue = newStringValue;
    }

    @Override
    public String getDisplayName() {
        return "Replace `String` literal";
    }

    @Override
    public String getDescription() {
        return "Replace the value of a complete `String` literal.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new org.openrewrite.java.ReplaceStringLiteralValue(oldLiteralValue, newLiteralValue));
    }
}
