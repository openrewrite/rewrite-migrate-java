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
package org.openrewrite.java.migrate.net;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

public class URLConstructorsToURI {
    @RecipeDescriptor(
            name = "Convert `new URL(String)` to `URI.create(String).toURL()`",
            description = "Converts `new URL(String)` constructors to `URI.create(String).toURL()`."
    )
    public static class URLSingleArgumentConstructor {
        @BeforeTemplate
        java.net.URL urlConstructor(String spec) throws Exception {
            return new java.net.URL(spec);
        }

        @AfterTemplate
        java.net.URL uriCreateToURL(String spec) throws Exception {
            return java.net.URI.create(spec).toURL();
        }
    }

    @RecipeDescriptor(
            name = "Convert `new URL(String, String, String)` to `new URI(...).toURL()`",
            description = "Converts `new URL(String, String, String)` constructors to `new URI(...).toURL()`."
    )
    public static class URLThreeArgumentConstructor {
        @BeforeTemplate
        java.net.URL urlConstructor(String protocol, String host, String file) throws Exception {
            return new java.net.URL(protocol, host, file);
        }

        @AfterTemplate
        java.net.URL newUriToUrl(String protocol, String host, String file) throws Exception {
            return new java.net.URI(protocol, null, host, -1, file, null, null).toURL();
        }
    }

    @RecipeDescriptor(
            name = "Convert `new URL(String, String, int, String)` to `new URI(...).toURL()`",
            description = "Converts `new URL(String, String, int, String)` constructors to `new URI(...).toURL()`."
    )
    public static class URLFourArgumentConstructor {
        @BeforeTemplate
        java.net.URL urlConstructor(String protocol, String host, int port, String file) throws Exception {
            return new java.net.URL(protocol, host, port, file);
        }

        @AfterTemplate
        java.net.URL newUriToUrl(String protocol, String host, int port, String file) throws Exception {
            return new java.net.URI(protocol, null, host, port, file, null, null).toURL();
        }
    }
}
