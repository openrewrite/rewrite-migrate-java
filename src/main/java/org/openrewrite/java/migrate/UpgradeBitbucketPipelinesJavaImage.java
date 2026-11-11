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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeBitbucketPipelinesJavaImage extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "25")
    Integer version;

    String displayName = "Upgrade Java images in Bitbucket Pipelines";
    String description = "Upgrade the Java version of the build images in `bitbucket-pipelines.yml`, being the default " +
            "`image`, the `image` of a step and the `image` of a service, in either the plain or the `name` form. " +
            "Images are upgraded as by `UpgradeDockerImageVersion`, which also moves deprecated `openjdk` images to " +
            "`eclipse-temurin`. Pipes, images that are not Java images, and images already at or beyond the target " +
            "version are left untouched.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private final JsonPathMatcher defaultImage = new JsonPathMatcher("$.image");
            private final JsonPathMatcher serviceImage = new JsonPathMatcher("$.definitions.services.*.image");

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                if (!"bitbucket-pipelines.yml".equals(documents.getSourcePath().toString())) {
                    return documents;
                }
                return super.visitDocuments(documents, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (!"image".equals(e.getKey().getValue()) ||
                    !(defaultImage.matches(getCursor()) || serviceImage.matches(getCursor()) || isStepImage(getCursor()))) {
                    return e;
                }
                if (e.getValue() instanceof Yaml.Mapping) {
                    Yaml.Mapping image = (Yaml.Mapping) e.getValue();
                    return e.withValue(image.withEntries(ListUtils.map(image.getEntries(), imageEntry ->
                            "name".equals(imageEntry.getKey().getValue()) ? imageEntry.withValue(upgraded(imageEntry.getValue())) : imageEntry)));
                }
                return e.withValue(upgraded(e.getValue()));
            }

            private boolean isStepImage(Cursor cursor) {
                Object owner = cursor.getParentTreeCursor().getParentTreeCursor().getValue();
                if (!(owner instanceof Yaml.Mapping.Entry)) {
                    return false;
                }
                String key = ((Yaml.Mapping.Entry) owner).getKey().getValue();
                return "step".equals(key) || "final".equals(key);
            }

            private Yaml.Block upgraded(Yaml.Block image) {
                if (!(image instanceof Yaml.Scalar)) {
                    return image;
                }
                Yaml.Scalar reference = (Yaml.Scalar) image;
                String upgraded = JavaImageReference.upgrade(reference.getValue(), version);
                return upgraded == null ? image : reference.withValue(upgraded);
            }
        };
    }
}
