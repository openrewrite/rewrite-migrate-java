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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindThreadLocalsMutatedOnlyInDefiningScope extends AbstractFindThreadLocals {

    @Override
    public String getDisplayName() {
        return "Find ThreadLocal variables mutated only in their defining scope";
    }

    @Override
    public String getDescription() {
        return "Find `ThreadLocal` variables that are only mutated within their defining class or initialization context (constructor/static initializer). " +
               "These may be candidates for refactoring as they have limited mutation scope. " +
               "The recipe identifies `ThreadLocal` variables that are only modified during initialization or within their declaring class.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("java25", "threadlocal", "scopedvalue", "migration"));
    }

    @Override
    protected boolean shouldMarkThreadLocal(ThreadLocalInfo info) {
        // Early return for ThreadLocals without mutations
        if (!info.hasAnyMutation()) {
            return false;
        }
        // Early return for non-private ThreadLocals
        if (!info.isPrivate()) {
            return false;
        }
        // Mark if only locally mutated
        return info.isOnlyLocallyMutated();
    }

    @Override
    protected String getMessage(ThreadLocalInfo info) {
        return info.hasOnlyInitMutations()
            ? "ThreadLocal is only mutated during initialization (constructor/static initializer)"
            : "ThreadLocal is only mutated within its defining class";
    }

    @Override
    protected String getMutationType(ThreadLocalInfo info) {
        return info.hasOnlyInitMutations()
            ? "Mutated only in initialization"
            : "Mutated in defining class";
    }
}