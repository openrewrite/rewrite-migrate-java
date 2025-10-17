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
package org.openrewrite.java.migrate.search.threadlocal;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindNeverMutatedThreadLocals extends AbstractFindThreadLocals {

    @Override
    public String getDisplayName() {
        return "Find ThreadLocal variables that are never mutated";
    }

    @Override
    public String getDescription() {
        return "Find `ThreadLocal` variables that are never mutated after initialization. " +
               "These are prime candidates for migration to `ScopedValue` in Java 25+ as they are effectively immutable. " +
               "The recipe identifies `ThreadLocal` variables that are only initialized but never reassigned or modified through `set()` or `remove()` methods.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("java25", "threadlocal", "scopedvalue", "migration"));
    }

    @Override
    protected boolean shouldMarkThreadLocal(ThreadLocalInfo info) {
        // Mark ThreadLocals that have no mutations at all
        return info.hasNoMutation();
    }

    @Override
    protected String getMessage(ThreadLocalInfo info) {
        return "ThreadLocal is never mutated and could be replaced with ScopedValue";
    }

    @Override
    protected String getMutationType(ThreadLocalInfo info) {
        return "Never mutated";
    }
}
