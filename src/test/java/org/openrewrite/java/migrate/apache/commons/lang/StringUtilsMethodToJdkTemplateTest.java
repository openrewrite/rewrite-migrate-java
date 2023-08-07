package org.openrewrite.java.migrate.apache.commons.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StringUtilsMethodToJdkTemplateTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    void defaultStringObjectsToString() {
        rewriteRun(
          spec -> spec.recipe(new StringUtilsMethodToJdkTemplate(
            "org.apache.commons.lang3.StringUtils defaultString(java.lang.String)",
            "Objects.toString(#{any()})",
            new String[]{"java.util.Objects"})),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;
            
            class Test {
               String s = StringUtils.defaultString("foo");
            }
            """, """
            import java.util.Objects;
            
            class Test {
               String s = Objects.toString("foo");
            }
            """)
        );
    }

}