package org.openrewrite.java.migrate.lombok.log;

import org.openrewrite.Recipe;

public abstract class ConvertLogRecipe extends Recipe {

    protected static final String FIELD_NAME_DESCRIPTION = "Name of the log field to replace. " +
            "If not specified, the field name is not checked and any field that satisfies the other checks is converted.";

    protected String getDisplayName(String annotation) {
        //language=markdown
        return String.format("Use `%s` instead of defining the field yourself", annotation);
    }

    protected String getDescription(String annotation, String pathToLogger) {
        //language=markdown
        return String.format("Prefer the lombok annotation `%s` over explicitly written out `%s` fields.", annotation, pathToLogger);
    }

}
