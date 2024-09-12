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
package org.openrewrite.java.migrate.table;

import lombok.Builder;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class JavaVersionMigrationPlan extends DataTable<JavaVersionMigrationPlan.Row> {

    public JavaVersionMigrationPlan(Recipe recipe) {
        super(
                recipe,
                "Java version migration plan",
                "A per-repository view of the current state of Java versions and associated build tools"
        );
    }

    @Builder
    @Value
    public static class Row {
        public static class Builder {
        }

        @Column(displayName = "Has Java",
                description = "Whether this is a Java repository at all.")
        boolean hasJava;

        @Column(displayName = "Source compatibility",
                description = "The source compatibility of the source file.")
        String sourceCompatibility;

        @Column(displayName = "Major version source compatibility",
                description = "The major version.")
        Integer majorVersionSourceCompatibility;

        @Column(displayName = "Target compatibility",
                description = "The target compatibility or `--release` version of the source file.")
        String targetCompatibility;

        @Column(displayName = "Gradle version",
                description = "The version of Gradle in use, if any.")
        String gradleVersion;

        @Column(displayName = "Has Gradle build",
                description = "Whether a build.gradle file exists in the repository.")
        Boolean hasGradleBuild;

        @Column(displayName = "Maven version",
                description = "The version of Maven in use, if any.")
        String mavenVersion;

        @Column(displayName = "Has Maven pom",
                description = "Whether a pom.xml file exists in the repository.")
        Boolean hasMavenPom;
    }
}
