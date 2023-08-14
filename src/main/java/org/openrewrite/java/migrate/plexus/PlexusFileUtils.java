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
package org.openrewrite.java.migrate.plexus;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import java.io.File;
import java.io.IOException;

class PlexusFileUtils {

    static class DeleteDirectoryString {
        @BeforeTemplate
        void before(String dir) throws IOException {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(dir);
        }

        @AfterTemplate
        void after(String dir) throws IOException {
            org.apache.commons.io.FileUtils.deleteDirectory(new File(dir));
        }
    }

    static class DeleteDirectoryFile {
        @BeforeTemplate
        void before(File dir) throws IOException {
            org.codehaus.plexus.util.FileUtils.deleteDirectory(dir);
        }

        @AfterTemplate
        void after(File dir) throws IOException {
            org.apache.commons.io.FileUtils.deleteDirectory(dir);
        }
    }

    static class FileExistsString {
        @BeforeTemplate
        boolean before(String fileName) throws IOException {
            return org.codehaus.plexus.util.FileUtils.fileExists(fileName);
        }

        @AfterTemplate
        boolean after(String fileName) throws IOException {
            return new File(fileName).exists();
        }
    }

}
