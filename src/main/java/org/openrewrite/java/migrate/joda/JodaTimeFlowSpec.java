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
package org.openrewrite.java.migrate.joda;

import lombok.NonNull;
import org.openrewrite.analysis.dataflow.DataFlowNode;
import org.openrewrite.analysis.dataflow.DataFlowSpec;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_CLASS_PATTERN;

class JodaTimeFlowSpec extends DataFlowSpec {

    @Override
    public boolean isSource(@NonNull DataFlowNode srcNode) {
        Object value = srcNode.getCursor().getParentTreeCursor().getValue();

        if (value instanceof J.Assignment && ((J.Assignment) value).getVariable() instanceof J.Identifier) {
            return isJodaType(((J.Assignment) value).getVariable().getType());
        }

        if (value instanceof J.VariableDeclarations.NamedVariable) {
            return isJodaType(((J.VariableDeclarations.NamedVariable) value).getType());
        }

        if (value instanceof J.VariableDeclarations) {
            if (srcNode.getCursor().getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.MethodDeclaration) {
                return isJodaType(((J.VariableDeclarations) value).getType());
            }
        }
        return false;
    }

    @Override
    public boolean isSink(@NonNull DataFlowNode sinkNode) {
        Object value = sinkNode.getCursor().getValue();
        Object parent = sinkNode.getCursor().getParentTreeCursor().getValue();
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation method = (J.MethodInvocation) parent;
            return (method.getSelect() != null && method.getSelect().equals(value)) ||
                   method.getArguments().stream().anyMatch(a -> a.equals(value));
        }
        return parent instanceof J.VariableDeclarations.NamedVariable ||
               parent instanceof J.NewClass ||
               parent instanceof J.Assignment ||
               parent instanceof J.Return;
    }

    static boolean isJodaType(JavaType type) {
        if (!(type instanceof JavaType.Class)) {
            return false;
        }
        return type.isAssignableFrom(JODA_CLASS_PATTERN);
    }
}
