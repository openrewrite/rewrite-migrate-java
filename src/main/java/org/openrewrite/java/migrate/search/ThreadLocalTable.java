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

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class ThreadLocalTable extends DataTable<ThreadLocalTable.Row> {

    public ThreadLocalTable(Recipe recipe) {
        super(recipe,
                "ThreadLocal usage",
                "ThreadLocal variables and their mutation patterns.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file containing the ThreadLocal declaration.")
        String sourceFile;

        @Column(displayName = "Class name",
                description = "The fully qualified class name where the ThreadLocal is declared.")
        String className;

        @Column(displayName = "Field name",
                description = "The name of the ThreadLocal field.")
        String fieldName;

        @Column(displayName = "Access modifier",
                description = "The access modifier of the ThreadLocal field (private, protected, public, package-private).")
        String accessModifier;

        @Column(displayName = "Field modifiers",
                description = "Additional modifiers like static, final.")
        String modifiers;

        @Column(displayName = "Mutation type",
                description = "Type of mutation detected (Never mutated, Mutated only in initialization, Mutated in defining class, Mutated externally, Potentially mutable).")
        String mutationType;

        @Column(displayName = "Message",
                description = "Detailed message about the ThreadLocal's usage pattern.")
        String message;
    }
}