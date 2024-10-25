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

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;

import java.util.HashSet;
import java.util.Set;

public class JodaTimeRecipe extends ScanningRecipe<Set<NamedVariable>> {
    @Override
    public String getDisplayName() {
        return "Migrate Joda Time to Java Time";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Prefer the Java standard library over third-party usage of Joda Time.";
    }

    @Override
    public Set<NamedVariable> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public JodaTimeScanner getScanner(Set<NamedVariable> acc) {
        return new JodaTimeScanner(acc);
    }

    @Override
    public JodaTimeVisitor getVisitor(Set<NamedVariable> acc) {
        return new JodaTimeVisitor(acc);
    }
}
