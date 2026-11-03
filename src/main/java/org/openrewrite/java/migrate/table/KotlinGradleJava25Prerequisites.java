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
package org.openrewrite.java.migrate.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class KotlinGradleJava25Prerequisites extends DataTable<KotlinGradleJava25Prerequisites.Row> {
    public KotlinGradleJava25Prerequisites(Recipe recipe) {
        super(recipe, "Kotlin Gradle prerequisites for Java 25", "Gradle builds preserved because their Kotlin compiler requires a manual compatibility decision.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Build file", description = "The compiler declaration associated with the preserved Gradle build.")
        String buildFile;
        @Column(displayName = "Compiler version", description = "The declared compiler value or configured default, or unknown when unresolved.")
        String compilerVersion;
        @Column(displayName = "Reason", description = "Why this build is not automatically migrated.")
        String reason;
        @Column(displayName = "Manual prerequisite", description = "The explicit compatibility decision needed before Java 25 migration.")
        String manualPrerequisite;
    }
}
