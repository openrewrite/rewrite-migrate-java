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
package org.openrewrite.java.migrate.guava;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoGuavaImmutableSetOf extends AbstractNoGuavaImmutableOf {
    public NoGuavaImmutableSetOf() {
        super("com.google.common.collect.ImmutableSet", "java.util.Set");
    }

    public NoGuavaImmutableSetOf(@Nullable Boolean convertReturnType) {
        super("com.google.common.collect.ImmutableSet", "java.util.Set", convertReturnType);
    }

    /**
     * {@code Set.of} throws on equal elements, where Guava keeps one of them.
     * Two or more elements convert only when each is a literal or an enum constant and no two are the same.
     */
    @Override
    boolean isConvertible(List<Expression> elements) {
        if (elements.size() < 2) {
            return true;
        }
        Set<String> distinct = new HashSet<>();
        for (Expression element : elements) {
            String constant = constant(element);
            if (constant == null || !distinct.add(constant)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String constant(Expression element) {
        if (element instanceof J.Literal) {
            J.Literal literal = (J.Literal) element;
            return literal.getValue() == null ? null : literal.getType() + ":" + literal.getValue();
        }
        JavaType.Variable field = element instanceof J.Identifier ? ((J.Identifier) element).getFieldType() :
                element instanceof J.FieldAccess ? ((J.FieldAccess) element).getName().getFieldType() : null;
        JavaType.FullyQualified enumType = field == null ? null : TypeUtils.asFullyQualified(field.getType());
        if (enumType != null && enumType.getKind() == JavaType.FullyQualified.Kind.Enum &&
            TypeUtils.isOfType(field.getOwner(), enumType)) {
            return enumType.getFullyQualifiedName() + "." + field.getName();
        }
        return null;
    }
}
