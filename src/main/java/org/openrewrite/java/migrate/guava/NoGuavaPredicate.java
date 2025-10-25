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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.search.UsesMethod;

public class NoGuavaPredicate extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change Guava's `Predicate` into `java.util.function.Predicate` where possible";
    }

    @Override
    public String getDescription() {
        return "Change the type only where no methods are used that explicitly require a Guava `Predicate`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.not(new UsesMethod<>("*..* *(.., com.google.common.base.Predicate)")),
                new ChangeType(
                        "com.google.common.base.Predicate",
                        "java.util.function.Predicate",
                        false)
                        .getVisitor()
        );
    }
}
