package org.openrewrite.java.migrate.apache.commons.lang;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;

public class ApacheCommonsFileUtils {
    private static class Write {
        @BeforeTemplate
        void before(File file, CharSequence data, Charset cs) throws Exception {
            FileUtils.write(file, data, cs);
        }

        @AfterTemplate
        void after(File file, CharSequence data, Charset cs) throws Exception {
            Files.write(file.toPath(), Arrays.asList(data), cs);
        }
    }

    private static class GetFile {
        @BeforeTemplate
        File before(String name) throws Exception {
            return FileUtils.getFile(name);
        }

        @AfterTemplate
        File after(String name) throws Exception {
            return new File(name);
        }
    }

}
