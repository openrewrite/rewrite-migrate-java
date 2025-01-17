/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @deprecated in favor of {@link org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion}
 */
@Value
@EqualsAndHashCode(callSuper = false)
@Deprecated
public class UpdateMavenProjectPropertyJavaVersion extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Override
    public String getDisplayName() {
        return "Update Maven Java project properties";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "The Java version is determined by several project properties, including:\n\n" +
                " * `java.version`\n" +
                " * `jdk.version`\n" +
                " * `javaVersion`\n" +
                " * `jdkVersion`\n" +
                " * `maven.compiler.source`\n" +
                " * `maven.compiler.target`\n" +
                " * `maven.compiler.release`\n" +
                " * `release.version`\n\n" +
                "If none of these properties are in use and the maven compiler plugin is not otherwise configured, adds the `maven.compiler.release` property.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion(version));
    }
}
