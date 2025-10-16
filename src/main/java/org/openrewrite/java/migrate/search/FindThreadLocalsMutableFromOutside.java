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
public class FindThreadLocalsMutableFromOutside extends AbstractFindThreadLocals {

    @Override
    public String getDisplayName() {
        return "Find ThreadLocal variables mutable from outside their defining class";
    }

    @Override
    public String getDescription() {
        return "Find `ThreadLocal` variables that can be mutated from outside their defining class. " +
               "These ThreadLocals have the highest risk as they can be modified by any code with access to them. " +
               "This includes non-private ThreadLocals or those mutated from other classes in the codebase.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("java25", "threadlocal", "scopedvalue", "migration", "security"));
    }

    @Override
    protected boolean shouldMarkThreadLocal(ThreadLocalInfo info) {
        // Mark ThreadLocals that are either:
        // 1. Actually mutated from outside their defining class
        // 2. Non-private (and thus potentially mutable from outside)
        return info.hasExternalMutations() || !info.isPrivate();
    }

    @Override
    protected String getMessage(ThreadLocalInfo info) {
        if (info.hasExternalMutations()) {
            return "ThreadLocal is mutated from outside its defining class";
        } else {
            // Non-private but not currently mutated externally
            String access = info.isStatic() ? "static " : "";
            return "ThreadLocal is " + access + "non-private and can potentially be mutated from outside";
        }
    }

    @Override
    protected String getMutationType(ThreadLocalInfo info) {
        if (info.hasExternalMutations()) {
            return "Mutated externally";
        } else {
            return "Potentially mutable";
        }
    }
}