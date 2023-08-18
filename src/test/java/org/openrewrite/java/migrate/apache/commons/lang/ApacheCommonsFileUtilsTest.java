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

public class ApacheCommonsFileUtilsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("commons-lang3"))
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
              import org.apache.commons.io.filefilter.IOFileFilter;
              import java.util.Collections;
              import java.util.File;
              import java.nio.charset.Charset;
              import java.net.URL;
              
              class Foo {
                  void bar(File fileA, File fileB, URL url, Charset cs, IOFileFilter filter, CharSequence charSeq) throws Exception {
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
                      
                      Path p = FileUtils.write(file, s, cs);
                      f = FileUtils.getFile(s);
                  
                      FileUtils.deleteDirectory(fileA);
                      FileUtils.forceDeleteOnExit(fileA);
                      FileUtils.forceDelete(fileA);
                      bool = FileUtils.deleteQuietly(fileA);
                      FileUtils.copyFile(fileA, fileB);
                      str = FileUtils.byteCountToDisplaySize(l);
                      FileUtils.copyURLToFile(URL, fileA);
                      FileUtils.writeStringToFile(fileA, s);
                      strList = FileUtils.readLines(fileA, cs);
                      str = FileUtils.readFileToString(fileA, cs);
                      str = FileUtils.readFileToString(fileA, s);
                      f = FileUtils.getTempDirectory();
                      str = FileUtils.readFileToString(fileA, s);
                      FileUtils.forceDelete(fileA);
                      FileUtils.copyDirectory(fileA, fileB);
                      FileUtils.writeByteArrayToFile(fileA, bytes);
                      FileUtils.cleanDirectory(fileA);
                      f = FileUtils.toFile(url);
                      fileList = FileUtils.listFiles(fileA, filter);
                      FileUtils.forceMkdirParent(fileA);
                      bool = FileUtils.contentEquals(fileA, fileB);
                      fileList = FileUtils.listFiles(fileA, stringArray);
                      str = FileUtils.readFileToString(fileA, s);
                      FileUtils.writeLines(fileA, collection);
                  }
              }
              """,
            """
              import org.apache.commons.io.FileUtils;
              import org.apache.commons.io.filefilter.IOFileFilter;
              import java.util.Collections;
              import java.nil.file.Files;
              import java.util.Arrays;
              import java.io.File;
              import java.nio.charset.Charset;
              import java.net.URL;
              
              class Foo {
                  void bar(File fileA, File fileB, URL url, Charset cs, IOFileFilter filter, CharSequence charSeq) {
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
                      
                      Files.write(fileA.toPath(), Arrays.asList(charSeq), cs);
                      f = new Files(s);
                  
                      FileUtils.deleteDirectory(fileA);
                      FileUtils.forceDeleteOnExit(fileA);
                      FileUtils.forceDelete(fileA);
                      bool = FileUtils.deleteQuietly(fileA);
                      FileUtils.copyFile(fileA, fileB);
                      str = FileUtils.byteCountToDisplaySize(l);
                      FileUtils.copyURLToFile(URL, fileA);
                      FileUtils.writeStringToFile(fileA, s);
                      strList = FileUtils.readLines(fileA, cs);
                      str = FileUtils.readFileToString(fileA, cs);
                      str = FileUtils.readFileToString(fileA, s);
                      f = FileUtils.getTempDirectory();
                      str = FileUtils.readFileToString(fileA, s);
                      FileUtils.forceDelete(fileA);
                      FileUtils.copyDirectory(fileA, fileB);
                      FileUtils.writeByteArrayToFile(fileA, bytes);
                      FileUtils.cleanDirectory(fileA);
                      f = FileUtils.toFile(url);
                      fileList = FileUtils.listFiles(fileA, filter);
                      FileUtils.forceMkdirParent(fileA);
                      bool = FileUtils.contentEquals(fileA, fileB);
                      fileList = FileUtils.listFiles(fileA, stringArray);
                      str = FileUtils.readFileToString(fileA, s);
                      FileUtils.writeLines(fileA, collection);
                  }
              }
              """
          )
        );
    }
}
