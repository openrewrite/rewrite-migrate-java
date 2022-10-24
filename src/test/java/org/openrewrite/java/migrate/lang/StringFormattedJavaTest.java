/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import org.junit.jupiter.api.Test;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class StringFormattedJavaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StringFormatted());
        spec.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/2163")
    void should_convert_text_block() {
        rewriteRun(version(java("""
                package com.example.app;
                class A {
                    String str = String.format(\"""
                    foo
                    %s
                    \""", "a");
                }""", """
                package com.example.app;
                class A {
                    String str = \"""
                    foo
                    %s
                    \""".formatted("a");
                }"""),
                17));
    }

    @Test
    void should_convert_concatenated_text() {
        rewriteRun(version(java("""
                package com.example.app;
                class A {
                    String str = String.format("foo"
                            + "%s", "a");
                }""", """
                package com.example.app;
                class A {
                    String str = ("foo"
                            + "%s").formatted("a");
                }"""),
                17));
    }

    @Test
    void should_convert_when_calling_function() {
        rewriteRun(version(java("""
                package com.example.app;

                class A {
                    String str = String.format(getTemplateString(), "a");

                    private String getTemplateString() {
                        return "foo %s";
                    }
                }""", """
                package com.example.app;

                class A {
                    String str = getTemplateString().formatted("a");

                    private String getTemplateString() {
                        return "foo %s";
                    }
                }"""),
                17));
    }

}
