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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;

import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

/// @deprecated Use [UpgradeJavaContainerImages], which covers these and other CI and Kubernetes files.
@Deprecated
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaCiContainerImage extends Recipe {
    @Option(displayName = "Java version", description = "The minimum Java version for CI job containers.", example = "25")
    Integer version;

    String displayName = "Upgrade CircleCI OpenJDK job images";
    String description = "Deprecated in favor of `UpgradeJavaContainerImages`, which this delegates to.";

    Set<String> tags = singleton("deprecated");

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new UpgradeJavaContainerImages(version));
    }
}
