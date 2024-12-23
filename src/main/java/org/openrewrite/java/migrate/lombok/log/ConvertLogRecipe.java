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
package org.openrewrite.java.migrate.lombok.log;

import org.openrewrite.Recipe;

public abstract class ConvertLogRecipe extends Recipe {

    protected static final String FIELD_NAME_DESCRIPTION = "Name of the log field to replace. " +
            "If not specified, the field name is not checked and any field that satisfies the other checks is converted.";

    protected String getDisplayName(String annotation) {
        return String.format("Use `%s` instead of defining the field yourself", annotation);
    }

    protected String getDescription(String annotation, String pathToLogger) {
        //language=markdown
        return String.format("Prefer the lombok annotation `%s` over explicitly written out `%s` fields.", annotation, pathToLogger);
    }

}
