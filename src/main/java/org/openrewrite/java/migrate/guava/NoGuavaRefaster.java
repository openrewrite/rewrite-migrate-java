package org.openrewrite.java.migrate.guava;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        name = "Refaster style Guava to Java migration recipes.",
        description = "Recipes that migrate from Guava to Java, using Refaster style templates for cases beyond what declarative recipes can cover."
)
public class NoGuavaRefaster {

    @RecipeDescriptor(
            name = "`Preconditions.checkNotNull` to `Objects.requireNonNull`",
            description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullToObjectsRequireNonNull {
        @BeforeTemplate
        Object before(Object object, Object message) {
            return com.google.common.base.Preconditions.checkNotNull(object, message);
        }

        @AfterTemplate
        Object after(Object object, Object message) {
            return java.util.Objects.requireNonNull(object, String.valueOf(message));
        }
    }

    @RecipeDescriptor(
            name = "`String.valueof(String)` to `String`",
            description = "Migrate from `String.valueof(String)` to `String`, mainly as a cleanup after other recipes."
    )
    public static class StringValueOfString {
        @BeforeTemplate
        String before(String string) {
            return String.valueOf(string);
        }

        @AfterTemplate
        String after(String string) {
            return (string);
        }
    }
}
