/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class FlagUsageVar extends FlagUsage {

    String key = "lombok.var.flagUsage";

    String displayName = "Flag usage of Lombok's `var`";

    String description = "Assign `lombok.var.flagUsage = error` in every `lombok.config`, so that Lombok fails the " +
            "build on a use of Lombok's `var` rather than compiling it. Java has had `var` since Java 10 and Lombok " +
            "deprecated its own, so a project on Java 10 or later can drop the `lombok.var` import and let the " +
            "compiler do the work. Run this once the uses are gone, to keep them from coming back. A file that " +
            "assigns the key another value is rewritten to `error`; a file that speaks about the key in a way that " +
            "cannot be rewritten, such as `clear lombok.var.flagUsage`, is left as written, as is a project with no " +
            "`lombok.config` at all, as there is then no file to write to.";
}
