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
package org.openrewrite.java.migrate.table;

import lombok.Value;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class DtoDataUses extends DataTable<DtoDataUses.Row> {

    public DtoDataUses(Recipe recipe) {
        super(recipe,
                "Uses of the data elements of a DTO",
                "The use of the data elements of a DTO by the method declaration using it.");
    }

    @Value
    public static class Row {
        String sourcePath;
        String methodName;
        String field;
    }
}
