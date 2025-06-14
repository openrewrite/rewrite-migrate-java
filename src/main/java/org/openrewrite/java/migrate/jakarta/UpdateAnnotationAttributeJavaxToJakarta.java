/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.trait.Annotated;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateAnnotationAttributeJavaxToJakarta extends Recipe {

    @Override
    public String getDisplayName() {
        return "Update annotation attributes using `javax` to `jakarta`";
    }

    @Override
    public String getDescription() {
        return "Replace `javax` with `jakarta` in annotation attributes for matching annotation signatures.";
    }

    @Option(
            displayName = "Annotation signature",
            description = "An annotation signature to match.",
            example = "@javax.jms..*",
            required = false
    )
    String signature;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Annotated.Matcher(signature).asVisitor(ann -> ann.getTree()
                .withArguments(ListUtils.map(ann.getTree().getArguments(), arg -> {
                    if (arg instanceof J.Assignment) {
                        J.Assignment as = (J.Assignment) arg;
                        if (as.getAssignment() instanceof J.Literal) {
                            return as.withAssignment(maybeReplaceLiteralValue((J.Literal) as.getAssignment()));
                        }
                    } else if (arg instanceof J.Literal) {
                        return maybeReplaceLiteralValue((J.Literal) arg);
                    }
                    return arg;
                })));
    }

    private J.Literal maybeReplaceLiteralValue(J.Literal arg) {
        if (arg.getType() == JavaType.Primitive.String && arg.getValue() instanceof String) {
            String oldValue = (String) arg.getValue();
            if (oldValue.contains("javax.")) {
                String newValue = oldValue.replace("javax.", "jakarta.");
                return arg.withValue(newValue).withValueSource('"' + newValue + '"');
            }
        }
        return arg;
    }
}
