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
package org.openrewrite.java.migrate.guava;

import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        name = "Refaster style Guava to Java migration recipes",
        description = "Recipes that migrate from Guava to Java, using Refaster style templates for cases beyond what declarative recipes can cover."
)
public class NoGuavaRefaster {

    @RecipeDescriptor(
            name = "`Preconditions.checkNotNull` to `Objects.requireNonNull`",
            description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullToObjectsRequireNonNull {
        @BeforeTemplate
        Object before(Object object) {
            return com.google.common.base.Preconditions.checkNotNull(object);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object) {
            return java.util.Objects.requireNonNull(object);
        }
    }

    // Note: Order of the below two recipes are important.
    @RecipeDescriptor(
            name = "`Preconditions.checkNotNull` with `String` message to `Objects.requireNonNull`",
            description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullWithMessageToObjectsRequireNonNull {
        @BeforeTemplate
        Object before(Object object, String message) {
            return com.google.common.base.Preconditions.checkNotNull(object, message);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object, String message) {
            return java.util.Objects.requireNonNull(object, message);
        }
    }

    @RecipeDescriptor(
        name = "`Preconditions.checkNotNull` with `Object` message to `Objects.requireNonNull` with `String.valueOf`",
        description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullWithMessageToObjectsRequireNonNullMessageTypeObject {
        @BeforeTemplate
        Object before(Object object, Object message) {
            return com.google.common.base.Preconditions.checkNotNull(object, message);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object, Object message) {
            return java.util.Objects.requireNonNull(object, String.valueOf(message));
        }
    }
}
