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
import org.openrewrite.xml.XPathMatcher;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * @deprecated Use {@link org.openrewrite.maven.UseMavenCompilerPluginReleaseConfiguration} instead.
 */
@Value
@EqualsAndHashCode(callSuper = false)
@Deprecated
public class UseMavenCompilerPluginReleaseConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build//plugins");

    @Option(
            displayName = "Release version",
            description = "The new value for the release configuration. This recipe prefers ${java.version} if defined.",
            example = "11"
    )
    Integer releaseVersion;

    @Override
    public String getDisplayName() {
        return "Use Maven compiler plugin release configuration";
    }

    @Override
    public String getDescription() {
        return "Replaces any explicit `source` or `target` configuration (if present) on the `maven-compiler-plugin` with " +
                "`release`, and updates the `release` value if needed. Will not downgrade the Java version if the current version is higher.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new org.openrewrite.maven.UseMavenCompilerPluginReleaseConfiguration(releaseVersion));
    }
}
