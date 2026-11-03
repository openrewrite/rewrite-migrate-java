/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.maven.tree.ResolvedDependency;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Predicate;

final class ResolvedDependencyGraph {
    private ResolvedDependencyGraph() {
    }

    static boolean anyMatch(Collection<ResolvedDependency> roots, Predicate<ResolvedDependency> predicate) {
        Set<ResolvedDependency> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<ResolvedDependency> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            ResolvedDependency dependency = pending.removeFirst();
            if (visited.add(dependency)) {
                if (predicate.test(dependency)) {
                    return true;
                }
                pending.addAll(dependency.getDependencies());
            }
        }
        return false;
    }
}
