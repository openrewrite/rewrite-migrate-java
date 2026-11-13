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
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaContainerImage extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "25")
    Integer version;

    @Option(displayName = "File pattern",
            description = "A glob expression matching the YAML files holding the image references. " +
                    "Multiple patterns may be separated by a semicolon `;`.",
            example = ".github/workflows/*.yml;.github/workflows/*.yaml")
    String filePattern;

    @Option(displayName = "JsonPath",
            description = "A JsonPath expression matching the image references, either as a plain reference " +
                    "or as a mapping holding the reference under `name`.",
            example = "$.jobs.*.container.image")
    String jsonPath;

    String displayName = "Upgrade Java container images in YAML";
    String description = "Upgrade the Java version of the container image references found at a JsonPath in YAML " +
            "files, as by `UpgradeDockerImageVersion`. Deprecated `openjdk` images move to `eclipse-temurin`, " +
            "while images that are not Java images, and images already at or beyond the target version, " +
            "are left untouched.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(jsonPath);
        return Preconditions.check(new FindSourceFiles(filePattern), new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                return matcher.matches(getCursor()) ? e.withValue(upgraded(e.getValue())) : e;
            }

            @Override
            public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, ExecutionContext ctx) {
                Yaml.Sequence.Entry e = super.visitSequenceEntry(entry, ctx);
                return matcher.matches(getCursor()) ? e.withBlock(upgraded(e.getBlock())) : e;
            }

            private Yaml.Block upgraded(Yaml.Block image) {
                if (image instanceof Yaml.Mapping) {
                    Yaml.Mapping mapping = (Yaml.Mapping) image;
                    return mapping.withEntries(ListUtils.map(mapping.getEntries(), entry ->
                            "name".equals(entry.getKey().getValue()) ? entry.withValue(upgraded(entry.getValue())) : entry));
                }
                if (!(image instanceof Yaml.Scalar)) {
                    return image;
                }
                Yaml.Scalar reference = (Yaml.Scalar) image;
                String upgraded = JavaImageReference.upgrade(reference.getValue(), version);
                return upgraded == null ? image : reference.withValue(upgraded);
            }
        });
    }
}
