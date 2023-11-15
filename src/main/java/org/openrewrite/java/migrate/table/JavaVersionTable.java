/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class JavaVersionTable extends DataTable<JavaVersionTable.Row> {

    public JavaVersionTable(Recipe recipe) {
        super(recipe, "Java version table", "Records versions of Java in use");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source compatibility",
                description = "The version of Java used to compile the source code")
        String sourceVersion;

        @Column(displayName = "Target compatibility",
                description = "The version of Java the bytecode is compiled to run on")
        String targetVersion;
    }
}
