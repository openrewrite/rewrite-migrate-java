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
package org.openrewrite.java.migrate.joda;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.marker.Marker;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A marker to indicate whether an expression is safe to migrate
 * and variables that are referenced in the expression.
 */
@Value
@With
public class SafeCheckMarker implements Marker {
    @EqualsAndHashCode.Include
    UUID id;
    boolean isSafe;
    Set<NamedVariable> references;

    public SafeCheckMarker(UUID id, boolean isSafe, Set<NamedVariable> references) {
        this.id = id;
        this.isSafe = isSafe;
        this.references = references;
    }

    public SafeCheckMarker(UUID id, boolean isSafe) {
        this(id, isSafe, new HashSet<>());
    }
}
