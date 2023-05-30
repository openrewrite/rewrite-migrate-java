/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.table;

import lombok.Value;
import org.openrewrite.Column;

@Value
public class JavaVersionRow {
    @Column(displayName = "Project name",
            description = "The module name (useful especially for multi-module repositories).")
    String projectName;

    @Column(displayName = "Source set name",
            description = "The source set, e.g. `main` or `test`.")
    String sourceSetName;

    @Column(displayName = "Created by",
            description = "The JDK release that was used to compile the source file.")
    String createdBy;

    @Column(displayName = "VM vendor",
            description = "The vendor of the JVM that was used to compile the source file.")
    String vmVendor;

    @Column(displayName = "Source compatibility",
            description = "The source compatibility of the source file.")
    String sourceCompatibility;

    @Column(displayName = "Major version source compatibility",
            description = "The major version.")
    String majorVersionSourceCompatibility;

    @Column(displayName = "Target compatibility",
            description = "The target compatibility or `--release` version of the source file.")
    String targetCompatibility;
}
