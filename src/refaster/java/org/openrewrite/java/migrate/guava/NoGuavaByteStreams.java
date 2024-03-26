/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import com.google.common.io.ByteStreams;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RecipeDescriptor(name = "No Guava ByteStreams",
        description = "Replaces Guava ByteStreams with Java 9+ alternatives.",
        tags = "guava")
public class NoGuavaByteStreams {
    private NoGuavaByteStreams() {
    }

    @RecipeDescriptor(
            name = "ByteStreams#copy",
            description = "Replaces Guava `ByteStreams.copy` with `InputStream.transferTo`.",
            tags = "guava")
    public static final class InputStreamTransferTo {
        @BeforeTemplate
        long before(InputStream in, OutputStream out) throws IOException {
            return ByteStreams.copy(in, out);
        }

        @AfterTemplate
        long after(InputStream in, OutputStream out) throws IOException {
            return in.transferTo(out);
        }
    }

    @RecipeDescriptor(
            name = "ByteStreams#toByteArray",
            description = "Replaces Guava `ByteStreams.toByteArray` with `InputStream.readAllBytes`.",
            tags = "guava")
    public static final class InputStreamReadAllBytes {
        @BeforeTemplate
        byte[] before(InputStream in) throws IOException {
            return ByteStreams.toByteArray(in);
        }

        @AfterTemplate
        byte[] after(InputStream in) throws IOException {
            return in.readAllBytes();
        }
    }
}
