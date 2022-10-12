package org.openrewrite.java.migrate.util;

import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import org.junit.jupiter.api.Test;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class OptionalNotPresentToIsEmptyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new OptionalNotPresentToIsEmpty());
        spec.parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @Test
    void should_replace_not_isPresent_with_isEmpty() {
        rewriteRun(version(java("""
                package com.example.app;
                import java.util.Optional;
                class App {
                    boolean notPresent(Optional<String> bar){
                        return !bar.isPresent();
                    }
                }""", """
                package com.example.app;
                import java.util.Optional;
                class App {
                    boolean notPresent(Optional<String> bar){
                        return bar.isEmpty();
                    }
                }"""),
                17));
    }

}
