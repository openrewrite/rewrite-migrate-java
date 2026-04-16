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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.search.UsesJavaVersion;

@EqualsAndHashCode(callSuper = false)
@Value
public class RenameUnderscoreIdentifier extends Recipe {

    String displayName = "Rename `_` identifier to `__`";

    String description = "Renames single-underscore identifiers to double-underscore " +
                          "in Java source files with source compatibility of Java 8 or below. " +
                          "In Java 9+, `_` is a reserved keyword and causes a compile error.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(1, 8),
                new RenameIdentifierVisitor("_", "__")
        );
    }
}
