package org.openrewrite.java.migrate.apache.commons.io

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest


class UseJavaStandardCharsetsTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("commons-io")
            .build()

    override val recipe: Recipe = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.migrate.apache.commons.io")
        .build()
        .activateRecipes("org.openrewrite.java.migrate.apache.commons.io.UseStandardCharsets")

    @Test
    @Suppress("deprecation")
    fun toStandardCharsets() {
        assertChanged(
            before = """
                import java.nio.charset.Charset;
                import org.apache.commons.io.Charsets;
    
                class A {
                     Charset iso88591 = Charsets.ISO_8859_1;
                     Charset usAscii = Charsets.US_ASCII;
                     Charset utf16 = Charsets.UTF_16;
                     Charset utf16be = Charsets.UTF_16BE;
                     Charset utf16le = Charsets.UTF_16LE;
                     Charset utf8 = Charsets.UTF_8;
                }
            """,
            after = """
                import java.nio.charset.Charset;
                import java.nio.charset.StandardCharsets;
    
                class A {
                     Charset iso88591 = StandardCharsets.ISO_8859_1;
                     Charset usAscii = StandardCharsets.US_ASCII;
                     Charset utf16 = StandardCharsets.UTF_16;
                     Charset utf16be = StandardCharsets.UTF_16BE;
                     Charset utf16le = StandardCharsets.UTF_16LE;
                     Charset utf8 = StandardCharsets.UTF_8;
                }
            """
        )
    }
}