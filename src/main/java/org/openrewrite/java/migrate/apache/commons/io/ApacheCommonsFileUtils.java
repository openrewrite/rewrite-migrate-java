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

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;

@SuppressWarnings("deprecation")
public class ApacheCommonsFileUtils {
    private static class GetFile {
        @BeforeTemplate
        File before(String name) {
            return FileUtils.getFile(name);
        }

        @AfterTemplate
        File after(String name) {
            return new File(name);
        }
    }

// NOTE: java: reference to compile is ambiguous; methods P3 & F3 match
//    private static class Write {
//        @BeforeTemplate
//        void before(File file, CharSequence data, Charset cs) throws Exception {
//            FileUtils.write(file, data, cs);
//        }
//
//        @AfterTemplate
//        void after(File file, CharSequence data, Charset cs) throws Exception {
//            Files.write(file.toPath(), Arrays.asList(data), cs);
//        }
//    }

    @SuppressWarnings("deprecation")
    private static class WriteStringToFile {
        @BeforeTemplate
        void before(File a, String s) throws Exception {
            FileUtils.writeStringToFile(a, s);
        }

        @AfterTemplate
        void after(File a, String s) throws Exception {
            Files.write(a.toPath(), s.getBytes());
        }
    }

    // file, string, boolean
    private static class WriteStringToFileOverloadOne {
        @BeforeTemplate
        void before(File a, String data, boolean append) throws Exception {
            FileUtils.writeStringToFile(a, data, append);
        }

        @AfterTemplate
        void after(File a, String data) throws Exception {
            Files.write(a.toPath(), data.getBytes(), StandardOpenOption.APPEND);
        }
    }

    // file, string, charset
    @SuppressWarnings("depcr")
    private static class WriteStringToFileOverloadTwo {
        @BeforeTemplate
        void before(File a, String data, Charset cs) throws Exception {
            FileUtils.writeStringToFile(a, data, cs);
        }

        @AfterTemplate
        void after(File a, String data, Charset cs) throws Exception {
            Files.write(a.toPath(), Collections.singletonList(data), cs);
        }
    }

    // file, string, charset, boolean
    private static class WriteStringToFileOverloadThree {
        @BeforeTemplate
        void before(File a, String data, Charset cs, boolean append) throws Exception {
            FileUtils.writeStringToFile(a, data, cs, append);
        }

        @AfterTemplate
        void after(File a, String data, Charset cs, boolean append) throws Exception {
            Files.write(a.toPath(), Collections.singletonList(data), cs, append ? StandardOpenOption.APPEND : null);
        }
    }

    // file, string, string
    private static class WriteStringToFileOverloadFour {
        @BeforeTemplate
        void before(File a, String data, String charsetName) throws Exception {
            FileUtils.writeStringToFile(a, data, charsetName);
        }

        @AfterTemplate
        void after(File a, String data, String charsetName) throws Exception {
            Files.write(a.toPath(), Collections.singletonList(data), Charset.forName(charsetName));
        }
    }

    // file, string, string, boolean
    private static class WriteStringToFileOverloadFive {
        @BeforeTemplate
        void before(File a, String data, String charsetName, boolean append) throws Exception {
            FileUtils.writeStringToFile(a, data, charsetName, append);
        }

        @AfterTemplate
        void after(File a, String data, String charsetName, boolean append) throws Exception {
            Files.write(a.toPath(), Collections.singletonList(data), Charset.forName(charsetName), append ? StandardOpenOption.APPEND : null);
        }
    }

}
