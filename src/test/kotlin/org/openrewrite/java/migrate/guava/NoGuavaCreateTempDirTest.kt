package org.openrewrite.java.migrate.guava

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("ResultOfMethodCallIgnored", "UnstableApiUsage")
class NoGuavaCreateTempDirTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("guava")
            .build()

    override val recipe: Recipe
        get() = NoGuavaCreateTempDir()

    @Test
    fun inMethodThrowingIoException() = assertChanged(
        before = """
            import java.io.File;
            import java.io.IOException;
            import com.google.common.io.Files;
            
            class A {
                void doSomething() throws IOException {
                    File dir = Files.createTempDir();
                    dir.createNewFile();
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void doSomething() throws IOException {
                    File dir = Files.createTempDirectory(null).toFile();
                    dir.createNewFile();
                }
            }
        """
    )

    @Test
    fun tempDirIsFieldVar() = assertChanged(
        before = """
            import java.io.File;
            import com.google.common.io.Files;
            
            class A {
                File gDir = Files.createTempDir();
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                File gDir = createTempDirectory();
            
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )

    @Test
    fun tempDirIsStaticBlock() = assertChanged(
        before = """
            import java.io.File;
            import com.google.common.io.Files;
            
            class A {
                private final File file;
                {
                    file = com.google.common.io.Files.createTempDir();
                }
                
                public void main(String[] args) {
                    System.out.println(file.getAbsolutePath());
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                private final File file;
                {
                    file = createTempDirectory();
                }
                
                public void main(String[] args) {
                    System.out.println(file.getAbsolutePath());
                }
            
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )

    @Test
    fun tempDirIsInMethodDeclaration() = assertChanged(
        before = """
            import java.io.File;
            import com.google.common.io.Files;
            
            class A {
                void doSomething() {
                    File gDir = Files.createTempDir();
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void doSomething() {
                    File gDir = createTempDirectory();
                }
            
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )

    @Test
    fun tempDirIsInMethodDeclarationCreateTempDirMethodExists() = assertChanged(
        before = """
            import java.io.File;
            import java.nio.file.Files;
            import java.io.IOException;
            
            class A {
                void doSomething() {
                    File gDir = com.google.common.io.Files.createTempDir();
                }
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        after = """
            import java.io.File;
            import java.nio.file.Files;
            import java.io.IOException;
            
            class A {
                void doSomething() {
                    File gDir = createTempDirectory();
                }
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )

    @Test
    fun guavaCreateTempDirIsMethodArgument() = assertChanged(
        before = """
            import java.io.File;
            import com.google.common.io.Files;
            
            class A {
                void butWhy() {
                    doSomething(Files.createTempDir(), "some text");
                }
            
                void doSomething(File file, String content) {
                }
            }
        """,
        after = """
            import java.io.File;
            import java.io.IOException;
            import java.nio.file.Files;
            
            class A {
                void butWhy() {
                    doSomething(createTempDirectory(), "some text");
                }
            
                void doSomething(File file, String content) {
                }
            
                private static File createTempDirectory() {
                    try {
                        return Files.createTempDirectory(null).toFile();
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                }
            }
        """,
        typeValidation = {methodInvocations = false}
    )
}
