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
package org.openrewrite.java.migrate.apache.commons.io;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ApacheCommonsFileUtilsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-io"))
          .recipe(new ApacheCommonsFileUtilsRecipes());
    }

    @Test
    @DocumentExample
    void ubertest() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.io.FileUtils;

              import java.io.File;
              import java.io.FileFilter;
              import java.net.URL;
              import java.nio.charset.Charset;
              import java.util.Collection;
              import java.util.Collections;
              import java.util.List;

              class Foo {
                  void bar(File fileA, File fileB, URL url, Charset cs, FileFilter filter, CharSequence charSeq) throws Exception {
                      long l = 10L;
                      String s = "hello world";
                      String[] stringArray = new String[4];
                      Collection<String> collection = Collections.EMPTY_LIST;
                      byte[] bytes = new byte[0];
                      String str;
                      boolean bool;
                      List<String> strList;
                      List<File> fileList;
                      File f;

                      FileUtils.write(fileA, s, cs);
                      f = FileUtils.getFile(s);
                      f = FileUtils.getFile(s, s);
                      f = FileUtils.toFile(url);

                      str = FileUtils.byteCountToDisplaySize(l);
                      FileUtils.cleanDirectory(fileA);
                      bool = FileUtils.contentEquals(fileA, fileB);
                      bool = FileUtils.contentEqualsIgnoreEOL(fileA, fileB, s);
                      FileUtils.copyDirectory(fileA, fileB);
                      FileUtils.copyFileToDirectory(fileA, fileB);
                      FileUtils.copyFile(fileA, fileB);
                      FileUtils.copyURLToFile(url, fileA);
                      f = FileUtils.current();
                      FileUtils.deleteDirectory(fileA);
                      f = FileUtils.delete(fileA);
                      bool = FileUtils.deleteQuietly(fileA);
                      FileUtils.forceDeleteOnExit(fileA);
                      FileUtils.forceDelete(fileA);
                      FileUtils.forceMkdir(fileA);
                      FileUtils.forceMkdirParent(fileA);
                      f = FileUtils.getTempDirectory();
                      str = FileUtils.readFileToString(fileA, cs);
                      str = FileUtils.readFileToString(fileA, s);
                      strList = FileUtils.readLines(fileA, cs);
                      FileUtils.writeByteArrayToFile(fileA, bytes);
                      FileUtils.writeLines(fileA, collection);
                      FileUtils.writeStringToFile(fileA, s);
                      
                      FileUtils.writeStringToFile(fileA, s, bool);
                      FileUtils.writeStringToFile(fileA, s, cs);
                      FileUtils.writeStringToFile(fileA, s, cs, bool);
                      FileUtils.writeStringToFile(fileA, s, str);
                      FileUtils.writeStringToFile(fileA, s, str, bool);
                  }
              }
              """,
            """
              import org.apache.commons.io.FileUtils;

              import java.io.File;
              import java.io.FileFilter;
              import java.net.URL;
              import java.nio.charset.Charset;
              import java.nio.file.Files;
              import java.nio.file.StandardOpenOption;
              import java.util.Collection;
              import java.util.Collections;
              import java.util.List;

              class Foo {
                  void bar(File fileA, File fileB, URL url, Charset cs, FileFilter filter, CharSequence charSeq) throws Exception {
                      long l = 10L;
                      String s = "hello world";
                      String[] stringArray = new String[4];
                      Collection<String> collection = Collections.EMPTY_LIST;
                      byte[] bytes = new byte[0];
                      String str;
                      boolean bool;
                      List<String> strList;
                      List<File> fileList;
                      File f;

                      FileUtils.write(fileA, s, cs);
                      f = new File(s);
                      f = FileUtils.getFile(s, s);
                      f = FileUtils.toFile(url);

                      str = FileUtils.byteCountToDisplaySize(l);
                      FileUtils.cleanDirectory(fileA);
                      bool = FileUtils.contentEquals(fileA, fileB);
                      bool = FileUtils.contentEqualsIgnoreEOL(fileA, fileB, s);
                      FileUtils.copyDirectory(fileA, fileB);
                      FileUtils.copyFileToDirectory(fileA, fileB);
                      FileUtils.copyFile(fileA, fileB);
                      FileUtils.copyURLToFile(url, fileA);
                      f = FileUtils.current();
                      FileUtils.deleteDirectory(fileA);
                      f = FileUtils.delete(fileA);
                      bool = FileUtils.deleteQuietly(fileA);
                      FileUtils.forceDeleteOnExit(fileA);
                      FileUtils.forceDelete(fileA);
                      FileUtils.forceMkdir(fileA);
                      FileUtils.forceMkdirParent(fileA);
                      f = FileUtils.getTempDirectory();
                      str = FileUtils.readFileToString(fileA, cs);
                      str = FileUtils.readFileToString(fileA, s);
                      strList = FileUtils.readLines(fileA, cs);
                      FileUtils.writeByteArrayToFile(fileA, bytes);
                      FileUtils.writeLines(fileA, collection);
                      Files.write(fileA.toPath(), s.getBytes());
                      
                      Files.write(fileA.toPath(), s.getBytes(), bool ? StandardOpenOption.APPEND : null);
                      Files.write(fileA.toPath(), Collections.singletonList(s), cs);
                      Files.write(fileA.toPath(), Collections.singletonList(s), cs, bool ? StandardOpenOption.APPEND : null);
                      Files.write(fileA.toPath(), Collections.singletonList(s), Charset.forName(str));
                      Files.write(fileA.toPath(), Collections.singletonList(s), Charset.forName(str), bool ? StandardOpenOption.APPEND : null);
                  }
              }
              """
          )
        );
    }
}
