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
import org.openrewrite.java.template.RecipeDescriptor;

import java.io.File;
import java.nio.file.Files;

public class ApacheCommonsFileUtils {
    @RecipeDescriptor(
            name = "Replace `FileUtils.getFile(String...)` with JDK internals",
            description = "Replace Apache Commons `FileUtils.getFile(String... name)` with JDK internals.")
    public static class GetFile {
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

    @RecipeDescriptor(
            name = "Replace `FileUtils.writeStringToFile(File, String)` with JDK internals",
            description = "Replace Apache Commons `FileUtils.writeStringToFile(File file, String data)` with JDK internals.")
    @SuppressWarnings("deprecation")
    public static class WriteStringToFile {
        @BeforeTemplate
        void before(File a, String s) throws Exception {
            FileUtils.writeStringToFile(a, s);
        }

        @AfterTemplate
        void after(File a, String s) throws Exception {
            Files.write(a.toPath(), s.getBytes());
        }
    }
}
