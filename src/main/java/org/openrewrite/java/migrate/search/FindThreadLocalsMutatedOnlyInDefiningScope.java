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
    protected boolean shouldMarkThreadLocal(ThreadLocalInfo info) {
        // Mark private ThreadLocals that ARE mutated, but only locally
        return info.hasAnyMutation() &&
               info.isPrivate() &&
               info.isOnlyLocallyMutated();
    }

    @Override
    protected String getMessage(ThreadLocalInfo info) {
        if (info.hasOnlyInitMutations()) {
            return "ThreadLocal is only mutated during initialization (constructor/static initializer)";
        } else {
            return "ThreadLocal is only mutated within its defining class";
        }
    }

    @Override
    protected String getMutationType(ThreadLocalInfo info) {
        if (info.hasOnlyInitMutations()) {
            return "Mutated only in initialization";
        } else {
            return "Mutated in defining class";
        }
    }
}