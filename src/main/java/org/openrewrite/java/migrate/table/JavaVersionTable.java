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
