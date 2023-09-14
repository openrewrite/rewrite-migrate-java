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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.Preconditions;
import org.openrewrite.java.search.UsesMethod;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeStringLiteralValue extends Recipe {

    @Option(displayName = "Old string value",
            description = "The string value to replace.",
            example = "apple")
    @NonNull
    String oldStringValue;

    @Option(displayName = "New stringvalue",
            description = "New string to replace the old string value with.",
            example = "orange")
    @NonNull
    String newStringValue;

    @JsonCreator
    public ChangeStringLiteralValue(@NonNull @JsonProperty("oldStringValue") String oldStringValue, @NonNull @JsonProperty("newStringValue") String newStringValue) {
        this.oldStringValue = oldStringValue;
        this.newStringValue = newStringValue;
    }

    @Override
    public String getDisplayName() {
        return "Change string literal";
    }

    @Override
    public String getDescription() {
        return "Changes the value of a string literal.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                String literalValue = (String) literal.getValue();
                if (literalValue != null && literalValue.equals(oldStringValue)) {
                    literal = literal.withValue(newStringValue).withValueSource("\"" + newStringValue + "\"");
                }
                return literal;
            }
        };
    }

}