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

}
