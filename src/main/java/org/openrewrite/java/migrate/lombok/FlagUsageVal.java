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
public class FlagUsageVal extends FlagUsage {

    String key = "lombok.val.flagUsage";

    String displayName = "Flag usage of Lombok's `val`";

    String description = "Assign `lombok.val.flagUsage = error` in every `lombok.config`, so that Lombok fails the " +
            "build on a use of `val` rather than compiling it. Java has had `var` since Java 10, which the compiler " +
            "understands where Lombok's `val` is an annotation processor trick that only Lombok understands, so a " +
            "project on Java 10 or later has no need of it. Run this once the uses are gone, as " +
            "`LombokValToFinalVar` leaves them, to keep them from coming back. A file that assigns the key " +
            "another value is rewritten to `error`; a file that speaks about the key in a way that cannot be " +
            "rewritten, such as `clear lombok.val.flagUsage`, is left as written, as is a project with no " +
            "`lombok.config` at all, as there is then no file to write to.";
}
