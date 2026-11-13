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
import org.jspecify.annotations.Nullable;
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

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.emptySet;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaContainerImage extends Recipe {

    private static final String ALIASED_IMAGES = "ALIASED_IMAGES";

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
            public Yaml.Document visitDocument(Yaml.Document document, ExecutionContext ctx) {
                Set<String> aliasedImages = new HashSet<>();
                new YamlIsoVisitor<Set<String>>() {
                    @Override
                    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Set<String> anchors) {
                        if (matcher.matches(getCursor())) {
                            addAnchors(entry.getValue(), anchors);
                        }
                        return super.visitMappingEntry(entry, anchors);
                    }

                    @Override
                    public Yaml.Sequence.Entry visitSequenceEntry(Yaml.Sequence.Entry entry, Set<String> anchors) {
                        if (matcher.matches(getCursor())) {
                            addAnchors(entry.getBlock(), anchors);
                        }
                        return super.visitSequenceEntry(entry, anchors);
                    }
                }.visit(document, aliasedImages, getCursor().getParentOrThrow());
                getCursor().putMessage(ALIASED_IMAGES, aliasedImages);
                return super.visitDocument(document, ctx);
            }

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

            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, ExecutionContext ctx) {
                Yaml.Scalar s = super.visitScalar(scalar, ctx);
                return isAliasedImage(s.getAnchor()) ? (Yaml.Scalar) upgraded(s) : s;
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                return isAliasedImage(m.getAnchor()) ? (Yaml.Mapping) upgraded(m) : m;
            }

            private boolean isAliasedImage(Yaml.@Nullable Anchor anchor) {
                return anchor != null &&
                       getCursor().<Set<String>>getNearestMessage(ALIASED_IMAGES, emptySet()).contains(anchor.getKey());
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

    /// An image given as an alias is upgraded where its anchor is defined, so that every alias follows.
    private static void addAnchors(Yaml.Block image, Set<String> anchors) {
        if (image instanceof Yaml.Alias) {
            anchors.add(((Yaml.Alias) image).getAnchor().getKey());
        } else if (image instanceof Yaml.Mapping) {
            for (Yaml.Mapping.Entry entry : ((Yaml.Mapping) image).getEntries()) {
                if ("name".equals(entry.getKey().getValue())) {
                    addAnchors(entry.getValue(), anchors);
                }
            }
        }
    }
}
