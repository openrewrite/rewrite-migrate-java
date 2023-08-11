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

    @Test
    void isEmpty() {
        rewriteRun(
          spec -> spec.recipe(new IsEmptyRecipe()),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;
    
            class Foo {
                String in = "foo";
                boolean out = StringUtils.isEmpty(in);
            }
            """, """
            class Foo {
                String in  = "foo";
                boolean out = in.isEmpty();
            }
            """)
        );
    }

    @Test
    void splitTest() {
        rewriteRun(
          spec -> spec.recipe(new SplitRecipe()),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;
            
            class Foo {
                String in = "foo";
                String[] out = StringUtils.split(in);
            }
            """, """
            class Foo {
                String in = "foo";
                String[] out = in.split(" ");
            }
            """)
        );
    }

    @Test
    void splitWithSplitArg() {
        rewriteRun(
          spec -> spec.recipe(new SplitRecipe()),
          //language=java
          java("""
            import org.apache.commons.lang3.StringUtils;
            
            class Foo {
                String in = "foo";
                String[] out = StringUtils.split(in, "|");    
            }
            """, """
            class Foo {
                String in = "foo";
                String[] out = in.split("|");
            }
            """)
        );
    }

    //@Test
    //void oneArgTwoTemplateParameters() {
    //    rewriteRun(
    //      spec -> spec.recipe(new IsEmptyRecipe()),
    //      //language=java
    //      java("""
    //        import org.apache.commons.lang3.StringUtils;
    //
    //        class Foo {
    //            String in = "foo";
    //            String out = StringUtils.chop(in);
    //        }
    //        """, """
    //        class Foo {
    //            String in = "foo";
    //            String out = in.substring(0, in.length() - 1);
    //        }
    //        """)
    //    );
    //}

    //@Test
    //void outOfOrderTemplateParameters() {
    //    rewriteRun(
    //      spec -> spec.recipe(new IsBlankRecipe()),
    //      //language=java
    //      java("""
    //        import org.apache.commons.lang3.StringUtils;
    //
    //        class Foo {
    //            String in = "foo";
    //            String suffix = "oo";
    //            String out = StringUtils.stripEnd(in, suffix);
    //        }
    //        """, """
    //        class Foo {
    //            String in = "foo";
    //            String suffix = "oo";
    //            String out = in.endsWith(suffix) ? in.substring(0, in.lastIndexOf(suffix)) : in;
    //        }
    //        """)
    //    );
    //}

    //@Test
    //void threeArguments() {
    //    rewriteRun(
    //      spec -> spec.recipe(new IsBlankRecipe()),
    //      //language=java
    //      java("""
    //        import org.apache.commons.lang3.StringUtils;
    //
    //        class Foo {
    //            String in = "foo";
    //            String out = StringUtils.replace(in, "o", "z");
    //        }
    //        """, """
    //        class Foo {
    //            String in = "foo";
    //            String out = in.replaceAll("o", "z");
    //        }
    //        """)
    //    );
    //}
}
