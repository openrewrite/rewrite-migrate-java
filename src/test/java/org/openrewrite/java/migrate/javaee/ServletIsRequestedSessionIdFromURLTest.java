package org.openrewrite.java.migrate.javaee;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ServletIsRequestedSessionIdFromURLTest  implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "javax.servlet-3.0"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.javaee8").build()
            .activateRecipes("org.openrewrite.java.migrate.javaee8.ServletIsRequestedSessionIdFromURL"));
    }

    @Test
    void testUpdateSessionURL() {
        rewriteRun(
          //language=java
          java("""
            package com.test;

            import javax.servlet.http.HttpServletRequestWrapper;
            
            public class IsRequestedSessionIdFromUrlTest {
                public static void main(String args[]) {
                    HttpServletRequestWrapper foo = new HttpServletRequestWrapper(null);
                    foo.isRequestedSessionIdFromUrl();
                }
            }
            """, """
            package com.test;

            import javax.servlet.http.HttpServletRequestWrapper;
            
            public class IsRequestedSessionIdFromUrlTest {
                public static void main(String args[]) {
                    HttpServletRequestWrapper foo = new HttpServletRequestWrapper(null);
                    foo.isRequestedSessionIdFromURL();
                }
            }
            """));
    }
}
