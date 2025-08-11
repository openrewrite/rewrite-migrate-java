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
package org.openrewrite.java.migrate;

import org.openrewrite.Recipe;
import org.openrewrite.staticanalysis.RemoveExtraSemicolons;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @deprecated Use {@link RemoveExtraSemicolons} instead.
 */
@Deprecated
public class RemoveIllegalSemicolons extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove illegal semicolons";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Remove semicolons after package declarations and imports, no longer accepted in Java 21 as of " +
                "[JDK-8027682](https://bugs.openjdk.org/browse/JDK-8027682).";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new RemoveExtraSemicolons());
    }
}
