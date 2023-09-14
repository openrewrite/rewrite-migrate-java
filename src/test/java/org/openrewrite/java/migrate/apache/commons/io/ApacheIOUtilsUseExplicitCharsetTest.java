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
package org.openrewrite.java.migrate.apache.commons.io;

import org.junit.jupiter.api.Test;

import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.srcMainJava;

class ApacheIOUtilsUseExplicitCharsetTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ApacheIOUtilsUseExplicitCharset(null))
          .parser(JavaParser.fromJavaVersion().classpath("commons-io"));
    }

    @Test
    void useCharset() {
        //language=java
        rewriteRun(
          srcMainJava(
            java(
              """
                import org.apache.commons.io.IOUtils;
                import java.io.InputStream;
                import java.io.OutputStream;
                import java.io.Reader;
                import java.io.Writer;
                import java.net.URI;
                import java.net.URL;
                                
                class T {
                    InputStream inputStream;
                    OutputStream outputStream;
                    Reader reader;
                    Writer writer;
                    CharSequence charSequence;
                    String someString;
                    byte[] bytes;
                    URI uri;
                    URL url;
                    char[] chars;
                    StringBuffer stringBuffer;
                  
                    void flex() {
                        IOUtils.copy(inputStream, writer);
                        IOUtils.copy(reader, outputStream);
                        IOUtils.readLines(inputStream);
                        IOUtils.toByteArray(someString);
                        IOUtils.toByteArray(reader);
                        IOUtils.toCharArray(inputStream);
                        IOUtils.toInputStream(charSequence);
                        IOUtils.toInputStream("Test");
                        IOUtils.toString("Test".getBytes());
                        IOUtils.toString(inputStream);
                        IOUtils.toString(uri);
                        IOUtils.toString(url);
                        IOUtils.write(bytes, writer);
                        IOUtils.write(chars, outputStream);
                        IOUtils.write(charSequence, outputStream);
                        IOUtils.write(someString, outputStream);
                        IOUtils.write(stringBuffer, outputStream);
                    }
                }
                """,
              """
                import org.apache.commons.io.IOUtils;
                import java.io.InputStream;
                import java.io.OutputStream;
                import java.io.Reader;
                import java.io.Writer;
                import java.net.URI;
                import java.net.URL;
                import java.nio.charset.StandardCharsets;
                                
                class T {
                    InputStream inputStream;
                    OutputStream outputStream;
                    Reader reader;
                    Writer writer;
                    CharSequence charSequence;
                    String someString;
                    byte[] bytes;
                    URI uri;
                    URL url;
                    char[] chars;
                    StringBuffer stringBuffer;
                  
                    void flex() {
                        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
                        IOUtils.copy(reader, outputStream, StandardCharsets.UTF_8);
                        IOUtils.readLines(inputStream, StandardCharsets.UTF_8);
                        someString.getBytes(StandardCharsets.UTF_8);
                        IOUtils.toByteArray(reader, StandardCharsets.UTF_8);
                        IOUtils.toCharArray(inputStream, StandardCharsets.UTF_8);
                        IOUtils.toInputStream(charSequence, StandardCharsets.UTF_8);
                        IOUtils.toInputStream("Test", StandardCharsets.UTF_8);
                        IOUtils.toString("Test".getBytes(), StandardCharsets.UTF_8);
                        IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                        IOUtils.toString(uri, StandardCharsets.UTF_8);
                        IOUtils.toString(url, StandardCharsets.UTF_8);
                        IOUtils.write(bytes, writer, StandardCharsets.UTF_8);
                        IOUtils.write(chars, outputStream, StandardCharsets.UTF_8);
                        IOUtils.write(charSequence, outputStream, StandardCharsets.UTF_8);
                        IOUtils.write(someString, outputStream, StandardCharsets.UTF_8);
                        IOUtils.write(stringBuffer, outputStream, StandardCharsets.UTF_8);
                    }
                }
                """
            )
          )
        );
    }
}
