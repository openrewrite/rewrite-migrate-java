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
package org.openrewrite.java.migrate.apache.commons.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ApacheCommonsStringUtilsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"));
    }

    @Test
    @DocumentExample
    void defaultString() {
        rewriteRun(
          spec -> spec.recipe(new DefaultStringRecipe()),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;
                          
            class Foo {
                String in = "foo";
                String out = StringUtils.defaultString(in);
            }
            """, """
            import java.util.Objects;
                          
            class Foo {
                String in = "foo";
                String out = Objects.toString(in);
            }
            """)
        );
    }
    @Test
    void defaultStringStatic() {
        rewriteRun(
          spec -> spec.recipe(new DefaultStringRecipe()),
          //language=java
          java("""
            import static org.apache.commons.lang3.StringUtils.defaultString;
                          
            class Foo {
                String in = "foo";
                String out = defaultString(in);
            }
            """, """
            import java.util.Objects;
                          
            class Foo {
                String in = "foo";
                String out = Objects.toString(in);
            }
            """)
        );
    }

}
